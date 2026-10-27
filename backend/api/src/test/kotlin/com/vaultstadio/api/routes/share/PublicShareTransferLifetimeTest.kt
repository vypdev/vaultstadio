package com.vaultstadio.api.routes.share

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareTransferLifetimeTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "S90-transfer-resource-lifetime {0}")
    @ValueSource(
        strings = [
            "empty",
            "binary",
            "utf8",
            "large",
            "prepare-failure",
            "late-failure",
            "revoke",
            "owner",
            "password",
            "content",
            "filename-unicode",
            "filename-control",
            "filename-quote",
            "filename-html",
        ],
    )
    fun streamsAreOwnedUntilTransferOrDenial(case: String) = testApplication {
        val closed = AtomicBoolean()
        val bytes = when (case) {
            "empty" -> byteArrayOf()
            "binary" -> ByteArray(256) { it.toByte() }
            "large", "late-failure" -> ByteArray(20000) { (it % 256).toByte() }
            else -> "shared bytes ☃\n".toByteArray()
        }
        val backend = observedBackend(case, closed)
        application { f.install(this, backendOverride = backend) }
        startApplication()
        val key = f.storage.backend.store(bytes.inputStream(), bytes.size.toLong(), null).getOrNull()!!
        val name = when (case) {
            "filename-unicode" -> "shared ☃.txt"
            "filename-control" -> "../unsafe\r\nheader.bin"
            "filename-quote" -> "quoted\"name.txt"
            "filename-html" -> "<img src=x onerror=alert(1)>.txt"
            else -> f.item.name
        }
        f.item = f.item.copy(size = bytes.size.toLong(), storageKey = key, name = name)
        f.storage.repository.update(f.item).fold({ throw it }, {})
        assertTransfer(client, case, bytes)
        withTimeout(5000) { while (!closed.get()) delay(10) }
        assertTrue(closed.get(), "Ktor must close the provider on success, denial or failure")
    }
    private suspend fun assertTransfer(client: HttpClient, case: String, bytes: ByteArray) {
        if (case == "late-failure") {
            var failed = false
            try {
                val response = client.get("/api/v1/share/${f.share.token}/download")
                val body = response.bodyAsBytes()
                failed = response.status != HttpStatusCode.OK || body.size != bytes.size
            } catch (_: Exception) {
                failed = true
            }
            assertTrue(failed, "A partial transfer must never be accepted as complete")
            assertEquals(1, f.count())
        } else {
            val response = client.get("/api/v1/share/${f.share.token}/download")
            val expected = when (case) {
                "prepare-failure" -> HttpStatusCode.InternalServerError
                "revoke" -> HttpStatusCode.Gone
                "owner" -> HttpStatusCode.NotFound
                "password", "content" -> HttpStatusCode.Conflict
                else -> HttpStatusCode.OK
            }
            assertEquals(expected, response.status)
            assertEquals(if (expected == HttpStatusCode.OK) 1 else 0, f.count())
            if (expected == HttpStatusCode.OK) {
                assertContentEquals(bytes, response.bodyAsBytes())
                assertAttachment(case, response.headers[HttpHeaders.ContentDisposition]!!)
            }
        }
    }

    private fun assertAttachment(case: String, disposition: String) {
        assertTrue(disposition.all { it.code in 32..126 }, "Attachment headers must be ASCII")
        if (case == "filename-unicode") assertTrue(disposition.contains("filename*="))
    }

    private fun observedBackend(case: String, closed: AtomicBoolean): StorageBackend {
        return object : StorageBackend by f.storage.backend {
            override suspend fun retrieve(storageKey: String): Either<StorageException, InputStream> {
                val result = f.storage.backend.retrieve(storageKey)
                mutateDuringOpen(case)
                return result.map { ObservedShareStream(it, case, closed) }
            }
        }
    }

    private fun mutateDuringOpen(case: String) {
        transaction {
            when (case) {
                "revoke" -> ShareLinksTable.update({
                    ShareLinksTable.id eq f.share.id
                }) { it[isActive] = false }
                "password" -> ShareLinksTable.update({ ShareLinksTable.id eq f.share.id }) {
                    it[password] =
                        "changed"
                }
                "owner" -> StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) {
                    it[ownerId] =
                        f.storage.bob.id
                }
                "content" -> StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) {
                    it[version] =
                        f.item.version + 1
                }
            }
        }
    }
    private class ObservedShareStream(
        source: InputStream,
        private val case: String,
        private val closed: AtomicBoolean,
    ) : FilterInputStream(source) {
        private var consumed = 0
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (case == "prepare-failure" || case == "late-failure" && consumed >= 8192) {
                throw IOException("private provider diagnostic")
            }
            return super.read(buffer, offset, length).also { if (it > 0) consumed += it }
        }
        override fun close() {
            closed.set(true)
            super.close()
        }
    }
}
