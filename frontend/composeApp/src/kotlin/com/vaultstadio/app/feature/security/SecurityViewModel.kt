package com.vaultstadio.app.feature.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.auth.model.ActiveSession
import com.vaultstadio.app.domain.auth.model.LoginEvent
import com.vaultstadio.app.domain.auth.model.SecuritySettings
import com.vaultstadio.app.domain.auth.usecase.GetActiveSessionsUseCase
import com.vaultstadio.app.domain.auth.usecase.GetLoginHistoryUseCase
import com.vaultstadio.app.domain.auth.usecase.GetSecuritySettingsUseCase
import com.vaultstadio.app.domain.auth.usecase.RevokeSessionUseCase
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * ViewModel for security settings.
 */
@KoinViewModel
class SecurityViewModel(
    private val getActiveSessionsUseCase: GetActiveSessionsUseCase,
    private val getLoginHistoryUseCase: GetLoginHistoryUseCase,
    private val getSecuritySettingsUseCase: GetSecuritySettingsUseCase,
    private val revokeSessionUseCase: RevokeSessionUseCase,
) : ViewModel() {

    var isLoading by mutableStateOf(true)
        private set

    var securitySettings by mutableStateOf<SecuritySettings?>(null)
        private set

    var sessions by mutableStateOf<List<ActiveSession>>(emptyList())
        private set

    var loginHistory by mutableStateOf<List<LoginEvent>>(emptyList())
        private set

    var showRevokeSessionDialog by mutableStateOf<ActiveSession?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadSecurityData()
    }

    fun loadSecurityData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // Load security settings
            when (val result = getSecuritySettingsUseCase()) {
                is Result.Success -> securitySettings = result.data
                is Result.Error -> errorMessage = result.message
                is Result.NetworkError -> errorMessage = result.message
            }

            // Load active sessions
            when (val result = getActiveSessionsUseCase()) {
                is Result.Success -> sessions = result.data
                is Result.Error -> { /* Ignore - not critical */ }
                is Result.NetworkError -> { /* Ignore - not critical */ }
            }

            // Load login history
            when (val result = getLoginHistoryUseCase()) {
                is Result.Success -> loginHistory = result.data
                is Result.Error -> { /* Ignore - not critical */ }
                is Result.NetworkError -> { /* Ignore - not critical */ }
            }

            isLoading = false
        }
    }

    fun toggleTwoFactor() {
        // Note: Backend 2FA endpoint not yet implemented
        // This would normally call a SetTwoFactorUseCase
        errorMessage = "Two-factor authentication setup is not yet available"
    }

    fun showRevokeDialog(session: ActiveSession) {
        showRevokeSessionDialog = session
    }

    fun dismissRevokeDialog() {
        showRevokeSessionDialog = null
    }

    fun revokeSession(session: ActiveSession) {
        viewModelScope.launch {
            when (val result = revokeSessionUseCase(session.id)) {
                is Result.Success -> {
                    sessions = sessions.filter { it.id != session.id }
                    showRevokeSessionDialog = null
                }
                is Result.Error -> {
                    errorMessage = result.message
                    showRevokeSessionDialog = null
                }
                is Result.NetworkError -> {
                    errorMessage = result.message
                    showRevokeSessionDialog = null
                }
            }
        }
    }

    fun dismissError() {
        errorMessage = null
    }
}
