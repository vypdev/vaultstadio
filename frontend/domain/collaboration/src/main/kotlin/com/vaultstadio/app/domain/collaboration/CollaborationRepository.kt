/**
 * Repository interface for collaboration operations.
 */

package com.vaultstadio.app.domain.collaboration

import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.model.UserPresence
import com.vaultstadio.app.domain.result.Result

interface CollaborationRepository {
    suspend fun joinSession(itemId: String): Result<CollaborationSession>
    suspend fun leaveSession(sessionId: String): Result<Unit>
    suspend fun getSession(sessionId: String): Result<CollaborationSession>
    suspend fun getParticipants(sessionId: String): Result<List<CollaborationParticipant>>
    suspend fun getDocumentState(itemId: String): Result<DocumentState>
    suspend fun saveDocument(itemId: String): Result<Unit>
    suspend fun getComments(itemId: String, includeResolved: Boolean = false): Result<List<DocumentComment>>
    suspend fun createComment(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Result<Unit>
    suspend fun resolveComment(itemId: String, commentId: String): Result<Unit>
    suspend fun deleteComment(itemId: String, commentId: String): Result<Unit>
    suspend fun updatePresence(status: PresenceStatus, activeDocument: String? = null): Result<Unit>
    suspend fun getUserPresence(userIds: List<String>): Result<List<UserPresence>>
    suspend fun setOffline(): Result<Unit>
}
