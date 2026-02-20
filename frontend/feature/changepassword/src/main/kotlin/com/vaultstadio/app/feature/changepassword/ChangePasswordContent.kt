package com.vaultstadio.app.feature.changepassword

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChangePasswordContent(
    component: ChangePasswordComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: ChangePasswordViewModel = koinViewModel()

    ChangePasswordScreen(
        currentPassword = viewModel.currentPassword,
        newPassword = viewModel.newPassword,
        confirmPassword = viewModel.confirmPassword,
        isLoading = viewModel.isLoading,
        errorMessage = viewModel.errorMessage,
        isSuccess = viewModel.isSuccess,
        showCurrentPassword = viewModel.showCurrentPassword,
        showNewPassword = viewModel.showNewPassword,
        showConfirmPassword = viewModel.showConfirmPassword,
        onCurrentPasswordChange = viewModel::updateCurrentPassword,
        onNewPasswordChange = viewModel::updateNewPassword,
        onConfirmPasswordChange = viewModel::updateConfirmPassword,
        onToggleCurrentPasswordVisibility = viewModel::toggleCurrentPasswordVisibility,
        onToggleNewPasswordVisibility = viewModel::toggleNewPasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
        onChangePassword = viewModel::changePassword,
        onDismissSuccess = viewModel::dismissSuccess,
        onNavigateBack = component::onBack,
        modifier = modifier,
    )
}
