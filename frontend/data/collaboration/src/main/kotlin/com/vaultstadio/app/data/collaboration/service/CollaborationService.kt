/**
 * Collaboration Service
 */

package com.vaultstadio.app.data.collaboration.service

import com.vaultstadio.app.data.collaboration.api.CollaborationApi
import com.vaultstadio.app.data.collaboration.mapper.toCommentList
import com.vaultstadio.app.data.collaboration.mapper.toDomain
import com.vaultstadio.app.data.collaboration.mapper.toParticipantList
import com.vaultstadio.app.data.collaboration.mapper.toPresenceList
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.model.UserPresence

class CollaborationService(private val api: CollaborationApi) {
    suspend fun joinSession(itemId: String): ApiResult<CollaborationSession> =
        api.joinSession(itemId).map { it.toDomain() }
    suspend fun leaveSession(sessionId: String): ApiResult<Unit> = api.leaveSession(sessionId)
    suspend fun getSession(sessionId: String): ApiResult<CollaborationSession> =
        api.getSession(sessionId).map { it.toDomain() }
    suspend fun getParticipants(sessionId: String): ApiResult<List<CollaborationParticipant>> =
        api.getParticipants(sessionId).map { it.toParticipantList() }
    suspend fun getDocumentState(itemId: String): ApiResult<DocumentState> =
        api.getDocumentState(itemId).map { it.toDomain() }
    suspend fun saveDocument(itemId: String): ApiResult<Unit> = api.saveDocument(itemId)
    suspend fun getComments(itemId: String, includeResolved: Boolean = false): ApiResult<List<DocumentComment>> =
        api.getComments(itemId, includeResolved).map { it.toCommentList() }
    suspend fun createComment(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): ApiResult<Unit> =
        api.createComment(itemId, content, startLine, startColumn, endLine, endColumn)
    suspend fun resolveComment(itemId: String, commentId: String): ApiResult<Unit> =
        api.resolveComment(itemId, commentId)
    suspend fun deleteComment(itemId: String, commentId: String): ApiResult<Unit> =
        api.deleteComment(itemId, commentId)
    suspend fun updatePresence(status: PresenceStatus, activeDocument: String? = null): ApiResult<Unit> =
        api.updatePresence(status.name, activeDocument)
    suspend fun getUserPresence(userIds: List<String>): ApiResult<List<UserPresence>> =
        api.getUserPresence(userIds).map { it.toPresenceList() }
    suspend fun setOffline(): ApiResult<Unit> = api.setOffline()
}
