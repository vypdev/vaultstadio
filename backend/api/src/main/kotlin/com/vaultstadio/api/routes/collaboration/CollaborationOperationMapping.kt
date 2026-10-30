package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.core.domain.model.CollaborationOperation
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

internal fun OperationData.toOperation(actor: String): CollaborationOperation {
    val now = Clock.System.now()
    return when (type.uppercase()) {
        "INSERT" -> CollaborationOperation.Insert(
            userId = actor,
            timestamp = now,
            baseVersion = baseVersion,
            position = position,
            text =
            text ?: "",
        )
        "DELETE" -> CollaborationOperation.Delete(
            userId = actor,
            timestamp = now,
            baseVersion = baseVersion,
            position = position,
            length =
            length ?: 0,
        )
        "RETAIN" -> CollaborationOperation.Retain(
            userId = actor,
            timestamp = now,
            baseVersion = baseVersion,
            count =
            length ?: 0,
        )
        else -> throw IllegalArgumentException("Unknown operation type")
    }
}

internal fun CollaborationOperation.response() = when (this) {
    is CollaborationOperation.Insert -> OperationResponse("INSERT", position, text, null, baseVersion)
    is CollaborationOperation.Delete -> OperationResponse("DELETE", position, null, length, baseVersion)
    is CollaborationOperation.Retain -> OperationResponse("RETAIN", 0, null, count, baseVersion)
}

@Serializable
internal data class OperationResponse(
    val type: String,
    val position: Int,
    val text: String?,
    val length: Int?,
    val baseVersion: Long,
)

@Serializable
internal data class OperationAcknowledgement(val success: Boolean, val newVersion: Long)
