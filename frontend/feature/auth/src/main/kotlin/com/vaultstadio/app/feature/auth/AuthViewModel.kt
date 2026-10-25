/**
 * ViewModel for authentication screens (Login/Register).
 *
 * Use Cases are injected automatically by Koin.
 * Runtime callback (onSuccess) is passed via parametersOf().
 */

package com.vaultstadio.app.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.auth.usecase.LoginUseCase
import com.vaultstadio.app.domain.auth.usecase.RegisterUseCase
import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val onSuccessCallback: AuthSuccessCallback,
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
                is Result.Success -> {
                    onSuccessCallback.onSuccess()
                }
                is Result.Error -> {
                    authError = AuthError.ApiError(result.message)
                }
                is Result.NetworkError -> {
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
                is Result.Success -> {
                    when (val loginResult = loginUseCase(registerEmail, registerPassword)) {
                        is Result.Success -> onSuccessCallback.onSuccess()
                        is Result.Error -> authError = AuthError.ApiError(loginResult.message)
                        is Result.NetworkError -> authError = AuthError.ApiError(loginResult.message)
                    }
                }
                is Result.Error -> {
                    authError = AuthError.ApiError(result.message)
                }
                is Result.NetworkError -> {
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
