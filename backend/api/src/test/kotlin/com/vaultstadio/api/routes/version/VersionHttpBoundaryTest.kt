package com.vaultstadio.api.routes.version

import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.domain.storage.model.ItemType
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.koin.ktor.ext.get as koinGet

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionHttpBoundaryTest {
    private val f = VersionIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @BeforeEach fun reset() = f.reset()

    @Test
    fun `owner missing version is 404 while a previous creator remains forbidden`() = testApplication {
        application { f.install(this) }
        val before = f.snapshot()
        val path = "/api/v1/versions/item/${f.item.id}/version/99"
        assertEquals(HttpStatusCode.NotFound, client.get(path) { bearerAuth("owner") }.status)
        assertEquals(HttpStatusCode.Forbidden, client.get(path) { bearerAuth("unrelated") }.status)
        assertEquals(before, f.snapshot())
    }

    @Test
    fun `real SQL failure remains 500 and cannot override an earlier denial`() = testApplication {
        application { f.install(this) }
        val before = f.snapshot()
        transaction { exec("ALTER TABLE file_versions RENAME TO private_provider_failure") }
        try {
            val path = "/api/v1/versions/item/${f.item.id}"
            val response = client.get(path) { bearerAuth("owner") }
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val text = response.bodyAsText()
            assertTrue(text.contains("DATABASE_ERROR"))
            assertFalse(text.contains("private_provider_failure"))
            assertFalse(text.contains(f.old.storageKey))
            assertEquals(HttpStatusCode.Forbidden, client.get(path) { bearerAuth("unrelated") }.status)
            assertEquals(
                HttpStatusCode.InternalServerError,
                client.delete("/api/v1/versions/${f.old.id}") {
                    bearerAuth("owner")
                }.status,
            )
        } finally {
            transaction { exec("ALTER TABLE private_provider_failure RENAME TO file_versions") }
        }
        assertEquals(before, f.snapshot())
    }

    @Test
    fun `actual unreadable historical bytes preserve state and produce a safe provider failure`() = testApplication {
        application { f.install(this) }
        val before = f.snapshot()
        val file = Files.walk(f.storage.contentPath).use { paths ->
            paths.filter {
                Files.isRegularFile(it) && Files.readAllBytes(it).contentEquals("old\n".toByteArray())
            }.findFirst().orElseThrow()
        }
        val rollback = file.resolveSibling(file.fileName.toString() + ".owned-rollback")
        Files.move(file, rollback)
        try {
            Files.createDirectory(file)
            val path = "/api/v1/versions/item/${f.item.id}/diff?from=1&to=2"
            val response = client.get(path) { bearerAuth("owner") }
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val text = response.bodyAsText()
            assertTrue(text.contains("STORAGE_BACKEND_ERROR"))
            assertFalse(text.contains(file.toString()))
            assertFalse(text.contains(f.old.storageKey))
            assertEquals(HttpStatusCode.Forbidden, client.get(path) { bearerAuth("shared") }.status)
        } finally {
            if (Files.isDirectory(file)) Files.delete(file)
            Files.move(rollback, file)
        }
        assertEquals(before, f.snapshot())
    }

    @Test
    fun `missing canonical item never authorizes retained history or version deletion`() = testApplication {
        application { f.install(this) }
        f.storage.repository.delete(f.item.id).fold({ throw it }, {})
        val before = f.snapshot()
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/v1/versions/item/${f.item.id}") {
                bearerAuth("owner")
            }.status,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.delete("/api/v1/versions/${f.old.id}") {
                bearerAuth("owner")
            }.status,
        )
        assertEquals(before, f.snapshot())
    }

    @Test
    fun `restored history retains its bytes after the source version is deleted`() = testApplication {
        application { f.install(this) }
        val restore = client.post("/api/v1/versions/item/${f.item.id}/restore") {
            bearerAuth("owner")
            contentType(ContentType.Application.Json)
            setBody("""{"versionNumber":1}""")
        }
        assertEquals(HttpStatusCode.OK, restore.status, restore.bodyAsText())
        val restored = f.repository.findByItemAndVersion(f.item.id, 3).fold({ throw it }, { checkNotNull(it) })
        assertEquals(
            HttpStatusCode.NoContent,
            client.delete("/api/v1/versions/${f.old.id}") {
                bearerAuth("owner")
            }.status,
        )
        val stream = f.storage.backend.retrieve(restored.storageKey).fold({ throw it }, { it })
        assertEquals("old\n", stream.bufferedReader().use { it.readText() })
        assertEquals(restored, f.repository.findById(restored.id).fold({ throw it }, { it }))
    }

    @Test
    fun `another owners current item protects a shared blob without exposing that item`() = testApplication {
        val foreign = f.storage.seed(owner = f.storage.bob, type = ItemType.FILE)
        f.storage.repository.update(foreign.copy(storageKey = f.old.storageKey, size = 4)).fold({ throw it }, {})
        val before = f.storage.repository.findById(foreign.id).fold({ throw it }, { it })
        application { f.install(this) }
        val response = client.delete("/api/v1/versions/${f.old.id}") { bearerAuth("owner") }
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals("", response.bodyAsText())
        assertEquals(before, f.storage.repository.findById(foreign.id).fold({ throw it }, { it }))
        val stream = f.storage.backend.retrieve(f.old.storageKey).fold({ throw it }, { it })
        assertEquals("old\n", stream.bufferedReader().use { it.readText() })
    }

    @Test
    fun `production service purges history without deleting the active file`() = testApplication {
        lateinit var service: FileVersionService
        application {
            f.install(this)
            service = koinGet()
        }
        startApplication()
        assertEquals(2, service.cleanupVersionsForItem(f.item.id, f.storage.alice.id).fold({ throw it }, { it }))
        assertEquals(emptyList(), f.repository.listVersions(f.item.id, 100, 0).fold({ throw it }, { it }))
        val stream = f.storage.backend.retrieve(checkNotNull(f.item.storageKey)).fold({ throw it }, { it })
        assertEquals("new\n", stream.bufferedReader().use { it.readText() })
    }
}
