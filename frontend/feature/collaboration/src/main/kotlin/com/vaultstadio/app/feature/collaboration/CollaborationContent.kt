package com.vaultstadio.app.feature.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CollaborationContent(
    component: CollaborationComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: CollaborationViewModel = koinViewModel {
        parametersOf(component.itemId)
    }

    CollaborationScreen(
        itemId = component.itemId,
        itemName = component.itemName,
        session = viewModel.session,
        documentState = viewModel.documentState,
        comments = viewModel.comments,
        participants = viewModel.participants,
        userPresences = viewModel.userPresences,
        otherCursors = viewModel.otherCursors,
        isLoading = viewModel.isLoading,
        isSaving = viewModel.isSaving,
        isConnected = viewModel.isConnected,
        error = viewModel.error,
        onJoinSession = viewModel::joinSession,
        onLeaveSession = viewModel::leaveSession,
        onLoadParticipants = viewModel::loadParticipants,
        onLoadSession = viewModel::loadSession,
        onLoadUserPresences = viewModel::loadUserPresences,
        onSaveDocument = viewModel::saveDocument,
        onContentChange = viewModel::updateContent,
        onCursorChange = viewModel::updateCursorPosition,
        onUpdatePresence = viewModel::updatePresence,
        onAddComment = viewModel::addComment,
        onResolveComment = viewModel::resolveComment,
        onDeleteComment = viewModel::deleteComment,
        onSetOffline = viewModel::setOffline,
        onClearError = viewModel::clearError,
        onBack = component::onBack,
        modifier = modifier,
    )
}
