package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.api.config.user
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.domain.common.exception.StorageException
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val collaborationMessageJson = Json { ignoreUnknownKeys = true }

fun Route.collaborationWebSocket(service: CollaborationService) {
    authenticate("auth-bearer") {
        webSocket("/api/v1/collaboration/ws/{sessionId}") {
            val actor = call.user
            val sessionId = call.parameters["sessionId"]
            if (actor == null || sessionId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }
            val session = service.getSession(sessionId, actor.id).getOrNull()
            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Access denied"))
                return@webSocket
            }
            val connection = CollaborationSocketConnection(session, actor.id, actor.username, this, service)
            serve(connection)
        }
    }
}

private suspend fun DefaultWebSocketServerSession.serve(connection: CollaborationSocketConnection) {
    try {
        for (frame in incoming) {
            if (frame is Frame.Text) connection.receive(frame.readText())
        }
    } finally {
        connection.disconnect()
    }
}

private suspend fun CollaborationSocketConnection.receive(text: String) {
    try {
        command(collaborationMessageJson.decodeFromString<WebSocketMessage>(text))
    } catch (error: CancellationException) {
        throw error
    } catch (error: StorageException) {
        send(
            SocketResponse(
                "error",
                code = error.httpStatus,
                message = if (error.httpStatus >= 500) {
                    "Collaboration operation failed"
                } else {
                    error.message
                },
            ),
        )
    } catch (expectedMalformed: SerializationException) {
        send(SocketResponse("error", code = 400, message = "Invalid message format"))
    } catch (expectedMalformed: IllegalArgumentException) {
        send(SocketResponse("error", code = 400, message = "Invalid collaboration command"))
    }
}
