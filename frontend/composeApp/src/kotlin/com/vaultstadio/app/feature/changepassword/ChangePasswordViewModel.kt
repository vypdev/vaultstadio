package com.vaultstadio.app.feature.changepassword

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.auth.usecase.ChangePasswordUseCase
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * ViewModel for changing user password.
 */
@KoinViewModel
class ChangePasswordViewModel(
    private val changePasswordUseCase: ChangePasswordUseCase,
) : ViewModel() {

    var currentPassword by mutableStateOf("")
        private set

    var newPassword by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isSuccess by mutableStateOf(false)
        private set

    var showCurrentPassword by mutableStateOf(false)
        private set

    var showNewPassword by mutableStateOf(false)
        private set

    var showConfirmPassword by mutableStateOf(false)
        private set

    fun updateCurrentPassword(value: String) {
        currentPassword = value
        errorMessage = null
    }

    fun updateNewPassword(value: String) {
        newPassword = value
        errorMessage = null
    }

    fun updateConfirmPassword(value: String) {
        confirmPassword = value
        errorMessage = null
    }

    fun toggleCurrentPasswordVisibility() {
        showCurrentPassword = !showCurrentPassword
    }

    fun toggleNewPasswordVisibility() {
        showNewPassword = !showNewPassword
    }

    fun toggleConfirmPasswordVisibility() {
        showConfirmPassword = !showConfirmPassword
    }

    fun changePassword() {
        if (!validateInput()) return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            when (val result = changePasswordUseCase(currentPassword, newPassword)) {
                is Result.Success -> {
                    isSuccess = true
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                }
                is Result.Error -> errorMessage = result.message
                is Result.NetworkError -> errorMessage = result.message
            }

            isLoading = false
        }
    }

    fun dismissSuccess() {
        isSuccess = false
    }

    private fun validateInput(): Boolean {
        if (currentPassword.isBlank()) {
            errorMessage = "Current password is required"
            return false
        }

        if (newPassword.isBlank()) {
            errorMessage = "New password is required"
            return false
        }

        if (newPassword.length < 8) {
            errorMessage = "Password must be at least 8 characters"
            return false
        }

        if (newPassword != confirmPassword) {
            errorMessage = "Passwords do not match"
            return false
        }

        if (currentPassword == newPassword) {
            errorMessage = "New password must be different from current password"
            return false
        }

        return true
    }
}
