package com.vaultstadio.app.feature.collaboration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.collaboration.websocket.ConnectionState
import com.vaultstadio.app.data.collaboration.websocket.CollaborationWebSocket
import com.vaultstadio.app.data.collaboration.websocket.CollaborationWsMessage
import com.vaultstadio.app.data.collaboration.websocket.CollaborationWsMessageType
import com.vaultstadio.app.domain.auth.usecase.GetCurrentUserUseCase
import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.model.UserPresence
import com.vaultstadio.app.domain.collaboration.usecase.CreateDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.DeleteDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentCommentsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentStateUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetSessionParticipantsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetUserPresenceUseCase
import com.vaultstadio.app.domain.collaboration.usecase.JoinCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.LeaveCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.ResolveDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.SaveDocumentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.SetOfflineUseCase
import com.vaultstadio.app.domain.collaboration.usecase.UpdatePresenceUseCase
import com.vaultstadio.app.domain.config.usecase.GetCollaborationUrlUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
/**
 * ViewModel for real-time collaboration on documents.
 */
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
    private val initialItemId: String,
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
                is Result.Success -> {
                    session = result.data
                    loadDocumentState(itemId)
                    loadComments(itemId)
                    connectWebSocket(itemId, result.data.id)
                }
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
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
                    CollaborationWsMessageType.OPERATION.name -> {
                        message.content?.let { content ->
                            documentState = documentState?.copy(content = content)
                            message.version?.let { documentVersion = it }
                        }
                    }
                    CollaborationWsMessageType.CURSOR_UPDATE.name -> {
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
                    CollaborationWsMessageType.LEAVE.name -> {
                        message.userId?.let { uid ->
                            otherCursors = otherCursors - uid
                        }
                    }
                    CollaborationWsMessageType.JOIN.name -> {
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
                is Result.Success -> documentState = result.data
                is Result.Error -> { /* Document may not have state yet */ }
                is Result.NetworkError -> { /* Ignore */ }
            }
        }
    }

    private fun loadComments(itemId: String) {
        viewModelScope.launch {
            when (val result = getCommentsUseCase(itemId)) {
                is Result.Success -> comments = result.data
                is Result.Error -> { /* Ignore */ }
                is Result.NetworkError -> { /* Ignore */ }
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
                is Result.Success -> session = null
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
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
                is Result.Success -> { /* Document saved */ }
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
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
                is Result.Success -> loadComments(itemId)
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun resolveComment(commentId: String) {
        viewModelScope.launch {
            val itemId = currentItemId ?: return@launch
            error = null
            when (val result = resolveCommentUseCase(itemId, commentId)) {
                is Result.Success -> loadComments(itemId)
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            val itemId = currentItemId ?: return@launch
            error = null
            when (val result = deleteCommentUseCase(itemId, commentId)) {
                is Result.Success -> loadComments(itemId)
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            error = null
            when (val result = getSessionUseCase(sessionId)) {
                is Result.Success -> session = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun loadParticipants() {
        viewModelScope.launch {
            val sessionId = session?.id ?: return@launch
            error = null
            when (val result = getParticipantsUseCase(sessionId)) {
                is Result.Success -> participants = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun updatePresence(status: PresenceStatus) {
        viewModelScope.launch {
            error = null
            when (val result = updatePresenceUseCase(status, currentItemId)) {
                is Result.Success -> { /* Presence updated */ }
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun loadUserPresences(userIds: List<String>) {
        viewModelScope.launch {
            error = null
            when (val result = getUserPresenceUseCase(userIds)) {
                is Result.Success -> userPresences = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun setOffline() {
        viewModelScope.launch {
            error = null
            when (val result = setOfflineUseCase()) {
                is Result.Success -> { /* Marked as offline */ }
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun clearError() {
        error = null
    }
}
