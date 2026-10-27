package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.domain.common.exception.AuthorizationException

internal suspend fun CollaborationSocketConnection.command(message: WebSocketMessage) {
    authorize(message)
    when (message.type) {
        "ping" -> send(SocketResponse("pong", timestamp = System.currentTimeMillis()))
        "join" -> join(message)
        "cursor_update" -> cursor(message)
        "selection_update" -> selection(message)
        "operation" -> operation(message)
        "presence" -> presence(message)
        else -> throw IllegalArgumentException("Unknown message type")
    }
}

private suspend fun CollaborationSocketConnection.cursor(message: WebSocketMessage) {
    val cursor = message.cursor ?: throw IllegalArgumentException("Missing cursor")
    service.updateCursor(session.id, participantId(), cursor, actor).socketValue()
    broadcast(SocketResponse("cursor_update", userId = actor, cursor = cursor))
}

private suspend fun CollaborationSocketConnection.selection(message: WebSocketMessage) {
    service.updateSelection(session.id, participantId(), message.selection, actor).socketValue()
    broadcast(SocketResponse("selection_update", userId = actor, selection = message.selection))
}

private suspend fun CollaborationSocketConnection.operation(message: WebSocketMessage) {
    val request = message.operation ?: throw IllegalArgumentException("Missing operation")
    val state = service.applyOperation(session.itemId, request.toOperation(actor), actor).socketValue()
    send(SocketResponse("ack_operation", newVersion = state.version))
    broadcast(SocketResponse("operation", userId = actor, operation = request, newVersion = state.version))
}

private suspend fun CollaborationSocketConnection.presence(message: WebSocketMessage) {
    val presence = message.presence ?: throw IllegalArgumentException("Missing presence")
    if (presence.activeDocument != null && presence.activeDocument != session.itemId) {
        throw AuthorizationException()
    }
    service.updatePresence(actor, presence.status, session.id, presence.activeDocument).socketValue()
    broadcast(SocketResponse("presence_update", userId = actor, status = presence.status.name))
}
