/**
 * VaultStadio Protected Routes Auth Tests
 *
 * Integration tests that verify protected endpoints use UserPrincipal (call.user)
 * and return 401 when not authenticated. Covers the auth contract after migrating
 * from JWTPrincipal to User/session-based auth.
 */

package com.vaultstadio.api.routes

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.api.config.configureSecurity
import com.vaultstadio.api.config.user
import com.vaultstadio.api.routes.collaboration.CollaborationSessionResponse
import com.vaultstadio.api.routes.collaboration.JoinSessionRequest
import com.vaultstadio.api.routes.collaboration.ParticipantResponse
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.common.exception.AuthenticationException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

private const val VALID_TOKEN = "test-session-token"

class ProtectedRoutesAuthTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST collaboration sessions join without Authorization returns 401`() = testApplication {
        val userService = mockk<UserService>()
        val collaborationService = mockk<CollaborationService>()
        coEvery { userService.validateSession(any()) } returns Either.Left(AuthenticationException("invalid"))

        application {
            (this as Application).install(Koin) {
                modules(
                    module {
                        single<UserService> { userService }
                        single<CollaborationService> { collaborationService }
                    },
                )
            }
            (this as Application).install(ServerContentNegotiation) {
                json(json)
            }
            (this as Application).configureSecurity()
            (this as Application).routing {
                route("/api/v1/collaboration") {
                    authenticate("auth-bearer") {
                        route("/sessions") {
                            post("/join") {
                                val request = call.receive<JoinSessionRequest>()
                                val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                                val userId = user.id
                                val userName = user.username
                                collaborationService.joinSession(
                                    com.vaultstadio.core.domain.service.JoinSessionInput(request.itemId),
                                    userId,
                                    userName,
                                ).fold(
                                    { call.respond(HttpStatusCode.InternalServerError, it.message ?: "Error") },
                                    { (session, participant) ->
                                        call.respond(
                                            HttpStatusCode.Created,
                                            CollaborationSessionResponse(
                                                id = session.id,
                                                itemId = session.itemId,
                                                participantId = participant.id,
                                                participants = session.participants.map { p ->
                                                    ParticipantResponse(
                                                        id = p.id,
                                                        userId = p.userId,
                                                        userName = p.userName,
                                                        color = p.color,
                                                        cursor = null,
                                                        selection = null,
                                                        isEditing = p.isEditing,
                                                    )
                                                },
                                                documentVersion = 0,
                                                createdAt = session.createdAt.toString(),
                                                expiresAt = session.expiresAt.toString(),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        val response = client.post("/api/v1/collaboration/sessions/join") {
            contentType(ContentType.Application.Json)
            setBody(JoinSessionRequest(itemId = "item-1"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST collaboration sessions join with valid Bearer token returns 201`() = testApplication {
        val now = Clock.System.now()
        val testUser = User(
            id = "user-test-1",
            email = "test@example.com",
            username = "TestUser",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )
        val userService = mockk<UserService>()
        val collaborationService = mockk<CollaborationService>()
        // Any token accepted so test is independent of exact header format from test client
        coEvery { userService.validateSession(any()) } returns Either.Right(testUser)

        val session = CollaborationSession(
            id = "session-1",
            itemId = "item-1",
            createdAt = now,
            expiresAt = now + 24.hours,
            participants = emptyList(),
        )
        val participant = CollaborationParticipant(
            id = "participant-1",
            userId = testUser.id,
            userName = testUser.username,
            color = "#3498DB",
            joinedAt = now,
            lastActiveAt = now,
        )
        coEvery {
            collaborationService.joinSession(any(), any(), any())
        } returns (session to participant).right()

        application {
            (this as Application).install(Koin) {
                modules(
                    module {
                        single<UserService> { userService }
                        single<CollaborationService> { collaborationService }
                    },
                )
            }
            (this as Application).install(ServerContentNegotiation) {
                json(json)
            }
            (this as Application).configureSecurity()
            (this as Application).routing {
                route("/api/v1/collaboration") {
                    authenticate("auth-bearer") {
                        route("/sessions") {
                            post("/join") {
                                val request = call.receive<JoinSessionRequest>()
                                val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                                collaborationService.joinSession(
                                    com.vaultstadio.core.domain.service.JoinSessionInput(request.itemId),
                                    user.id,
                                    user.username,
                                ).fold(
                                    { call.respond(HttpStatusCode.InternalServerError, it.message ?: "Error") },
                                    { (s, p) ->
                                        call.respond(
                                            HttpStatusCode.Created,
                                            CollaborationSessionResponse(
                                                id = s.id,
                                                itemId = s.itemId,
                                                participantId = p.id,
                                                participants = s.participants.map { part ->
                                                    ParticipantResponse(
                                                        id = part.id,
                                                        userId = part.userId,
                                                        userName = part.userName,
                                                        color = part.color,
                                                        cursor = null,
                                                        selection = null,
                                                        isEditing = part.isEditing,
                                                    )
                                                },
                                                documentVersion = 0,
                                                createdAt = s.createdAt.toString(),
                                                expiresAt = s.expiresAt.toString(),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        val response = client.post("/api/v1/collaboration/sessions/join") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $VALID_TOKEN")
            setBody(JoinSessionRequest(itemId = "item-1"))
        }

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 but got ${response.status}")
    }
}
