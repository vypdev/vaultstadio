package com.vaultstadio.app.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.usecase.auth.LoginUseCase
import com.vaultstadio.app.domain.usecase.auth.RegisterUseCase
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

/**
 * Validation error types for authentication.
 */
sealed class AuthError {
    data object EmailPasswordRequired : AuthError()
    data object AllFieldsRequired : AuthError()
    data object PasswordsDoNotMatch : AuthError()
    data object PasswordTooShort : AuthError()
    data class ApiError(val message: String) : AuthError()
}

/**
 * Callback interface for authentication success.
 * Used instead of lambda to avoid KSP code generation issues with Function types.
 */
fun interface AuthSuccessCallback {
    fun onSuccess()
}

/**
 * ViewModel for authentication screens (Login/Register).
 *
 * Use Cases are injected automatically by Koin.
 * Runtime callback (onSuccess) is passed via parametersOf().
 */
@KoinViewModel
class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    @InjectedParam private val onSuccessCallback: AuthSuccessCallback,
) : ViewModel() {
    // State
    var isLoading by mutableStateOf(false)
        private set
    var authError by mutableStateOf<AuthError?>(null)
        private set
    var showRegister by mutableStateOf(false)
        private set

    // Login form state
    var loginEmail by mutableStateOf("")
        private set
    var loginPassword by mutableStateOf("")
        private set

    // Register form state
    var registerEmail by mutableStateOf("")
        private set
    var registerUsername by mutableStateOf("")
        private set
    var registerPassword by mutableStateOf("")
        private set
    var registerConfirmPassword by mutableStateOf("")
        private set

    // Actions
    fun updateLoginEmail(value: String) {
        loginEmail = value
    }

    fun updateLoginPassword(value: String) {
        loginPassword = value
    }

    fun updateRegisterEmail(value: String) {
        registerEmail = value
    }

    fun updateRegisterUsername(value: String) {
        registerUsername = value
    }

    fun updateRegisterPassword(value: String) {
        registerPassword = value
    }

    fun updateRegisterConfirmPassword(value: String) {
        registerConfirmPassword = value
    }

    fun login() {
        if (loginEmail.isBlank() || loginPassword.isBlank()) {
            authError = AuthError.EmailPasswordRequired
            return
        }

        viewModelScope.launch {
            isLoading = true
            authError = null

            when (val result = loginUseCase(loginEmail, loginPassword)) {
                is ApiResult.Success -> {
                    onSuccessCallback.onSuccess()
                }
                is ApiResult.Error -> {
                    authError = AuthError.ApiError(result.message)
                }
                is ApiResult.NetworkError -> {
                    authError = AuthError.ApiError(result.message)
                }
            }

            isLoading = false
        }
    }

    fun register() {
        if (registerEmail.isBlank() || registerUsername.isBlank() || registerPassword.isBlank()) {
            authError = AuthError.AllFieldsRequired
            return
        }

        if (registerPassword != registerConfirmPassword) {
            authError = AuthError.PasswordsDoNotMatch
            return
        }

        if (registerPassword.length < 8) {
            authError = AuthError.PasswordTooShort
            return
        }

        viewModelScope.launch {
            isLoading = true
            authError = null

            when (val result = registerUseCase(registerEmail, registerUsername, registerPassword)) {
                is ApiResult.Success -> {
                    // After successful registration, log in automatically
                    when (val loginResult = loginUseCase(registerEmail, registerPassword)) {
                        is ApiResult.Success -> onSuccessCallback.onSuccess()
                        is ApiResult.Error -> authError = AuthError.ApiError(loginResult.message)
                        is ApiResult.NetworkError -> authError = AuthError.ApiError(loginResult.message)
                    }
                }
                is ApiResult.Error -> {
                    authError = AuthError.ApiError(result.message)
                }
                is ApiResult.NetworkError -> {
                    authError = AuthError.ApiError(result.message)
                }
            }

            isLoading = false
        }
    }

    fun toggleRegister() {
        showRegister = !showRegister
        authError = null
    }

    fun clearError() {
        authError = null
    }
}
