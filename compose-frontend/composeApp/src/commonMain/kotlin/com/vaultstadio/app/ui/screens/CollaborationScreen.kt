/**
 * VaultStadio Collaboration Screen
 *
 * Screen for real-time document collaboration.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vaultstadio.app.domain.model.CollaborationParticipant
import com.vaultstadio.app.domain.model.CollaborationSession
import com.vaultstadio.app.domain.model.DocumentComment
import com.vaultstadio.app.domain.model.DocumentState
import com.vaultstadio.app.domain.model.PresenceStatus
import com.vaultstadio.app.domain.model.UserPresence
import com.vaultstadio.app.i18n.strings
import com.vaultstadio.app.ui.screens.collaboration.AddCommentDialog
import com.vaultstadio.app.ui.screens.collaboration.CollaborationErrorDialog
import com.vaultstadio.app.ui.screens.collaboration.CollaborationLoadingState
import com.vaultstadio.app.ui.screens.collaboration.CollaborationTopBar
import com.vaultstadio.app.ui.screens.collaboration.CommentsDialog
import com.vaultstadio.app.ui.screens.collaboration.DocumentEditor
import com.vaultstadio.app.ui.screens.collaboration.FailedToJoinState
import com.vaultstadio.app.ui.screens.collaboration.ParticipantsDialog
import com.vaultstadio.app.ui.screens.collaboration.SelectFileToStartState

@Composable
fun CollaborationScreen(
    itemId: String,
    itemName: String,
    session: CollaborationSession?,
    documentState: DocumentState?,
    comments: List<DocumentComment>,
    participants: List<CollaborationParticipant>,
    userPresences: List<UserPresence>,
    otherCursors: Map<String, Pair<String, Pair<Int, Int>>>,
    isLoading: Boolean,
    isSaving: Boolean,
    isConnected: Boolean,
    error: String?,
    onJoinSession: (String) -> Unit,
    onLeaveSession: () -> Unit,
    onLoadParticipants: () -> Unit,
    onLoadSession: (String) -> Unit,
    onLoadUserPresences: (List<String>) -> Unit,
    onSaveDocument: () -> Unit,
    onContentChange: (String) -> Unit,
    onCursorChange: (Int, Int) -> Unit,
    onUpdatePresence: (PresenceStatus) -> Unit,
    onAddComment: (String, Int, Int, Int, Int, String?) -> Unit,
    onResolveComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSetOffline: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = strings()
    var showParticipants by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (itemId.isNotBlank()) {
            onJoinSession(itemId)
        }
    }

    LaunchedEffect(session?.id) {
        session?.id?.let { sessionId ->
            onLoadSession(sessionId)
        }
    }

    LaunchedEffect(participants) {
        if (participants.isNotEmpty()) {
            onLoadUserPresences(participants.map { it.id })
        }
    }

    LaunchedEffect(documentState?.content) {
        documentState?.content?.let {
            if (editedContent.isEmpty()) {
                editedContent = it
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onLeaveSession()
        }
    }

    Scaffold(
        topBar = {
            CollaborationTopBar(
                itemName = itemName,
                session = session,
                comments = comments,
                isSaving = isSaving,
                showMoreMenu = showMoreMenu,
                onBack = onBack,
                onShowParticipants = { showParticipants = true },
                onShowComments = { showComments = true },
                onSave = {
                    onContentChange(editedContent)
                    onSaveDocument()
                },
                onShowMoreMenu = { showMoreMenu = true },
                onHideMoreMenu = { showMoreMenu = false },
                onAddComment = { showAddCommentDialog = true },
                strings = strings,
            )
        },
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            when {
                itemId.isBlank() -> SelectFileToStartState(strings = strings)
                isLoading -> CollaborationLoadingState(strings = strings)
                session == null -> FailedToJoinState(
                    itemId = itemId,
                    onRetry = onJoinSession,
                    strings = strings,
                )
                else -> DocumentEditor(
                    documentState = documentState,
                    editedContent = editedContent,
                    onContentChange = { editedContent = it },
                )
            }
        }
    }

    if (showParticipants && session != null) {
        LaunchedEffect(Unit) { onLoadParticipants() }
        ParticipantsDialog(
            participants = participants.ifEmpty { session.participants },
            isConnected = isConnected,
            onDismiss = { showParticipants = false },
            strings = strings,
        )
    }

    if (showComments) {
        CommentsDialog(
            comments = comments,
            onResolve = onResolveComment,
            onDelete = onDeleteComment,
            onAddComment = { showAddCommentDialog = true },
            onDismiss = { showComments = false },
            strings = strings,
        )
    }

    if (showAddCommentDialog) {
        AddCommentDialog(
            onAdd = { text ->
                onAddComment(text, 1, 1, 1, 1, null)
                showAddCommentDialog = false
            },
            onDismiss = { showAddCommentDialog = false },
            strings = strings,
        )
    }

    error?.let { errorMessage ->
        CollaborationErrorDialog(message = errorMessage, onDismiss = onClearError)
    }

    DisposableEffect(Unit) {
        onDispose { onSetOffline() }
    }
}
