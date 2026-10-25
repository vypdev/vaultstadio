package com.vaultstadio.app.feature.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminContent(
    component: AdminComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminViewModel = koinViewModel()

    AdminScreen(
        users = viewModel.users,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onNavigateBack = component::onBack,
        onLoadUsers = viewModel::loadUsers,
        onUpdateQuota = viewModel::updateUserQuota,
        onUpdateUserRole = viewModel::updateUserRole,
        onUpdateUserStatus = viewModel::updateUserStatus,
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}
