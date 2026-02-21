/**
 * VaultStadio Upload Session Manager – In-memory implementation.
 */

package com.vaultstadio.api.service

import com.vaultstadio.core.domain.service.UploadSession
import com.vaultstadio.core.domain.service.UploadSessionManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

/**
 * In-memory implementation of UploadSessionManager.
 */
class InMemoryUploadSessionManager : UploadSessionManager {

    private val sessions = ConcurrentHashMap<String, UploadSession>()

    override fun createSession(session: UploadSession): UploadSession {
        sessions[session.id] = session
        logger.debug { "Created upload session ${session.id} for file ${session.fileName}" }
        return session
    }

    override fun getSession(uploadId: String): UploadSession? =
        sessions[uploadId]

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
        val expiredSessions = sessions.values.filter { it.lastActivityAt < cutoff }
        for (session in expiredSessions) {
            removeSession(session.id)
        }
        if (expiredSessions.isNotEmpty()) {
            logger.info { "Cleaned up ${expiredSessions.size} expired upload sessions" }
        }
        return expiredSessions.size
    }

    override fun getSessionsForUser(userId: String): List<UploadSession> =
        sessions.values.filter { it.userId == userId }

    override fun getActiveSessionCount(): Int = sessions.size
}
