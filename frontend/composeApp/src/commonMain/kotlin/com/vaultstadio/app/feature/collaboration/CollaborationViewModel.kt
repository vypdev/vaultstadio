package com.vaultstadio.app.feature.collaboration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.CollaborationMessageType
import com.vaultstadio.app.data.network.CollaborationWebSocket
import com.vaultstadio.app.data.network.ConnectionState
import com.vaultstadio.app.domain.model.CollaborationParticipant
import com.vaultstadio.app.domain.model.CollaborationSession
import com.vaultstadio.app.domain.model.DocumentComment
import com.vaultstadio.app.domain.model.DocumentState
import com.vaultstadio.app.domain.model.PresenceStatus
import com.vaultstadio.app.domain.model.UserPresence
import com.vaultstadio.app.domain.usecase.auth.GetCurrentUserUseCase
import com.vaultstadio.app.domain.usecase.collaboration.CreateDocumentCommentUseCase
import com.vaultstadio.app.domain.usecase.collaboration.DeleteDocumentCommentUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetCollaborationSessionUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetDocumentCommentsUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetDocumentStateUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetSessionParticipantsUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetUserPresenceUseCase
import com.vaultstadio.app.domain.usecase.collaboration.JoinCollaborationSessionUseCase
import com.vaultstadio.app.domain.usecase.collaboration.LeaveCollaborationSessionUseCase
import com.vaultstadio.app.domain.usecase.collaboration.ResolveDocumentCommentUseCase
import com.vaultstadio.app.domain.usecase.collaboration.SaveDocumentUseCase
import com.vaultstadio.app.domain.usecase.collaboration.SetOfflineUseCase
import com.vaultstadio.app.domain.usecase.collaboration.UpdatePresenceUseCase
import com.vaultstadio.app.domain.usecase.config.GetCollaborationUrlUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

/**
 * ViewModel for real-time collaboration on documents.
 */
