package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

@Serializable
internal data class SocketResponse(
    val type: String,
    val participantId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val cursor: CursorPosition? = null,
    val selection: TextSelection? = null,
    val operation: OperationData? = null,
    val newVersion: Long? = null,
    val status: String? = null,
    val message: String? = null,
    val code: Int? = null,
    val timestamp: Long? = null,
) {
    fun encode(): String = Json.encodeToString(this)
}
