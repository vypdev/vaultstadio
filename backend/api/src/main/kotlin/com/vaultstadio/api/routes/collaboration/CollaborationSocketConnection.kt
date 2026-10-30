package com.vaultstadio.api.routes.collaboration

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

internal class CollaborationSocketConnection(
    val session: CollaborationSession,
    val actor: String,
    private val name: String,
    private val socket: WebSocketSession,
    val service: CollaborationService,
) {
    private val id = UUID.randomUUID().toString()
    private var participant: String? = null
    private var peer: CollaborationSocketPeer? = null

    suspend fun authorize(message: WebSocketMessage) {
        val current = service.getSession(session.id, actor).socketValue()
        if (current.itemId != session.itemId) throw AuthorizationException()
        requireItemMatch(message)
        if (message.type != "join" && message.type != "ping") {
            val bound = participantId()
            requireParticipantMatch(message, bound)
            service.authorizeParticipant(session.id, bound, actor, session.itemId).socketValue()
        } else if (message.type == "ping" && participant != null) {
            service.authorizeParticipant(session.id, checkNotNull(participant), actor, session.itemId).socketValue()
        }
    }

    private fun requireItemMatch(message: WebSocketMessage) {
        if (message.itemId != null && message.itemId != session.itemId) throw AuthorizationException()
    }

    private fun requireParticipantMatch(message: WebSocketMessage, bound: String) {
        if (message.participantId != null && message.participantId != bound) throw AuthorizationException()
    }

    suspend fun join(message: WebSocketMessage) {
        val requested = message.participantId ?: throw AuthorizationException()
        if (participant != null && participant != requested) throw AuthorizationException()
        service.authorizeParticipant(session.id, requested, actor, session.itemId).socketValue()
        if (peer == null) {
            val recipient = CollaborationSocketPeer(session.id, socket) {
                service.authorizeParticipant(session.id, requested, actor, session.itemId).isRight()
            }
            participant = requested
            peer = recipient
            CollaborationWebSocketManager.addConnection(id, recipient)
            broadcast(SocketResponse("participant_joined", userId = actor, userName = name))
        }
        send(SocketResponse("joined", participantId = requested, sessionId = session.id))
    }

    fun participantId(): String = participant ?: throw AuthorizationException()

    suspend fun send(message: SocketResponse) = socket.send(Frame.Text(message.encode()))

    suspend fun broadcast(message: SocketResponse) =
        CollaborationWebSocketManager.broadcast(session.id, message, excludeConnection = id)

    suspend fun disconnect() {
        peer?.let { CollaborationWebSocketManager.removeConnection(id, it) }
        val bound = participant ?: return
        withContext(NonCancellable) {
            withTimeout(5_000) {
                if (service.leaveSession(session.id, bound, actor).isRight()) {
                    CollaborationWebSocketManager.broadcast(
                        session.id,
                        SocketResponse("participant_left", userId = actor),
                        excludeConnection = null,
                    )
                }
            }
        }
    }
}

internal fun <T> Either<StorageException, T>.socketValue(): T = fold({ throw it }, { it })
