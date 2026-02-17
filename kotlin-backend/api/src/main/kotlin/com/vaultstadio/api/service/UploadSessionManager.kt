/**
 * VaultStadio Upload Session Manager
 *
 * Manages chunked upload sessions with cleanup capabilities.
 * This implementation uses in-memory storage; for production,
 * consider using Redis or database-backed storage.
 */

package com.vaultstadio.api.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

/**
 * Represents an ongoing upload session.
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
 * Interface for upload session management.
 * Allows for different implementations (in-memory, Redis, database).
 */
interface UploadSessionManager {

    /**
     * Creates a new upload session.
     */
    fun createSession(session: UploadSession): UploadSession

    /**
     * Gets an upload session by ID.
     */
    fun getSession(uploadId: String): UploadSession?

    /**
     * Gets an upload session if it belongs to the specified user.
     */
    fun getSessionForUser(uploadId: String, userId: String): UploadSession?

    /**
     * Updates an upload session.
     */
    fun updateSession(session: UploadSession)

    /**
     * Marks a chunk as received.
     */
    fun markChunkReceived(uploadId: String, chunkIndex: Int): Boolean

    /**
     * Removes an upload session.
     */
    fun removeSession(uploadId: String): UploadSession?

    /**
     * Cleans up expired sessions.
     *
     * @param maxAge Maximum age of sessions to keep
     * @return Number of sessions cleaned up
     */
    fun cleanupExpiredSessions(maxAge: Duration = 24.hours): Int

    /**
     * Gets all sessions for a user.
     */
    fun getSessionsForUser(userId: String): List<UploadSession>

    /**
     * Gets the count of active sessions.
     */
    fun getActiveSessionCount(): Int
}

/**
 * In-memory implementation of UploadSessionManager.
 * Suitable for development and single-instance deployments.
 */
class InMemoryUploadSessionManager : UploadSessionManager {

    private val sessions = ConcurrentHashMap<String, UploadSession>()

    override fun createSession(session: UploadSession): UploadSession {
        sessions[session.id] = session
        logger.debug { "Created upload session ${session.id} for file ${session.fileName}" }
        return session
    }

    override fun getSession(uploadId: String): UploadSession? {
        return sessions[uploadId]
    }

    override fun getSessionForUser(uploadId: String, userId: String): UploadSession? {
        val session = sessions[uploadId]
        return if (session?.userId == userId) session else null
    }

    override fun updateSession(session: UploadSession) {
        sessions[session.id] = session
    }

    override fun markChunkReceived(uploadId: String, chunkIndex: Int): Boolean {
        val session = sessions[uploadId] ?: return false
        session.receivedChunks.add(chunkIndex)
        sessions[uploadId] = session.copy(lastActivityAt = Clock.System.now())
        return true
    }

    override fun removeSession(uploadId: String): UploadSession? {
        val session = sessions.remove(uploadId)
        if (session != null) {
            // Clean up temp directory
            try {
                val tempDir = File(session.tempDir)
                if (tempDir.exists()) {
                    tempDir.deleteRecursively()
                    logger.debug { "Cleaned up temp directory for session ${session.id}" }
                }
            } catch (e: Exception) {
                logger.warn { "Failed to clean up temp directory for session ${session.id}: ${e.message}" }
            }
        }
        return session
    }

    override fun cleanupExpiredSessions(maxAge: Duration): Int {
        val now = Clock.System.now()
        val cutoff = now - maxAge
        var cleanedCount = 0

        val expiredSessions = sessions.values.filter { session ->
            session.lastActivityAt < cutoff
        }

        for (session in expiredSessions) {
            removeSession(session.id)
            cleanedCount++
            logger.info { "Cleaned up expired upload session ${session.id} for file ${session.fileName}" }
        }

        if (cleanedCount > 0) {
            logger.info { "Cleaned up $cleanedCount expired upload sessions" }
        }

        return cleanedCount
    }

    override fun getSessionsForUser(userId: String): List<UploadSession> {
        return sessions.values.filter { it.userId == userId }
    }

    override fun getActiveSessionCount(): Int {
        return sessions.size
    }
}
