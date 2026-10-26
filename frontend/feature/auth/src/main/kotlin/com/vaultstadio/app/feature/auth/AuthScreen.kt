package com.vaultstadio.app.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.StringResources
import com.vaultstadio.app.core.resources.alreadyHaveAccount
import com.vaultstadio.app.core.resources.dontHaveAccount
import com.vaultstadio.app.core.resources.login
import com.vaultstadio.app.core.resources.register

@Composable
fun AuthScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(400.dp).padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AuthHeading(viewModel)
                AuthBody(viewModel)
            }
        }
    }
}

@Composable
private fun AuthHeading(viewModel: AuthViewModel) {
    val strings = LocalStrings.current
    Text(
        text = if (viewModel.showRegister) strings.register else strings.login,
        style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    val errorMessage = viewModel.authError?.localizedMessage(strings)
    AnimatedVisibility(visible = errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text = errorMessage.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AuthBody(viewModel: AuthViewModel) {
    val strings = LocalStrings.current
    val focusManager = LocalFocusManager.current
    if (viewModel.showRegister) {
        RegisterForm(viewModel, { viewModel.register() }, focusManager)
    } else {
        LoginForm(viewModel, { viewModel.login() }, focusManager)
    }
    if (viewModel.isLoading) CircularProgressIndicator()
    TextButton(onClick = { viewModel.toggleRegister() }, enabled = !viewModel.isLoading) {
        Text(if (viewModel.showRegister) strings.alreadyHaveAccount else strings.dontHaveAccount)
    }
}

private fun AuthError.localizedMessage(strings: StringResources): String = when (this) {
    is AuthError.EmailPasswordRequired -> strings.errorEmailPasswordRequired
    is AuthError.AllFieldsRequired -> strings.errorAllFieldsRequired
    is AuthError.PasswordsDoNotMatch -> strings.errorPasswordsDoNotMatch
    is AuthError.PasswordTooShort -> strings.errorPasswordTooShort
    is AuthError.ApiError -> message
}
