package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * An operation in the operational transformation system.
 * These operations can be composed and transformed for conflict resolution.
 */
@Serializable
sealed class CollaborationOperation {
    abstract val id: String
    abstract val userId: String
    abstract val timestamp: Instant
    abstract val baseVersion: Long

    /**
     * Insert text at a position.
     */
    @Serializable
    data class Insert(
        override val id: String = UUID.randomUUID().toString(),
        override val userId: String,
        override val timestamp: Instant,
        override val baseVersion: Long,
        val position: Int,
        val text: String,
    ) : CollaborationOperation()

    /**
     * Delete text at a range.
     */
    @Serializable
    data class Delete(
        override val id: String = UUID.randomUUID().toString(),
        override val userId: String,
        override val timestamp: Instant,
        override val baseVersion: Long,
        val position: Int,
        val length: Int,
        val deletedText: String? = null,
    ) : CollaborationOperation()

    /**
     * Retain/skip characters (no-op).
     */
    @Serializable
    data class Retain(
        override val id: String = UUID.randomUUID().toString(),
        override val userId: String,
        override val timestamp: Instant,
        override val baseVersion: Long,
        val count: Int,
    ) : CollaborationOperation()
}