@KoinViewModel
class CollaborationViewModel(
    private val joinSessionUseCase: JoinCollaborationSessionUseCase,
    private val leaveSessionUseCase: LeaveCollaborationSessionUseCase,
    private val getSessionUseCase: GetCollaborationSessionUseCase,
    private val getParticipantsUseCase: GetSessionParticipantsUseCase,
    private val getDocumentStateUseCase: GetDocumentStateUseCase,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val getCommentsUseCase: GetDocumentCommentsUseCase,
    private val createCommentUseCase: CreateDocumentCommentUseCase,
    private val resolveCommentUseCase: ResolveDocumentCommentUseCase,
    private val deleteCommentUseCase: DeleteDocumentCommentUseCase,
    private val updatePresenceUseCase: UpdatePresenceUseCase,
    private val getUserPresenceUseCase: GetUserPresenceUseCase,
    private val setOfflineUseCase: SetOfflineUseCase,
    private val getCollaborationUrlUseCase: GetCollaborationUrlUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    @InjectedParam private val initialItemId: String,
) : ViewModel() {

    private val userId: String
        get() = getCurrentUserUseCase.currentUserFlow.value?.id ?: "anonymous"
    private val userName: String
        get() = getCurrentUserUseCase.currentUserFlow.value?.username ?: "Anonymous"

    var session by mutableStateOf<CollaborationSession?>(null)
        private set
    var documentState by mutableStateOf<DocumentState?>(null)
        private set
    var comments by mutableStateOf<List<DocumentComment>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isConnected by mutableStateOf(false)
        private set
    var otherCursors by mutableStateOf<Map<String, Pair<String, Pair<Int, Int>>>>(emptyMap())
        private set
    var participants by mutableStateOf<List<CollaborationParticipant>>(emptyList())
        private set
    var userPresences by mutableStateOf<List<UserPresence>>(emptyList())
        private set

    private var currentItemId: String? = null
    private var webSocket: CollaborationWebSocket? = null
    private var documentVersion = 0

    init {
        if (initialItemId.isNotBlank()) {
            joinSession(initialItemId)
        }
    }

    fun joinSession(itemId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            currentItemId = itemId
            when (val result = joinSessionUseCase(itemId)) {
                is ApiResult.Success -> {
                    session = result.data
                    loadDocumentState(itemId)
                    loadComments(itemId)
                    connectWebSocket(itemId, result.data.id)
                }
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    private fun connectWebSocket(itemId: String, sessionId: String) {
        webSocket = CollaborationWebSocket(
            baseUrl = getCollaborationUrlUseCase(),
            itemId = itemId,
            sessionId = sessionId,
            userId = userId,
            userName = userName,
        )

        // Observe connection state
        viewModelScope.launch {
            webSocket?.connectionState?.collectLatest { state ->
                isConnected = state == ConnectionState.CONNECTED
            }
        }

        // Observe incoming messages
        viewModelScope.launch {
            webSocket?.messages?.collectLatest { message ->
                when (message.type) {
                    CollaborationMessageType.OPERATION.name -> {
                        message.content?.let { content ->
                            documentState = documentState?.copy(content = content)
                            message.version?.let { documentVersion = it }
                        }
                    }
                    CollaborationMessageType.CURSOR_UPDATE.name -> {
                        message.userId?.let { uid ->
                            if (uid != userId) {
                                message.cursorPosition?.let { cursor ->
                                    otherCursors = otherCursors + (
                                        uid to Pair(
                                            message.userName ?: "User",
                                            Pair(cursor.line, cursor.column),
                                        )
                                        )
                                }
                            }
                        }
                    }
                    CollaborationMessageType.LEAVE.name -> {
                        message.userId?.let { uid ->
                            otherCursors = otherCursors - uid
                        }
                    }
                    CollaborationMessageType.JOIN.name -> {
                        // Optionally refresh participants list
                    }
                    else -> { /* Ignore other message types */ }
                }
            }
        }

        // Observe errors
        viewModelScope.launch {
            webSocket?.errors?.collectLatest { errorMsg ->
                error = errorMsg
            }
        }

        webSocket?.connect()
    }

    private fun loadDocumentState(itemId: String) {
        viewModelScope.launch {
            when (val result = getDocumentStateUseCase(itemId)) {
                is ApiResult.Success -> documentState = result.data
                is ApiResult.Error -> { /* Document may not have state yet */ }
                is ApiResult.NetworkError -> { /* Ignore */ }
            }
        }
    }

    private fun loadComments(itemId: String) {
        viewModelScope.launch {
            when (val result = getCommentsUseCase(itemId)) {
                is ApiResult.Success -> comments = result.data
                is ApiResult.Error -> { /* Ignore */ }
                is ApiResult.NetworkError -> { /* Ignore */ }
            }
        }
    }

    fun leaveSession() {
        viewModelScope.launch {
            // Disconnect WebSocket first
            webSocket?.disconnect()
            webSocket = null
            otherCursors = emptyMap()
            isConnected = false

            val currentSession = session ?: return@launch
            when (val result = leaveSessionUseCase(currentSession.id)) {
                is ApiResult.Success -> session = null
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun updateContent(newContent: String) {
        // Update local state immediately for responsive UI
        documentState = documentState?.copy(content = newContent)
        documentVersion++

        // Send update via WebSocket if connected
        if (isConnected) {
            webSocket?.sendOperation(newContent, documentVersion)
        }
    }

    fun updateCursorPosition(line: Int, column: Int) {
        if (isConnected) {
            webSocket?.sendCursorUpdate(line, column)
        }
    }

    fun saveDocument() {
        viewModelScope.launch {
            val itemId = currentItemId ?: return@launch
            isSaving = true
            error = null
            when (val result = saveDocumentUseCase(itemId)) {
                is ApiResult.Success -> { /* Document saved */ }
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isSaving = false
        }
    }

    fun addComment(
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        @Suppress("UNUSED_PARAMETER") quotedText: String? = null,
    ) {
        viewModelScope.launch {
            val itemId = currentItemId ?: return@launch
            error = null
            when (
                val result = createCommentUseCase(
                    itemId,
                    content,
                    startLine,
                    startColumn,
                    endLine,
                    endColumn,
                )
            ) {
                is ApiResult.Success -> loadComments(itemId)
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun resolveComment(commentId: String) {
        viewModelScope.launch {
            val itemId = currentItemId ?: return@launch
            error = null
            when (val result = resolveCommentUseCase(itemId, commentId)) {
                is ApiResult.Success -> loadComments(itemId)
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            val itemId = currentItemId ?: return@launch
            error = null
            when (val result = deleteCommentUseCase(itemId, commentId)) {
                is ApiResult.Success -> loadComments(itemId)
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            error = null
            when (val result = getSessionUseCase(sessionId)) {
                is ApiResult.Success -> session = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun loadParticipants() {
        viewModelScope.launch {
            val sessionId = session?.id ?: return@launch
            error = null
            when (val result = getParticipantsUseCase(sessionId)) {
                is ApiResult.Success -> participants = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun updatePresence(status: PresenceStatus) {
        viewModelScope.launch {
            error = null
            when (val result = updatePresenceUseCase(status, currentItemId)) {
                is ApiResult.Success -> { /* Presence updated */ }
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun loadUserPresences(userIds: List<String>) {
        viewModelScope.launch {
            error = null
            when (val result = getUserPresenceUseCase(userIds)) {
                is ApiResult.Success -> userPresences = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun setOffline() {
        viewModelScope.launch {
            error = null
            when (val result = setOfflineUseCase()) {
                is ApiResult.Success -> { /* Marked as offline */ }
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun clearError() {
        error = null
    }
}
