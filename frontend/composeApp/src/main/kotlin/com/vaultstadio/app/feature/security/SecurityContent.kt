package com.vaultstadio.app.feature.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.SecurityScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Content wrapper for SecurityScreen.
 */
@Composable
fun SecurityContent(
    component: SecurityComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: SecurityViewModel = koinViewModel()

    SecurityScreen(
        isLoading = viewModel.isLoading,
        securitySettings = viewModel.securitySettings,
        sessions = viewModel.sessions,
        loginHistory = viewModel.loginHistory,
        showRevokeSessionDialog = viewModel.showRevokeSessionDialog,
        errorMessage = viewModel.errorMessage,
        onToggleTwoFactor = viewModel::toggleTwoFactor,
        onShowRevokeDialog = viewModel::showRevokeDialog,
        onDismissRevokeDialog = viewModel::dismissRevokeDialog,
        onRevokeSession = viewModel::revokeSession,
        onDismissError = viewModel::dismissError,
        onNavigateBack = component::onBack,
        modifier = modifier,
    )
}
