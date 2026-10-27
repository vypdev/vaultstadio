package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.domain.collaboration.repository.CollaborationMembershipRepository
import com.vaultstadio.domain.collaboration.repository.CollaborationScopeRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import kotlinx.coroutines.flow.Flow

/** Actor-scoped application boundary; adapters must never call persistence in place of this policy. */
class CollaborationService(
    collaborationRepository: CollaborationRepository,
    storageItemRepository: StorageItemRepository,
    scopeRepository: CollaborationScopeRepository,
    membershipRepository: CollaborationMembershipRepository,
) {
    private val access = CollaborationAccess(storageItemRepository, scopeRepository, collaborationRepository)
    private val sessions = CollaborationSessions(access, membershipRepository)
    private val documents = CollaborationDocuments(access, collaborationRepository)
    private val comments = CollaborationComments(access, collaborationRepository)
    private val presence = CollaborationPresence(access, collaborationRepository)
    private val streams = CollaborationStreams(access, presence, collaborationRepository)

    suspend fun joinSession(
        input: JoinSessionInput,
        userId: String,
        userName: String,
    ): Either<StorageException, Pair<CollaborationSession, CollaborationParticipant>> = sessions.join(
        input,
        userId,
        userName,
    )

    suspend fun leaveSession(sessionId: String, participantId: String, userId: String): Either<StorageException, Unit> =
        sessions.leave(sessionId, participantId, userId)

    suspend fun getSession(sessionId: String, userId: String): Either<StorageException, CollaborationSession> =
        access.session(sessionId, userId).map { it.session }

    suspend fun getParticipants(
        sessionId: String,
        userId: String,
    ): Either<StorageException, List<CollaborationParticipant>> =
        access.session(sessionId, userId).map { it.session.participants }

    suspend fun authorizeParticipant(
        sessionId: String,
        participantId: String,
        userId: String,
        itemId: String,
    ): Either<StorageException, CollaborationParticipant> = access.participant(
        sessionId,
        participantId,
        userId,
    ).flatMap {
        if (it.context.item.id != itemId) AuthorizationException().left() else it.participant.right()
    }

    suspend fun updateCursor(
        sessionId: String,
        participantId: String,
        cursor: CursorPosition,
        userId: String,
    ): Either<StorageException, CollaborationParticipant> = sessions.cursor(sessionId, participantId, cursor, userId)

    suspend fun updateSelection(
        sessionId: String,
        participantId: String,
        selection: TextSelection?,
        userId: String,
    ): Either<StorageException, CollaborationParticipant> = sessions.selection(
        sessionId,
        participantId,
        selection,
        userId,
    )

    suspend fun getDocumentState(itemId: String, userId: String): Either<StorageException, DocumentState?> =
        documents.read(itemId, userId)

    suspend fun applyOperation(
        itemId: String,
        operation: CollaborationOperation,
        userId: String,
    ): Either<StorageException, DocumentState> = documents.apply(itemId, operation, userId)

    suspend fun getOperationsSince(
        itemId: String,
        sinceVersion: Long,
        userId: String,
    ): Either<StorageException, List<CollaborationOperation>> = documents.operations(itemId, sinceVersion, userId)

    suspend fun saveDocument(itemId: String, userId: String): Either<StorageException, DocumentState> =
        documents.save(itemId, userId)

    suspend fun streamOperations(
        itemId: String,
        userId: String,
    ): Either<StorageException, Flow<CollaborationOperation>> = streams.operations(itemId, userId)

    suspend fun updatePresence(
        userId: String,
        status: PresenceStatus,
        activeSession: String? = null,
        activeDocument: String? = null,
    ): Either<StorageException, UserPresence> = presence.update(userId, status, activeSession, activeDocument)

    suspend fun getPresence(
        userIds: List<String>,
        userId: String,
    ): Either<StorageException, Map<String, UserPresence>> =
        presence.read(userIds, userId)

    suspend fun setOffline(userId: String): Either<StorageException, Unit> = presence.offline(userId)

    suspend fun streamPresence(
        userIds: List<String>,
        userId: String,
    ): Either<StorageException, Flow<UserPresence>> = streams.presence(userIds, userId)

    suspend fun createComment(input: CreateCommentInput, userId: String): Either<StorageException, DocumentComment> =
        comments.create(input, userId)

    suspend fun getComments(
        itemId: String,
        userId: String,
        includeResolved: Boolean = false,
    ): Either<StorageException, List<DocumentComment>> = comments.list(itemId, userId, includeResolved)

    suspend fun resolveComment(
        itemId: String,
        commentId: String,
        userId: String,
    ): Either<StorageException, DocumentComment> =
        comments.resolve(itemId, commentId, userId)

    suspend fun addReply(
        itemId: String,
        commentId: String,
        content: String,
        userId: String,
    ): Either<StorageException, DocumentComment> = comments.reply(itemId, commentId, content, userId)

    suspend fun deleteComment(itemId: String, commentId: String, userId: String): Either<StorageException, Unit> =
        comments.delete(itemId, commentId, userId)

    suspend fun streamComments(itemId: String, userId: String): Either<StorageException, Flow<DocumentComment>> =
        streams.comments(itemId, userId)

    suspend fun cleanupExpiredSessions(userId: String): Either<StorageException, Int> = sessions.cleanup(userId)
}
