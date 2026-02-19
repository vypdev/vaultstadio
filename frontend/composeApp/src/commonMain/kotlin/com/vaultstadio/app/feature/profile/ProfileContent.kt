package com.vaultstadio.app.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.ProfileScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Profile feature content - injects ViewModel and delegates to ProfileScreen.
 */
@Composable
fun ProfileContent(
    component: ProfileComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileViewModel = koinViewModel()

    ProfileScreen(
        user = viewModel.user,
        quota = viewModel.quota,
        isLoading = viewModel.isLoading,
        isSaving = viewModel.isSaving,
        error = viewModel.error,
        successMessage = viewModel.successMessage,
        onRefresh = viewModel::loadProfile,
        onUpdateProfile = viewModel::updateProfile,
        onChangePassword = viewModel::changePassword,
        onClearError = viewModel::clearError,
        onClearSuccessMessage = viewModel::clearSuccessMessage,
        onNavigateBack = component::onBack,
        onNavigateToChangePassword = component::navigateToChangePassword,
        onNavigateToSecurity = component::navigateToSecurity,
        onExportData = viewModel::exportData,
        modifier = modifier,
    )
}
