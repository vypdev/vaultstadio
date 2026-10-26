package com.vaultstadio.app.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.usecase.LoginUseCase
import com.vaultstadio.app.domain.auth.usecase.RegisterUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.feature.auth.AuthSuccessCallback
import com.vaultstadio.app.feature.auth.AuthViewModel
import com.vaultstadio.app.feature.auth.LoginForm
import com.vaultstadio.app.feature.auth.RegisterForm

internal fun inputAuthViewModel() = AuthViewModel(
    object : LoginUseCase {
        override suspend fun invoke(email: String, password: String): Result<LoginResult> =
            error("Editing tests must not submit authentication")
    },
    object : RegisterUseCase {
        override suspend fun invoke(email: String, username: String, password: String): Result<User> =
            error("Editing tests must not register accounts")
    },
    AuthSuccessCallback { error("Editing tests must not authenticate") },
)

@Composable
internal fun AuthFieldFixture(id: String, model: AuthViewModel) {
    Column {
        if (id.startsWith("auth.login.")) {
            LoginForm(model, { model.login() }, LocalFocusManager.current)
        } else {
            RegisterForm(model, { model.register() }, LocalFocusManager.current)
        }
    }
}
