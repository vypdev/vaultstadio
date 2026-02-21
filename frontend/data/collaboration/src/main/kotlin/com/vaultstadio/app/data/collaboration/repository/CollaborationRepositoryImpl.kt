/**
 * Collaboration Repository implementation
 */

package com.vaultstadio.app.data.collaboration.repository

import com.vaultstadio.app.data.collaboration.service.CollaborationService
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.model.UserPresence
import com.vaultstadio.app.domain.result.Result

class CollaborationRepositoryImpl(
    private val service: CollaborationService,
) : CollaborationRepository {

    override suspend fun joinSession(itemId: String): Result<CollaborationSession> =
        service.joinSession(itemId).toResult()

    override suspend fun leaveSession(sessionId: String): Result<Unit> =
        service.leaveSession(sessionId).toResult()

    override suspend fun getSession(sessionId: String): Result<CollaborationSession> =
        service.getSession(sessionId).toResult()

    override suspend fun getParticipants(sessionId: String): Result<List<CollaborationParticipant>> =
        service.getParticipants(sessionId).toResult()

    override suspend fun getDocumentState(itemId: String): Result<DocumentState> =
        service.getDocumentState(itemId).toResult()

    override suspend fun saveDocument(itemId: String): Result<Unit> =
        service.saveDocument(itemId).toResult()

    override suspend fun getComments(
        itemId: String,
        includeResolved: Boolean,
    ): Result<List<DocumentComment>> =
        service.getComments(itemId, includeResolved).toResult()

    override suspend fun createComment(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Result<Unit> =
        service.createComment(itemId, content, startLine, startColumn, endLine, endColumn).toResult()

    override suspend fun resolveComment(itemId: String, commentId: String): Result<Unit> =
        service.resolveComment(itemId, commentId).toResult()

    override suspend fun deleteComment(itemId: String, commentId: String): Result<Unit> =
        service.deleteComment(itemId, commentId).toResult()

    override suspend fun updatePresence(
        status: PresenceStatus,
        activeDocument: String?,
    ): Result<Unit> =
        service.updatePresence(status, activeDocument).toResult()

    override suspend fun getUserPresence(userIds: List<String>): Result<List<UserPresence>> =
        service.getUserPresence(userIds).toResult()

    override suspend fun setOffline(): Result<Unit> = service.setOffline().toResult()
}
