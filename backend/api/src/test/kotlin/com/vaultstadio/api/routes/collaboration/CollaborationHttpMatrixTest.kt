package com.vaultstadio.api.routes.collaboration

import arrow.core.Either
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.domain.storage.model.Visibility
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationHttpMatrixTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "C89-HTTP {0} {1} {2}")
    @MethodSource("cases")
    fun `production HTTP boundary rejects nonowners without any state or byte change`(
        op: String,
        visibility: Visibility,
        actor: String,
    ) {
        f.reset(visibility)
        testApplication {
            application { f.install(this) }
            val joined = client.request("$ROOT/sessions/join") {
                method = HttpMethod.Post
                bearerAuth("owner")
                contentType(ContentType.Application.Json)
                setBody("""{"itemId":"${f.item.id}"}""")
            }
            assertEquals(HttpStatusCode.Created, joined.status)
            val data = Json.parseToJsonElement(joined.bodyAsText()).jsonObject
            val session = data.getValue("id").jsonPrimitive.content
            val participant = data.getValue("participantId").jsonPrimitive.content
            val comment = f.seedComment()
            val before = f.snapshot()
            val response = invoke(client, op, actor, CollaborationHttpContext(session, participant, comment))
            if (actor == "owner") {
                assertTrue(response.status.value in 200..299, response.bodyAsText())
                verifyOwnerOutcome(op, comment)
            } else {
                assertEquals(
                    if (actor ==
                        "anonymous"
                    ) {
                        HttpStatusCode.Unauthorized
                    } else {
                        HttpStatusCode.Forbidden
                    },
                    response.status,
                )
                assertEquals(before, f.snapshot())
            }
        }
    }

    private suspend fun invoke(
        client: HttpClient,
        op: String,
        actor: String,
        context: CollaborationHttpContext,
    ): HttpResponse {
        val target = target(op, context.session, context.comment.id)
        return client.request(ROOT + target.path) {
            method = target.method
            if (actor != "anonymous") bearerAuth(actor)
            headers.append("X-Participant-ID", context.participant)
            target.body?.let {
                contentType(ContentType.Application.Json)
                setBody(it)
            }
        }
    }

    private fun target(op: String, session: String, comment: String): CollaborationHttpTarget {
        val item = f.item.id
        val document = "/documents/$item"
        val comments = "$document/comments"
        return when (op) {
            "join" -> CollaborationHttpTarget(HttpMethod.Post, "/sessions/join", """{"itemId":"$item"}""")
            "leave" -> CollaborationHttpTarget(HttpMethod.Post, "/sessions/$session/leave")
            "session" -> CollaborationHttpTarget(HttpMethod.Get, "/sessions/$session")
            "participants" -> CollaborationHttpTarget(HttpMethod.Get, "/sessions/$session/participants")
            "cursor" -> CollaborationHttpTarget(
                HttpMethod.Post,
                "/sessions/$session/cursor",
                """{"line":2,"column":3,"offset":4}""",
            )
            "selection" -> CollaborationHttpTarget(
                HttpMethod.Post,
                "/sessions/$session/selection",
                """{"start":{"line":0,"column":0,"offset":0},"end":{"line":1,"column":1,"offset":2}}""",
            )
            "document" -> CollaborationHttpTarget(HttpMethod.Get, document)
            "apply" -> CollaborationHttpTarget(
                HttpMethod.Post,
                "$document/operations",
                """{"type":"INSERT","position":0,"text":"applied","baseVersion":1}""",
            )
            "operations" -> CollaborationHttpTarget(HttpMethod.Get, "$document/operations")
            "save" -> CollaborationHttpTarget(HttpMethod.Post, "$document/save")
            "comments" -> CollaborationHttpTarget(HttpMethod.Get, comments)
            "createComment" -> CollaborationHttpTarget(
                HttpMethod.Post,
                comments,
                """{"content":"new","startLine":0,"startColumn":0,"endLine":1,"endColumn":1}""",
            )
            "resolve" -> CollaborationHttpTarget(HttpMethod.Post, "$comments/$comment/resolve")
            "reply" -> CollaborationHttpTarget(
                HttpMethod.Post,
                "$comments/$comment/replies",
                """{"content":"reply with correct identity"}""",
            )
            "delete" -> CollaborationHttpTarget(HttpMethod.Delete, "$comments/$comment")
            else -> error("Unenumerated HTTP surface")
        }
    }

    private suspend fun verifyOwnerOutcome(op: String, comment: DocumentComment) {
        when (op) {
            "reply" -> {
                val updated = f.repository.findComment(comment.id).required().let { checkNotNull(it) }
                assertEquals("reply with correct identity", updated.replies.single().content)
                assertEquals(f.storage.alice.id, updated.replies.single().userId)
            }
            "apply" -> {
                val state = f.repository.getDocumentState(f.item.id).required().let { checkNotNull(it) }
                assertEquals(2, state.version)
                assertEquals("appliedowned text\n", state.content)
            }
            "delete" -> {
                assertEquals(null, f.repository.findComment(comment.id).required())
            }
            "resolve" -> {
                assertTrue(checkNotNull(f.repository.findComment(comment.id).required()).isResolved)
            }
            else -> {
                Unit
            }
        }
    }

    companion object {
        private const val ROOT = "/api/v1/collaboration"

        @JvmStatic
        fun cases(): List<Arguments> {
            val operations =
                listOf(
                    "join",
                    "leave",
                    "session",
                    "participants",
                    "cursor",
                    "selection",
                    "document",
                    "apply",
                    "operations",
                    "save",
                    "comments",
                    "createComment",
                    "resolve",
                    "reply",
                    "delete",
                )
            val authenticated = operations.flatMap { op ->
                Visibility.entries.flatMap { visibility ->
                    listOf("owner", "unrelated", "shared", "admin").map { Arguments.of(op, visibility, it) }
                }
            }
            return authenticated + operations.map { Arguments.of(it, Visibility.PRIVATE, "anonymous") }
        }
    }
}

private data class CollaborationHttpTarget(val method: HttpMethod, val path: String, val body: String? = null)

private data class CollaborationHttpContext(val session: String, val participant: String, val comment: DocumentComment)

private fun <A : Throwable, B> Either<A, B>.required(): B = fold({ throw it }, { it })
