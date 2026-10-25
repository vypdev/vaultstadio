/**
 * WebSocket endpoint and connection manager for collaboration real-time updates.
 * Extracted from CollaborationRoutes to keep the main file under the line limit.
 */

package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.api.config.user
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.service.CollaborationService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class WebSocketMessage(
    val type: String,
    val participantId: String? = null,
    val itemId: String? = null,
    val cursor: CursorPosition? = null,
    val selection: TextSelection? = null,
    val operation: OperationData? = null,
    val presence: PresenceData? = null,
)

@Serializable
data class OperationData(
    val type: String,
    val position: Int,
    val text: String? = null,
    val length: Int? = null,
    val baseVersion: Long,
)

@Serializable
data class PresenceData(
    val status: PresenceStatus,
    val activeDocument: String? = null,
)

object CollaborationWebSocketManager {
    private val connections = ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketSession>>()

    fun addConnection(sessionId: String, userId: String, session: WebSocketSession) {
        val sessionConnections = connections.computeIfAbsent(sessionId) { ConcurrentHashMap() }
        sessionConnections[userId] = session
    }

    fun removeConnection(sessionId: String, userId: String) {
        connections[sessionId]?.remove(userId)
        if (connections[sessionId]?.isEmpty() == true) {
            connections.remove(sessionId)
        }
    }

    suspend fun broadcast(sessionId: String, message: String, excludeUserId: String?) {
        connections[sessionId]?.forEach { (userId, session) ->
            if (userId != excludeUserId) {
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    // Connection may be closed
                }
            }
        }
    }

    fun getParticipantCount(sessionId: String): Int {
        return connections[sessionId]?.size ?: 0
    }
}

/**
 * Registers the collaboration WebSocket route. Call this from [collaborationRoutes] after the HTTP routes.
 */
fun Route.collaborationWebSocket(collaborationService: CollaborationService) {
    authenticate("auth-bearer") {
        webSocket("/api/v1/collaboration/ws/{sessionId}") {
            val sessionId = call.parameters["sessionId"]
            if (sessionId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing session ID"))
                return@webSocket
            }

            val user = call.user
            if (user == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }

            val userId = user.id
            val userName = user.username
            val json = Json { ignoreUnknownKeys = true }

            var participantId: String? = null

            try {
                CollaborationWebSocketManager.addConnection(sessionId, userId, this)

                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val message = json.decodeFromString<WebSocketMessage>(text)

                            when (message.type) {
                                "ping" -> {
                                    send(Frame.Text("""{"type":"pong","timestamp":${System.currentTimeMillis()}}"""))
                                }

                                "join" -> {
                                    participantId = message.participantId
                                    send(
                                        Frame.Text(
                                            """{"type":"joined","participantId":"$participantId","sessionId":"$sessionId"}""",
                                        ),
                                    )
                                    CollaborationWebSocketManager.broadcast(
                                        sessionId,
                                        """{"type":"participant_joined","userId":"$userId","userName":"$userName"}""",
                                        excludeUserId = userId,
                                    )
                                }

                                "cursor_update" -> {
                                    message.cursor?.let { cursor ->
                                        collaborationService.updateCursor(
                                            sessionId,
                                            message.participantId ?: participantId ?: return@let,
                                            cursor,
                                        )
                                        CollaborationWebSocketManager.broadcast(
                                            sessionId,
                                            """{"type":"cursor_update","userId":"$userId","cursor":{"line":${cursor.line},"column":${cursor.column},"offset":${cursor.offset}}}""",
                                            excludeUserId = userId,
                                        )
                                    }
                                }

                                "selection_update" -> {
                                    message.selection?.let { selection ->
                                        collaborationService.updateSelection(
                                            sessionId,
                                            message.participantId ?: participantId ?: return@let,
                                            selection,
                                        )
                                        val selectionJson = """{"start":{"line":${selection.start.line},""" +
                                            """column":${selection.start.column},""" +
                                            """offset":${selection.start.offset}},""" +
                                            """end":{"line":${selection.end.line},""" +
                                            """column":${selection.end.column},""" +
                                            """offset":${selection.end.offset}}}"""
                                        val msg = """{"type":"selection_update","userId":"$userId",""" +
                                            """"selection":$selectionJson}"""
                                        CollaborationWebSocketManager.broadcast(
                                            sessionId,
                                            msg,
                                            excludeUserId = userId,
                                        )
                                    }
                                }

                                "operation" -> {
                                    message.operation?.let { op ->
                                        val now = kotlinx.datetime.Clock.System.now()
                                        val operation = when (op.type.uppercase()) {
                                            "INSERT" -> CollaborationOperation.Insert(
                                                userId = userId,
                                                timestamp = now,
                                                baseVersion = op.baseVersion,
                                                position = op.position,
                                                text = op.text ?: "",
                                            )
                                            "DELETE" -> CollaborationOperation.Delete(
                                                userId = userId,
                                                timestamp = now,
                                                baseVersion = op.baseVersion,
                                                position = op.position,
                                                length = op.length ?: 0,
                                            )
                                            else -> CollaborationOperation.Retain(
                                                userId = userId,
                                                timestamp = now,
                                                baseVersion = op.baseVersion,
                                                count = op.length ?: 0,
                                            )
                                        }

                                        collaborationService.applyOperation(message.itemId ?: "", operation).fold(
                                            { error ->
                                                send(Frame.Text("""{"type":"error","message":"${error.message}"}"""))
                                            },
                                            { state ->
                                                send(
                                                    Frame.Text(
                                                        """{"type":"ack_operation","newVersion":${state.version}}""",
                                                    ),
                                                )
                                                val opJson = """{"type":"${op.type}","position":${op.position},"text":${op.text?.let {
                                                    "\"$it\""
                                                } ?: "null"},"length":${op.length ?: 0},"baseVersion":${op.baseVersion}}"""
                                                CollaborationWebSocketManager.broadcast(
                                                    sessionId,
                                                    """{"type":"operation","userId":"$userId","operation":$opJson,"newVersion":${state.version}}""",
                                                    excludeUserId = userId,
                                                )
                                            },
                                        )
                                    }
                                }

                                "presence" -> {
                                    message.presence?.let { presence ->
                                        collaborationService.updatePresence(
                                            userId,
                                            presence.status,
                                            activeDocument = presence.activeDocument,
                                        )
                                        CollaborationWebSocketManager.broadcast(
                                            sessionId,
                                            """{"type":"presence_update","userId":"$userId","status":"${presence.status.name}"}""",
                                            excludeUserId = userId,
                                        )
                                    }
                                }

                                else -> {
                                    send(
                                        Frame.Text(
                                            """{"type":"error","message":"Unknown message type: ${message.type}"}""",
                                        ),
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            send(Frame.Text("""{"type":"error","message":"Invalid message format: ${e.message}"}"""))
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle disconnection
            } finally {
                CollaborationWebSocketManager.removeConnection(sessionId, userId)
                CollaborationWebSocketManager.broadcast(
                    sessionId,
                    """{"type":"participant_left","userId":"$userId"}""",
                    excludeUserId = null,
                )
                participantId?.let { pid ->
                    collaborationService.leaveSession(sessionId, pid)
                }
            }
        }
    }
}
