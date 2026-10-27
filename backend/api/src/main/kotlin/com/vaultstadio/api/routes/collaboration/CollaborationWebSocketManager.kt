package com.vaultstadio.api.routes.collaboration

import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

internal data class CollaborationSocketPeer(
    val sessionId: String,
    val socket: WebSocketSession,
    val authorized: suspend () -> Boolean,
)

/** A connection ID, rather than an account ID, is the identity of a live transport. */
object CollaborationWebSocketManager {
    private val connections = ConcurrentHashMap<String, CollaborationSocketPeer>()

    internal fun addConnection(id: String, peer: CollaborationSocketPeer) {
        check(connections.putIfAbsent(id, peer) == null)
    }

    internal fun removeConnection(id: String, peer: CollaborationSocketPeer) {
        connections.remove(id, peer)
    }

    internal suspend fun broadcast(session: String, message: SocketResponse, excludeConnection: String?) {
        for ((id, peer) in connections.entries.toList()) {
            if (peer.sessionId == session && id != excludeConnection) deliver(id, peer, message)
        }
    }

    private suspend fun deliver(id: String, peer: CollaborationSocketPeer, message: SocketResponse) {
        try {
            if (peer.authorized()) {
                peer.socket.send(Frame.Text(message.encode()))
            } else {
                removeConnection(id, peer)
                peer.socket.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Access denied"))
            }
        } catch (expectedTransport: IOException) {
            removeConnection(id, peer)
        } catch (expectedTransport: ClosedSendChannelException) {
            // A broken transport cannot retain a registered recipient or break delivery to another transport.
            removeConnection(id, peer)
        }
    }

    fun getParticipantCount(sessionId: String): Int = connections.values.count { it.sessionId == sessionId }
}
