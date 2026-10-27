package com.vaultstadio.api.routes.version

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionRetentionHttpTest {
    private val f = VersionIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @BeforeEach fun reset() = f.reset()

    @Test
    fun `retention preserves restored history and canonical current content`() = testApplication {
        application { f.install(this) }
        val restore = client.post("/api/v1/versions/item/${f.item.id}/restore") {
            bearerAuth("owner")
            contentType(ContentType.Application.Json)
            setBody("""{"versionNumber":1}""")
        }
        assertEquals(HttpStatusCode.OK, restore.status, restore.bodyAsText())
        val response = client.post("/api/v1/versions/item/${f.item.id}/cleanup") {
            bearerAuth("owner")
            contentType(ContentType.Application.Json)
            setBody("""{"maxVersions":1,"minVersionsToKeep":1}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val versions = f.repository.listVersions(f.item.id, 100, 0).fold({ throw it }, { it })
        assertEquals(listOf(3), versions.map { it.versionNumber })
        for ((key, content) in listOf(f.old.storageKey to "old\n", f.latest.storageKey to "new\n")) {
            val stream = f.storage.backend.retrieve(key).fold({ throw it }, { it })
            assertEquals(content, stream.bufferedReader().use { it.readText() })
        }
    }
}
