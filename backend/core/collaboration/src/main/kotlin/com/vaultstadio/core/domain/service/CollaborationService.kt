/**
 * VaultStadio Collaboration Service
 *
 * Business logic for real-time collaboration operations.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CommentAnchor
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.time.Duration.Companion.hours

/**
 * Input for joining a collaboration session.
 *
 * @property itemId Storage item ID to collaborate on
 */
data class JoinSessionInput(
    val itemId: String,
)

/**
 * Input for creating a comment.
 *
 * @property itemId Storage item ID
 * @property content Comment text
 * @property startLine Start line of anchor
 * @property startColumn Start column of anchor
 * @property endLine End line of anchor
 * @property endColumn End column of anchor
 * @property quotedText Text being commented on
 */
data class CreateCommentInput(
    val itemId: String,
    val content: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)

/**
 * Service for managing real-time collaboration.
 *
 * @property collaborationRepository Repository for collaboration persistence
 */
class CollaborationService(
    private val collaborationRepository: CollaborationRepository,
) {

    // List of available colors for participants
    private val participantColors = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
        "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F",
        "#BB8FCE", "#85C1E9", "#F8B500", "#00CED1",
    )

    // ========================================================================
    // Session Management
    // ========================================================================

    /**
     * Join or create a collaboration session for an item.
     *
     * @param input Join session input
     * @param userId User ID
     * @param userName User display name
     * @return The session and participant
     */
    suspend fun joinSession(
        input: JoinSessionInput,
        userId: String,
        userName: String,
    ): Either<StorageException, Pair<CollaborationSession, CollaborationParticipant>> {
        // Find or create session
        return collaborationRepository.findActiveSessionForItem(input.itemId).flatMap { existing ->
            val session = if (existing != null) {
                existing
            } else {
                val now = Clock.System.now()
                val newSession = CollaborationSession(
                    id = UUID.randomUUID().toString(),
                    itemId = input.itemId,
                    createdAt = now,
                    expiresAt = now.plus(24.hours),
                )
                collaborationRepository.createSession(newSession).fold(
                    { return it.left() },
                    { it },
                )
            }

            // Create participant
            val colorIndex = session.participantCount % participantColors.size
            val participant = CollaborationParticipant(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                color = participantColors[colorIndex],
                joinedAt = Clock.System.now(),
                lastActiveAt = Clock.System.now(),
            )

            // Add participant to session
            collaborationRepository.addParticipant(session.id, participant).map { updatedSession ->
                Pair(updatedSession, participant)
            }
        }
    }

    /**
     * Leave a collaboration session.
     *
     * @param sessionId Session ID
     * @param participantId Participant ID
     * @return Unit on success
     */
    suspend fun leaveSession(
        sessionId: String,
        participantId: String,
    ): Either<StorageException, Unit> {
        return collaborationRepository.removeParticipant(sessionId, participantId).flatMap { session ->
            // Close session if no participants remain
            if (session.participants.isEmpty()) {
                collaborationRepository.closeSession(sessionId)
            } else {
                Unit.right()
            }
        }
    }

    /**
     * Get a session by ID.
     *
     * @param sessionId Session ID
     * @return The session
     */
    suspend fun getSession(sessionId: String): Either<StorageException, CollaborationSession> {
        return collaborationRepository.findSession(sessionId).flatMap { session ->
            session?.right() ?: ItemNotFoundException(
                itemId = sessionId,
                message = "Session not found: $sessionId",
            ).left()
        }
    }

    /**
     * Get all participants in a session.
     *
     * @param sessionId Session ID
     * @return List of participants
     */
    suspend fun getParticipants(sessionId: String): Either<StorageException, List<CollaborationParticipant>> {
        return collaborationRepository.getParticipants(sessionId)
    }

    // ========================================================================
    // Cursor & Selection
    // ========================================================================

    /**
     * Update cursor position.
     *
     * @param sessionId Session ID
     * @param participantId Participant ID
     * @param cursor New cursor position
     * @return The updated participant
     */
    suspend fun updateCursor(
        sessionId: String,
        participantId: String,
        cursor: CursorPosition,
    ): Either<StorageException, CollaborationParticipant> {
        return collaborationRepository.getParticipants(sessionId).flatMap { participants ->
            val participant = participants.find { it.id == participantId }
            if (participant == null) {
                ItemNotFoundException(message = "Participant not found").left()
            } else {
                collaborationRepository.updateParticipant(
                    sessionId,
                    participant.copy(
                        cursor = cursor,
                        lastActiveAt = Clock.System.now(),
                    ),
                )
            }
        }
    }

    /**
     * Update text selection.
     *
     * @param sessionId Session ID
     * @param participantId Participant ID
     * @param selection New selection (null to clear)
     * @return The updated participant
     */
    suspend fun updateSelection(
        sessionId: String,
        participantId: String,
        selection: TextSelection?,
    ): Either<StorageException, CollaborationParticipant> {
        return collaborationRepository.getParticipants(sessionId).flatMap { participants ->
            val participant = participants.find { it.id == participantId }
            if (participant == null) {
                ItemNotFoundException(message = "Participant not found").left()
            } else {
                collaborationRepository.updateParticipant(
                    sessionId,
                    participant.copy(
                        selection = selection,
                        lastActiveAt = Clock.System.now(),
                    ),
                )
            }
        }
    }

    // ========================================================================
    // Document Operations (OT)
    // ========================================================================

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
    ): Either<StorageException, DocumentState> {
        return collaborationRepository.getDocumentState(itemId).flatMap { state ->
            if (state == null) {
                ItemNotFoundException(message = "Document state not found").left()
            } else {
                // Transform operation if needed (OT)
                val transformedOp = transformCollaborationOperation(operation, state)
                collaborationRepository.applyOperation(itemId, transformedOp)
            }
        }
    }

    /**
     * Get document state.
     *
     * @param itemId Storage item ID
     * @return Current document state
     */
    suspend fun getDocumentState(itemId: String): Either<StorageException, DocumentState?> {
        return collaborationRepository.getDocumentState(itemId)
    }

    /**
     * Get operations since a version.
     *
     * @param itemId Storage item ID
     * @param sinceVersion Version to start from
     * @return List of operations
     */
    suspend fun getOperationsSince(
        itemId: String,
        sinceVersion: Long,
    ): Either<StorageException, List<CollaborationOperation>> {
        return collaborationRepository.getOperationsSince(itemId, sinceVersion)
    }

    /**
     * Stream operations in real-time.
     *
     * @param itemId Storage item ID
     * @return Flow of operations
     */
    fun streamOperations(itemId: String): Flow<CollaborationOperation> {
        return collaborationRepository.streamOperations(itemId)
    }

    // ========================================================================
    // Presence
    // ========================================================================

    /**
     * Update user presence.
     *
     * @param userId User ID
     * @param status Presence status
     * @param activeSession Active session ID (if any)
     * @param activeDocument Active document ID (if any)
     * @return Updated presence
     */
    suspend fun updatePresence(
        userId: String,
        status: PresenceStatus,
        activeSession: String? = null,
        activeDocument: String? = null,
    ): Either<StorageException, UserPresence> {
        val presence = UserPresence(
            userId = userId,
            status = status,
            lastSeen = Clock.System.now(),
            activeSession = activeSession,
            activeDocument = activeDocument,
        )
        return collaborationRepository.updatePresence(presence)
    }

    /**
     * Set user as offline.
     *
     * @param userId User ID
     * @return Unit on success
     */
    suspend fun setOffline(userId: String): Either<StorageException, Unit> {
        return collaborationRepository.setOffline(userId)
    }

    /**
     * Get presence for multiple users.
     *
     * @param userIds List of user IDs
     * @return Map of user ID to presence
     */
    suspend fun getPresence(userIds: List<String>): Either<StorageException, Map<String, UserPresence>> {
        return collaborationRepository.getPresenceBulk(userIds)
    }

    /**
     * Stream presence updates.
     *
     * @param userIds User IDs to watch
     * @return Flow of presence updates
     */
    fun streamPresence(userIds: List<String>): Flow<UserPresence> {
        return collaborationRepository.streamPresence(userIds)
    }

    // ========================================================================
    // Comments
    // ========================================================================

    /**
     * Create a comment on a document.
     *
     * @param input Comment input
     * @param userId User ID
     * @return The created comment
     */
    suspend fun createComment(
        input: CreateCommentInput,
        userId: String,
    ): Either<StorageException, DocumentComment> {
        val now = Clock.System.now()
        val comment = DocumentComment(
            id = UUID.randomUUID().toString(),
            itemId = input.itemId,
            userId = userId,
            content = input.content,
            anchor = CommentAnchor(
                startLine = input.startLine,
                startColumn = input.startColumn,
                endLine = input.endLine,
                endColumn = input.endColumn,
                quotedText = input.quotedText,
            ),
            createdAt = now,
            updatedAt = now,
        )
        return collaborationRepository.createComment(comment)
    }

    /**
     * Get comments for a document.
     *
     * @param itemId Storage item ID
     * @param includeResolved Include resolved comments
     * @return List of comments
     */
    suspend fun getComments(
        itemId: String,
        includeResolved: Boolean = false,
    ): Either<StorageException, List<DocumentComment>> {
        return collaborationRepository.getCommentsForItem(itemId, includeResolved)
    }

    /**
     * Resolve a comment.
     *
     * @param commentId Comment ID
     * @return The resolved comment
     */
    suspend fun resolveComment(commentId: String): Either<StorageException, DocumentComment> {
        return collaborationRepository.resolveComment(commentId, Clock.System.now())
    }

    /**
     * Add a reply to a comment.
     *
     * @param commentId Comment ID
     * @param content Reply content
     * @param userId User ID
     * @return The updated comment
     */
    suspend fun addReply(
        commentId: String,
        content: String,
        userId: String,
    ): Either<StorageException, DocumentComment> {
        val reply = CommentReply(
            id = UUID.randomUUID().toString(),
            userId = userId,
            content = content,
            createdAt = Clock.System.now(),
        )
        return collaborationRepository.addReply(commentId, reply)
    }

    /**
     * Delete a comment.
     *
     * @param commentId Comment ID
     * @param userId User ID (for authorization)
     * @return Unit on success
     */
    suspend fun deleteComment(
        commentId: String,
        userId: String,
    ): Either<StorageException, Unit> {
        return collaborationRepository.findComment(commentId).flatMap { comment ->
            when {
                comment == null -> ItemNotFoundException(
                    itemId = commentId,
                    message = "Comment not found: $commentId",
                ).left()
                comment.userId != userId -> AuthorizationException(
                    message = "Not authorized to delete this comment",
                ).left()
                else -> collaborationRepository.deleteComment(commentId)
            }
        }
    }

    /**
     * Stream comment updates.
     *
     * @param itemId Storage item ID
     * @return Flow of comments
     */
    fun streamComments(itemId: String): Flow<DocumentComment> {
        return collaborationRepository.streamComments(itemId)
    }

    // ========================================================================
    // Maintenance
    // ========================================================================

    /**
     * Clean up expired sessions.
     *
     * @return Number of sessions cleaned up
     */
    suspend fun cleanupExpiredSessions(): Either<StorageException, Int> {
        return collaborationRepository.cleanupExpiredSessions(Clock.System.now())
    }
}
