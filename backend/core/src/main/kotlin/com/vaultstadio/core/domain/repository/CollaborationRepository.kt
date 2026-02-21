/**
 * VaultStadio Collaboration Repository
 *
 * Interface for real-time collaboration persistence operations.
 */

package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository interface for collaboration features.
 */
interface CollaborationRepository {

    // ========================================================================
    // Session Management
    // ========================================================================

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

    // ========================================================================
    // Participant Management
    // ========================================================================

    /**
     * Add a participant to a session.
     *
     * @param sessionId Session ID
     * @param participant The participant to add
     * @return The updated session
     */
    suspend fun addParticipant(
        sessionId: String,
        participant: CollaborationParticipant,
    ): Either<StorageException, CollaborationSession>

    /**
     * Remove a participant from a session.
     *
     * @param sessionId Session ID
     * @param participantId Participant ID
     * @return The updated session
     */
    suspend fun removeParticipant(
        sessionId: String,
        participantId: String,
    ): Either<StorageException, CollaborationSession>

    /**
     * Update a participant's state.
     *
     * @param sessionId Session ID
     * @param participant Updated participant
     * @return The updated participant
     */
    suspend fun updateParticipant(
        sessionId: String,
        participant: CollaborationParticipant,
    ): Either<StorageException, CollaborationParticipant>

    /**
     * Get all participants in a session.
     *
     * @param sessionId Session ID
     * @return List of participants
     */
    suspend fun getParticipants(sessionId: String): Either<StorageException, List<CollaborationParticipant>>

    // ========================================================================
    // Document State & Operations
    // ========================================================================

    /**
     * Get or create document state.
     *
     * @param itemId Storage item ID
     * @return Current document state
     */
    suspend fun getDocumentState(itemId: String): Either<StorageException, DocumentState?>

    /**
     * Save document state.
     *
     * @param state Document state to save
     * @return The saved state
     */
    suspend fun saveDocumentState(state: DocumentState): Either<StorageException, DocumentState>

    /**
     * Apply an operation to a document.
     *
     * @param itemId Storage item ID
     * @param operation The operation to apply
     * @return The new document state
     */
    suspend fun applyOperation(
        itemId: String,
        operation: CollaborationOperation,
    ): Either<StorageException, DocumentState>

    /**
     * Get operations since a version.
     *
     * @param itemId Storage item ID
     * @param sinceVersion Get operations after this version
     * @return List of operations
     */
    suspend fun getOperationsSince(
        itemId: String,
        sinceVersion: Long,
    ): Either<StorageException, List<CollaborationOperation>>

    /**
     * Stream operations in real-time.
     *
     * @param itemId Storage item ID
     * @return Flow of operations
     */
    fun streamOperations(itemId: String): Flow<CollaborationOperation>

    // ========================================================================
    // Presence
    // ========================================================================

    /**
     * Update user presence.
     *
     * @param presence Updated presence
     * @return The updated presence
     */
    suspend fun updatePresence(presence: UserPresence): Either<StorageException, UserPresence>

    /**
     * Get presence for a user.
     *
     * @param userId User ID
     * @return Current presence or null
     */
    suspend fun getPresence(userId: String): Either<StorageException, UserPresence?>

    /**
     * Get presence for multiple users.
     *
     * @param userIds List of user IDs
     * @return Map of user ID to presence
     */
    suspend fun getPresenceBulk(userIds: List<String>): Either<StorageException, Map<String, UserPresence>>

    /**
     * Set user as offline.
     *
     * @param userId User ID
     * @return Unit on success
     */
    suspend fun setOffline(userId: String): Either<StorageException, Unit>

    /**
     * Get online users in a folder.
     *
     * @param folderId Folder ID (null for root)
     * @return List of online users
     */
    suspend fun getOnlineUsersInFolder(folderId: String?): Either<StorageException, List<UserPresence>>

    /**
     * Stream presence updates.
     *
     * @param userIds User IDs to watch
     * @return Flow of presence updates
     */
    fun streamPresence(userIds: List<String>): Flow<UserPresence>

    // ========================================================================
    // Comments
    // ========================================================================

    /**
     * Create a comment.
     *
     * @param comment The comment to create
     * @return The created comment
     */
    suspend fun createComment(comment: DocumentComment): Either<StorageException, DocumentComment>

    /**
     * Find a comment by ID.
     *
     * @param commentId Comment ID
     * @return The comment or null
     */
    suspend fun findComment(commentId: String): Either<StorageException, DocumentComment?>

    /**
     * Get comments for an item.
     *
     * @param itemId Storage item ID
     * @param includeResolved Include resolved comments
     * @return List of comments
     */
    suspend fun getCommentsForItem(
        itemId: String,
        includeResolved: Boolean = false,
    ): Either<StorageException, List<DocumentComment>>

    /**
     * Update a comment.
     *
     * @param comment Updated comment
     * @return The updated comment
     */
    suspend fun updateComment(comment: DocumentComment): Either<StorageException, DocumentComment>

    /**
     * Resolve a comment.
     *
     * @param commentId Comment ID
     * @param resolvedAt When it was resolved
     * @return The updated comment
     */
    suspend fun resolveComment(
        commentId: String,
        resolvedAt: Instant,
    ): Either<StorageException, DocumentComment>

    /**
     * Add a reply to a comment.
     *
     * @param commentId Comment ID
     * @param reply The reply to add
     * @return The updated comment
     */
    suspend fun addReply(
        commentId: String,
        reply: CommentReply,
    ): Either<StorageException, DocumentComment>

    /**
     * Delete a comment.
     *
     * @param commentId Comment ID
     * @return Unit on success
     */
    suspend fun deleteComment(commentId: String): Either<StorageException, Unit>

    /**
     * Stream comments for an item.
     *
     * @param itemId Storage item ID
     * @return Flow of comment updates
     */
    fun streamComments(itemId: String): Flow<DocumentComment>
}
