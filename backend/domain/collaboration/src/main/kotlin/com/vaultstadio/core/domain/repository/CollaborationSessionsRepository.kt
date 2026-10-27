package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.datetime.Instant

interface CollaborationSessionsRepository {
    /**
     * Create a new collaboration session.
     *
     * @param session The session to create
     * @return The created session
     */
    suspend fun createSession(session: CollaborationSession): Either<StorageException, CollaborationSession>

    /**
     * Find a session by ID.
     *
     * @param sessionId Session ID
     * @return The session or null
     */
    suspend fun findSession(sessionId: String): Either<StorageException, CollaborationSession?>

    /**
     * Find active session for an item.
     *
     * @param itemId Storage item ID
     * @return Active session or null
     */
    suspend fun findActiveSessionForItem(itemId: String): Either<StorageException, CollaborationSession?>

    /**
     * Update a session.
     *
     * @param session Updated session
     * @return The updated session
     */
    suspend fun updateSession(session: CollaborationSession): Either<StorageException, CollaborationSession>

    /**
     * Close a session.
     *
     * @param sessionId Session ID
     * @return Unit on success
     */
    suspend fun closeSession(sessionId: String): Either<StorageException, Unit>

    /**
     * List active sessions for a user.
     *
     * @param userId User ID
     * @return List of active sessions
     */
    suspend fun listUserSessions(userId: String): Either<StorageException, List<CollaborationSession>>

    /**
     * Clean up expired sessions.
     *
     * @param expiredBefore Sessions expired before this timestamp
     * @return Number of sessions cleaned up
     */
    suspend fun cleanupExpiredSessions(expiredBefore: Instant): Either<StorageException, Int>
}
