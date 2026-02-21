/**
 * VaultStadio Upload Session Manager Port
 *
 * Interface for chunked upload session management.
 * Implementations may use in-memory, Redis, or database storage.
 */

package com.vaultstadio.core.domain.service

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Represents an ongoing chunked upload session.
 */
@Serializable
data class UploadSession(
    val id: String,
    val userId: String,
    val fileName: String,
    val totalSize: Long,
    val mimeType: String?,
    val parentId: String?,
    val chunkSize: Long,
    val totalChunks: Int,
    val receivedChunks: MutableSet<Int> = mutableSetOf(),
    val createdAt: Instant,
    val lastActivityAt: Instant = createdAt,
    val tempDir: String,
) {
    val isComplete: Boolean get() = receivedChunks.size == totalChunks
    val uploadedBytes: Long get() = receivedChunks.size.toLong() * chunkSize
    val progress: Float get() = receivedChunks.size.toFloat() / totalChunks.toFloat()
}

/**
 * Port for upload session management.
 */
interface UploadSessionManager {

    fun createSession(session: UploadSession): UploadSession
    fun getSession(uploadId: String): UploadSession?
    fun getSessionForUser(uploadId: String, userId: String): UploadSession?
    fun updateSession(session: UploadSession)
    fun markChunkReceived(uploadId: String, chunkIndex: Int): Boolean
    fun removeSession(uploadId: String): UploadSession?
    fun cleanupExpiredSessions(maxAge: Duration): Int
    fun getSessionsForUser(userId: String): List<UploadSession>
    fun getActiveSessionCount(): Int
}
