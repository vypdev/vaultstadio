package com.vaultstadio.api.routes.share

import com.vaultstadio.api.dto.ApiResponse
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareGrantBoundaryTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "S90-no-expanded-grant {0}")
    @ValueSource(
        strings = [
            "storage-token", "history-token", "collaboration-token",
            "storage-recipient", "history-recipient", "collaboration-recipient",
        ],
    )
    fun validLinkDoesNotGrantProtectedOperations(case: String) = testApplication {
        application { f.install(this) }
        startApplication()
        f.update(f.share.copy(sharedWithUsers = listOf(f.storage.bob.id)))
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/share/${f.share.token}").status)
        val path = when (case.substringBefore('-')) {
            "storage" -> "/api/v1/storage/item/${f.item.id}"
            "history" -> "/api/v1/versions/item/${f.item.id}"
            else -> "/api/v1/collaboration/documents/${f.item.id}"
        }
        val usingToken = case.endsWith("token")
        val response = client.get(path) { bearerAuth(if (usingToken) f.share.token else "unrelated") }
        assertEquals(if (usingToken) HttpStatusCode.Unauthorized else HttpStatusCode.Forbidden, response.status)
        assertEquals(0, f.count())
        val bytes = f.storage.backend.retrieve(f.item.storageKey!!).getOrNull()!!.use { it.readBytes() }
        assertEquals("shared bytes ☃\n", bytes.decodeToString())
    }

    @ParameterizedTest(name = "S90-protected-owner-positive {0}")
    @ValueSource(strings = ["storage", "history"])
    fun validOwnerStillHasItsOriginalProtectedRead(case: String) = testApplication {
        application { f.install(this) }
        val path = if (case == "storage") "/api/v1/storage/item/${f.item.id}" else "/api/v1/versions/item/${f.item.id}"
        assertEquals(HttpStatusCode.OK, client.get(path) { bearerAuth("owner") }.status)
        assertEquals(0, f.count())
    }

    @Test
    fun `S90 injected response failure cannot consume a metadata allowance`() = testApplication {
        application {
            f.install(this)
            sendPipeline.intercept(ApplicationSendPipeline.Before) {
                val body = subject
                if (body is ApiResponse<*> && body.data is PublicShareResponse) {
                    throw IOException("Deliberate response boundary failure")
                }
            }
        }
        val response = client.get("/api/v1/share/${f.share.token}")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        response.bodyAsText()
        assertEquals(0, f.count())
    }
}
