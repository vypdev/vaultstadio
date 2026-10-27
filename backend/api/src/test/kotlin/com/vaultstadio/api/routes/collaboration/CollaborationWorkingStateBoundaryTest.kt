package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.core.domain.service.JoinSessionInput
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.Visibility
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationWorkingStateBoundaryTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @Test
    fun `cold file has no fake collaboration snapshot or saved acknowledgement`() {
        f.reset()
        testApplication {
            application { f.install(this) }
            val cold = f.storage.seedStoredFile("cold bytes that must survive")
            val before = f.snapshot()
            val joined = client.post("$ROOT/sessions/join") {
                bearerAuth("owner")
                contentType(ContentType.Application.Json)
                setBody("""{"itemId":"${cold.id}"}""")
            }
            assertEquals(HttpStatusCode.Created, joined.status)
            assertEquals(
                "0",
                Json.parseToJsonElement(
                    joined.bodyAsText(),
                ).jsonObject.getValue("documentVersion").jsonPrimitive.content,
            )
            val afterJoin = f.snapshot()
            assertEquals(
                HttpStatusCode.NotFound,
                client.get("$ROOT/documents/${cold.id}") {
                    bearerAuth("owner")
                }.status,
            )
            assertEquals(
                HttpStatusCode.NotFound,
                client.post("$ROOT/documents/${cold.id}/save") {
                    bearerAuth("owner")
                }.status,
            )
            assertEquals(afterJoin, f.snapshot())
            assertEquals(before.first(), afterJoin.first())
            assertEquals(before.last(), afterJoin.last())
        }
    }

    @ParameterizedTest(name = "C89-BIND comment parent {0}")
    @ValueSource(strings = ["resolve", "replies", "delete"])
    fun `comment operations reject another owned parent without reading or changing the comment`(operation: String) {
        f.reset()
        testApplication {
            application { f.install(this) }
            val other = f.storage.seed(type = ItemType.FILE)
            val comment = f.seedComment()
            val before = f.snapshot()
            val suffix = if (operation == "delete") "" else "/$operation"
            val response = client.request("$ROOT/documents/${other.id}/comments/${comment.id}$suffix") {
                bearerAuth("owner")
                method = if (operation == "delete") HttpMethod.Delete else HttpMethod.Post
                if (operation == "replies") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"content":"must not apply"}""")
                }
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(before, f.snapshot())
        }
    }

    @ParameterizedTest(name = "C89-BIND legacy membership {0}")
    @EnumSource(Visibility::class)
    fun `previously unguarded membership cannot become an access grant`(visibility: Visibility) {
        f.reset(visibility)
        testApplication {
            application { f.install(this) }
            startApplication()
            val (session, participant) = f.service.joinSession(
                JoinSessionInput(f.item.id),
                f.storage.alice.id,
                "Owner",
            ).fold({
                throw it
            }, { it })
            f.repository.addParticipant(
                session.id,
                participant.copy(id = UUID.randomUUID().toString(), userId = f.storage.bob.id),
            ).fold({
                throw it
            }, {})
            val before = f.snapshot()
            assertEquals(
                HttpStatusCode.Forbidden,
                client.get("$ROOT/sessions/${session.id}") {
                    bearerAuth("unrelated")
                }.status,
            )
            val denied = client.post("$ROOT/sessions/join") {
                bearerAuth("unrelated")
                contentType(ContentType.Application.Json)
                setBody("""{"itemId":"${f.item.id}"}""")
            }
            assertEquals(HttpStatusCode.Forbidden, denied.status)
            assertEquals(before, f.snapshot())
        }
    }

    companion object {
        private const val ROOT = "/api/v1/collaboration"
    }
}
