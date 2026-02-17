/**
 * Collaboration Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.CollaborationService
import com.vaultstadio.app.domain.model.CollaborationParticipant
import com.vaultstadio.app.domain.model.CollaborationSession
import com.vaultstadio.app.domain.model.DocumentComment
import com.vaultstadio.app.domain.model.DocumentState
import com.vaultstadio.app.domain.model.PresenceStatus
import com.vaultstadio.app.domain.model.UserPresence
import org.koin.core.annotation.Single

/**
 * Repository interface for collaboration operations.
 */
interface CollaborationRepository {
    suspend fun joinSession(itemId: String): ApiResult<CollaborationSession>
    suspend fun leaveSession(sessionId: String): ApiResult<Unit>
    suspend fun getSession(sessionId: String): ApiResult<CollaborationSession>
    suspend fun getParticipants(sessionId: String): ApiResult<List<CollaborationParticipant>>
    suspend fun getDocumentState(itemId: String): ApiResult<DocumentState>
    suspend fun saveDocument(itemId: String): ApiResult<Unit>
    suspend fun getComments(itemId: String, includeResolved: Boolean = false): ApiResult<List<DocumentComment>>
    suspend fun createComment(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): ApiResult<Unit>
    suspend fun resolveComment(itemId: String, commentId: String): ApiResult<Unit>
    suspend fun deleteComment(itemId: String, commentId: String): ApiResult<Unit>
    suspend fun updatePresence(status: PresenceStatus, activeDocument: String? = null): ApiResult<Unit>
    suspend fun getUserPresence(userIds: List<String>): ApiResult<List<UserPresence>>
    suspend fun setOffline(): ApiResult<Unit>
}

@Single(binds = [CollaborationRepository::class])
class CollaborationRepositoryImpl(
    private val service: CollaborationService,
) : CollaborationRepository {

    override suspend fun joinSession(itemId: String) = service.joinSession(itemId)
    override suspend fun leaveSession(sessionId: String) = service.leaveSession(sessionId)
    override suspend fun getSession(sessionId: String) = service.getSession(sessionId)
    override suspend fun getParticipants(sessionId: String) = service.getParticipants(sessionId)
    override suspend fun getDocumentState(itemId: String) = service.getDocumentState(itemId)
    override suspend fun saveDocument(itemId: String) = service.saveDocument(itemId)

    override suspend fun getComments(
        itemId: String,
        includeResolved: Boolean,
    ) = service.getComments(itemId, includeResolved)

    override suspend fun createComment(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ) = service.createComment(itemId, content, startLine, startColumn, endLine, endColumn)

    override suspend fun resolveComment(itemId: String, commentId: String) =
        service.resolveComment(itemId, commentId)

    override suspend fun deleteComment(itemId: String, commentId: String) =
        service.deleteComment(itemId, commentId)

    override suspend fun updatePresence(
        status: PresenceStatus,
        activeDocument: String?,
    ) = service.updatePresence(status, activeDocument)

    override suspend fun getUserPresence(userIds: List<String>) = service.getUserPresence(userIds)
    override suspend fun setOffline() = service.setOffline()
}
