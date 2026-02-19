/**
 * VaultStadio Collaboration Routes
 *
 * API endpoints for real-time collaboration operations.
 */

package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.api.config.user
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.core.domain.service.CreateCommentInput
import com.vaultstadio.core.domain.service.JoinSessionInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Configure collaboration routes.
 */
fun Route.collaborationRoutes(collaborationService: CollaborationService) {
    authenticate("auth-bearer") {
        route("/api/v1/collaboration") {
            // ================================================================
            // Session Management
            // ================================================================

            route("/sessions") {
                // Join or create a session
                post("/join") {
                    val request = call.receive<JoinSessionRequest>()
                    val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val userId = user.id
                    val userName = user.username

                    val itemId = request.itemId.trim()
                    if (itemId.isEmpty()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "itemId is required and must not be empty"),
                        )
                    }

                    val input = JoinSessionInput(itemId = itemId)

                    collaborationService.joinSession(input, userId, userName).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { (session, participant) ->
                            call.respond(
                                HttpStatusCode.Created,
                                CollaborationSessionResponse(
                                    id = session.id,
                                    itemId = session.itemId,
                                    participantId = participant.id,
                                    participants = session.participants.map { it.toResponse() },
                                    documentVersion = 0,
                                    createdAt = session.createdAt.toString(),
                                    expiresAt = session.expiresAt.toString(),
                                ),
                            )
                        },
                    )
                }

                // Leave a session
                post("/{sessionId}/leave") {
                    val sessionId = call.parameters["sessionId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing session ID")
                    val participantId = call.request.headers["X-Participant-ID"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing X-Participant-ID")

                    collaborationService.leaveSession(sessionId, participantId).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { call.respond(HttpStatusCode.OK, mapOf("message" to "Left session")) },
                    )
                }

                // Get session info
                get("/{sessionId}") {
                    val sessionId = call.parameters["sessionId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing session ID")

                    collaborationService.getSession(sessionId).fold(
                        { error -> call.respond(HttpStatusCode.NotFound, error.message ?: "Session not found") },
                        { session ->
                            if (session != null) {
                                call.respond(
                                    CollaborationSessionResponse(
                                        id = session.id,
                                        itemId = session.itemId,
                                        participantId = "",
                                        participants = session.participants.map { it.toResponse() },
                                        documentVersion = 0,
                                        createdAt = session.createdAt.toString(),
                                        expiresAt = session.expiresAt.toString(),
                                    ),
                                )
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Session not found")
                            }
                        },
                    )
                }

                // Get participants
                get("/{sessionId}/participants") {
                    val sessionId = call.parameters["sessionId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing session ID")

                    collaborationService.getParticipants(sessionId).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { participants -> call.respond(participants.map { it.toResponse() }) },
                    )
                }

                // Update cursor
                post("/{sessionId}/cursor") {
                    val sessionId = call.parameters["sessionId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing session ID")
                    val participantId = call.request.headers["X-Participant-ID"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing X-Participant-ID")
                    val request = call.receive<UpdateCursorRequest>()

                    val cursor = CursorPosition(
                        line = request.line,
                        column = request.column,
                        offset = request.offset,
                    )

                    collaborationService.updateCursor(sessionId, participantId, cursor).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { call.respond(HttpStatusCode.OK) },
                    )
                }

                // Update selection
                post("/{sessionId}/selection") {
                    val sessionId = call.parameters["sessionId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing session ID")
                    val participantId = call.request.headers["X-Participant-ID"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing X-Participant-ID")
                    val request = call.receive<UpdateSelectionRequest>()

                    val selection = TextSelection(
                        start = CursorPosition(
                            line = request.start.line,
                            column = request.start.column,
                            offset = request.start.offset,
                        ),
                        end = CursorPosition(
                            line = request.end.line,
                            column = request.end.column,
                            offset = request.end.offset,
                        ),
                    )

                    collaborationService.updateSelection(sessionId, participantId, selection).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { call.respond(HttpStatusCode.OK) },
                    )
                }
            }

            // ================================================================
            // Document Operations
            // ================================================================

            route("/documents") {
                // Get document state
                get("/{itemId}") {
                    val itemId = call.parameters["itemId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing item ID")

                    collaborationService.getDocumentState(itemId).fold(
                        { error -> call.respond(HttpStatusCode.NotFound, error.message ?: "Document not found") },
                        { state ->
                            if (state != null) {
                                call.respond(
                                    DocumentStateResponse(
                                        itemId = state.itemId,
                                        version = state.version,
                                        content = state.content,
                                        lastModified = state.lastModified.toString(),
                                    ),
                                )
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Document not found")
                            }
                        },
                    )
                }

                // Apply an operation
                post("/{itemId}/operations") {
                    val itemId = call.parameters["itemId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                    val request = call.receive<OperationRequest>()
                    val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val userId = user.id

                    val now = kotlinx.datetime.Clock.System.now()
                    val operation = when (request.type.uppercase()) {
                        "INSERT" -> CollaborationOperation.Insert(
                            userId = userId,
                            timestamp = now,
                            baseVersion = request.baseVersion,
                            position = request.position,
                            text = request.text ?: "",
                        )
                        "DELETE" -> CollaborationOperation.Delete(
                            userId = userId,
                            timestamp = now,
                            baseVersion = request.baseVersion,
                            position = request.position,
                            length = request.length ?: 0,
                        )
                        else -> CollaborationOperation.Retain(
                            userId = userId,
                            timestamp = now,
                            baseVersion = request.baseVersion,
                            count = request.length ?: 0,
                        )
                    }

                    collaborationService.applyOperation(itemId, operation).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { state ->
                            call.respond(
                                mapOf(
                                    "success" to true,
                                    "newVersion" to state.version,
                                ),
                            )
                        },
                    )
                }

                // Get operations since version
                get("/{itemId}/operations") {
                    val itemId = call.parameters["itemId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                    val sinceVersion = call.request.queryParameters["since"]?.toLongOrNull() ?: 0

                    collaborationService.getOperationsSince(itemId, sinceVersion).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { operations ->
                            call.respond(
                                operations.map { op ->
                                    when (op) {
                                        is CollaborationOperation.Insert -> mapOf(
                                            "type" to "INSERT",
                                            "position" to op.position,
                                            "text" to op.text,
                                            "baseVersion" to op.baseVersion,
                                        )
                                        is CollaborationOperation.Delete -> mapOf(
                                            "type" to "DELETE",
                                            "position" to op.position,
                                            "length" to op.length,
                                            "baseVersion" to op.baseVersion,
                                        )
                                        is CollaborationOperation.Retain -> mapOf(
                                            "type" to "RETAIN",
                                            "position" to 0,
                                            "length" to op.count,
                                            "baseVersion" to op.baseVersion,
                                        )
                                    }
                                },
                            )
                        },
                    )
                }

                // Save document (just returns current state)
                post("/{itemId}/save") {
                    val itemId = call.parameters["itemId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing item ID")

                    collaborationService.getDocumentState(itemId).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { _ -> call.respond(HttpStatusCode.OK, mapOf("message" to "Document saved")) },
                    )
                }
            }

            // ================================================================
            // Comments
            // ================================================================

            route("/documents/{itemId}/comments") {
                // Get comments
                get {
                    val itemId = call.parameters["itemId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                    val includeResolved = call.request.queryParameters["includeResolved"]?.toBoolean() ?: false

                    collaborationService.getComments(itemId, includeResolved).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { comments -> call.respond(comments.map { it.toResponse() }) },
                    )
                }

                // Create a comment
                post {
                    val itemId = call.parameters["itemId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                    val request = call.receive<CreateCommentRequest>()
                    val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val userId = user.id

                    val input = CreateCommentInput(
                        itemId = itemId,
                        content = request.content,
                        startLine = request.startLine,
                        startColumn = request.startColumn,
                        endLine = request.endLine,
                        endColumn = request.endColumn,
                        quotedText = request.quotedText,
                    )

                    collaborationService.createComment(input, userId).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { comment -> call.respond(HttpStatusCode.Created, mapOf("id" to comment.id)) },
                    )
                }

                // Resolve a comment
                post("/{commentId}/resolve") {
                    val commentId = call.parameters["commentId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing comment ID")

                    collaborationService.resolveComment(commentId).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { call.respond(HttpStatusCode.OK, mapOf("message" to "Comment resolved")) },
                    )
                }

                // Add a reply
                post("/{commentId}/replies") {
                    val commentId = call.parameters["commentId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing comment ID")
                    val request = call.receive<AddReplyRequest>()
                    val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val userId = user.id

                    collaborationService.addReply(commentId, userId, request.content).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { comment ->
                            call.respond(
                                HttpStatusCode.Created,
                                mapOf(
                                    "id" to (comment.replies.lastOrNull()?.id ?: ""),
                                ),
                            )
                        },
                    )
                }

                // Delete a comment
                delete("/{commentId}") {
                    val commentId = call.parameters["commentId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing comment ID")
                    val user = call.user ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                    val userId = user.id

                    collaborationService.deleteComment(commentId, userId).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { call.respond(HttpStatusCode.NoContent) },
                    )
                }
            }

            // ================================================================
            // Presence
            // ================================================================

            route("/presence") {
                // Update presence
                post {
                    val request = call.receive<UpdatePresenceRequest>()
                    val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val userId = user.id

                    val status = try {
                        PresenceStatus.valueOf(request.status.uppercase())
                    } catch (e: Exception) {
                        PresenceStatus.ONLINE
                    }

                    collaborationService.updatePresence(
                        userId = userId,
                        status = status,
                        activeDocument = request.activeDocument,
                    ).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { call.respond(HttpStatusCode.OK) },
                    )
                }

                // Get presence for users
                get {
                    val userIds = call.request.queryParameters["userIds"]?.split(",") ?: emptyList()

                    collaborationService.getPresence(userIds).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { presenceMap ->
                            call.respond(
                                presenceMap.map { (userId, presence) ->
                                    UserPresenceResponse(
                                        userId = userId,
                                        userName = null,
                                        status = presence.status.name,
                                        lastSeen = presence.lastSeen.toString(),
                                        activeDocument = presence.activeDocument,
                                    )
                                },
                            )
                        },
                    )
                }

                // Set offline (on disconnect)
                post("/offline") {
                    val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val userId = user.id

                    collaborationService.setOffline(userId).fold(
                        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                        { call.respond(HttpStatusCode.OK) },
                    )
                }
            }
        }
    }

    collaborationWebSocket(collaborationService)
}
