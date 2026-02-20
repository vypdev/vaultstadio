package com.vaultstadio.app.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.usecase.ChangePasswordUseCase
import com.vaultstadio.app.domain.auth.usecase.GetCurrentUserUseCase
import com.vaultstadio.app.domain.auth.usecase.GetQuotaUseCase
import com.vaultstadio.app.domain.auth.usecase.UpdateProfileUseCase
import com.vaultstadio.app.domain.result.Result
import kotlin.time.Clock
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getQuotaUseCase: GetQuotaUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val profileComponent: ProfileComponent,
) : ViewModel() {

    var user by mutableStateOf<User?>(null)
        private set
    var quota by mutableStateOf<StorageQuota?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            isLoading = true
            error = null

            when (val result = getCurrentUserUseCase()) {
                is Result.Success -> user = result.data
                is Result.Error -> {}
                is Result.NetworkError -> {}
            }

            when (val result = getQuotaUseCase()) {
                is Result.Success -> quota = result.data
                is Result.Error -> {}
                is Result.NetworkError -> {}
            }
            isLoading = false
        }
    }

    fun updateProfile(username: String) {
        viewModelScope.launch {
            isSaving = true
            error = null
            when (val result = updateProfileUseCase(username, null)) {
                is Result.Success -> {
                    user = result.data
                    successMessage = "Profile updated successfully"
                }
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isSaving = false
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            isSaving = true
            error = null
            when (val result = changePasswordUseCase(currentPassword, newPassword)) {
                is Result.Success -> successMessage = "Password changed successfully"
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isSaving = false
        }
    }

    fun clearError() {
        error = null
    }

    fun clearSuccessMessage() {
        successMessage = null
    }

    fun exportData() {
        viewModelScope.launch {
            val currentUser = user ?: return@launch
            val currentQuota = quota

            val exportData = buildString {
                appendLine("{")
                appendLine("  \"exportDate\": \"${Clock.System.now()}\",")
                appendLine("  \"user\": {")
                appendLine("    \"id\": \"${currentUser.id}\",")
                appendLine("    \"username\": \"${currentUser.username}\",")
                appendLine("    \"email\": \"${currentUser.email}\",")
                appendLine("    \"role\": \"${currentUser.role}\",")
                appendLine("    \"createdAt\": \"${currentUser.createdAt}\"")
                appendLine("  },")
                if (currentQuota != null) {
                    appendLine("  \"quota\": {")
                    appendLine("    \"usedBytes\": ${currentQuota.usedBytes},")
                    appendLine("    \"quotaBytes\": ${currentQuota.quotaBytes}")
                    appendLine("  }")
                } else {
                    appendLine("  \"quota\": null")
                }
                appendLine("}")
            }

            val fileName = "vaultstadio_export_${currentUser.username}.json"
            profileComponent.exportData(fileName, exportData.encodeToByteArray(), "application/json")
            successMessage = "Data exported successfully"
        }
    }
}
