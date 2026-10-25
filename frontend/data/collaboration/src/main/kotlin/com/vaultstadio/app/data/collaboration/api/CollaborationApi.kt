/**
 * Collaboration API
 */

package com.vaultstadio.app.data.collaboration.api

import com.vaultstadio.app.data.collaboration.dto.CollaborationParticipantDTO
import com.vaultstadio.app.data.collaboration.dto.CollaborationSessionDTO
import com.vaultstadio.app.data.collaboration.dto.DocumentCommentDTO
import com.vaultstadio.app.data.collaboration.dto.DocumentStateDTO
import com.vaultstadio.app.data.collaboration.dto.UserPresenceDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient

class CollaborationApi(client: HttpClient) : BaseApi(client) {

    suspend fun joinSession(itemId: String): ApiResult<CollaborationSessionDTO> =
        post("/api/v1/collaboration/sessions/join", mapOf("itemId" to itemId))

    suspend fun leaveSession(sessionId: String): ApiResult<Unit> =
        postNoBody("/api/v1/collaboration/sessions/$sessionId/leave")

    suspend fun getSession(sessionId: String): ApiResult<CollaborationSessionDTO> =
        get("/api/v1/collaboration/sessions/$sessionId")

    suspend fun getParticipants(sessionId: String): ApiResult<List<CollaborationParticipantDTO>> =
        get("/api/v1/collaboration/sessions/$sessionId/participants")

    suspend fun getDocumentState(itemId: String): ApiResult<DocumentStateDTO> =
        get("/api/v1/collaboration/documents/$itemId")

    suspend fun saveDocument(itemId: String): ApiResult<Unit> =
        postNoBody("/api/v1/collaboration/documents/$itemId/save")

    suspend fun getComments(itemId: String, includeResolved: Boolean): ApiResult<List<DocumentCommentDTO>> =
        get(
            "/api/v1/collaboration/documents/$itemId/comments",
            mapOf("includeResolved" to includeResolved.toString()),
        )

    suspend fun createComment(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): ApiResult<Unit> =
        post(
            "/api/v1/collaboration/documents/$itemId/comments",
            mapOf(
                "content" to content,
                "startLine" to startLine.toString(),
                "startColumn" to startColumn.toString(),
                "endLine" to endLine.toString(),
                "endColumn" to endColumn.toString(),
            ),
        )

    suspend fun resolveComment(itemId: String, commentId: String): ApiResult<Unit> =
        postNoBody("/api/v1/collaboration/documents/$itemId/comments/$commentId/resolve")

    suspend fun deleteComment(itemId: String, commentId: String): ApiResult<Unit> =
        delete("/api/v1/collaboration/documents/$itemId/comments/$commentId")

    suspend fun updatePresence(status: String, activeDocument: String?): ApiResult<Unit> {
        val body = mutableMapOf("status" to status)
        activeDocument?.let { body["activeDocument"] = it }
        return post("/api/v1/collaboration/presence", body)
    }

    suspend fun getUserPresence(userIds: List<String>): ApiResult<List<UserPresenceDTO>> =
        get("/api/v1/collaboration/presence", mapOf("userIds" to userIds.joinToString(",")))

    suspend fun setOffline(): ApiResult<Unit> = postNoBody("/api/v1/collaboration/presence/offline")
}
