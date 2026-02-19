/**
 * Authentication screen content (Login/Register).
 */

package com.vaultstadio.app.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.alreadyHaveAccount
import com.vaultstadio.app.core.resources.confirmPassword
import com.vaultstadio.app.core.resources.dontHaveAccount
import com.vaultstadio.app.core.resources.email
import com.vaultstadio.app.core.resources.login
import com.vaultstadio.app.core.resources.password
import com.vaultstadio.app.core.resources.register
import com.vaultstadio.app.core.resources.username
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AuthContent(
    component: AuthComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: AuthViewModel = koinViewModel {
        parametersOf(AuthSuccessCallback { component.onAuthSuccess() })
    }
    val strings = LocalStrings.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = if (viewModel.showRegister) strings.register else strings.login,
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                val errorMessage = viewModel.authError?.let { error ->
                    when (error) {
                        is AuthError.EmailPasswordRequired -> strings.errorEmailPasswordRequired
                        is AuthError.AllFieldsRequired -> strings.errorAllFieldsRequired
                        is AuthError.PasswordsDoNotMatch -> strings.errorPasswordsDoNotMatch
                        is AuthError.PasswordTooShort -> strings.errorPasswordTooShort
                        is AuthError.ApiError -> error.message
                    }
                }
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (viewModel.showRegister) {
                    RegisterForm(
                        viewModel = viewModel,
                        onSubmit = { viewModel.register() },
                        focusManager = focusManager,
                    )
                } else {
                    LoginForm(
                        viewModel = viewModel,
                        onSubmit = { viewModel.login() },
                        focusManager = focusManager,
                    )
                }

                if (viewModel.isLoading) {
                    CircularProgressIndicator()
                }

                TextButton(
                    onClick = { viewModel.toggleRegister() },
                    enabled = !viewModel.isLoading,
                ) {
                    Text(
                        text = if (viewModel.showRegister) {
                            strings.alreadyHaveAccount
                        } else {
                            strings.dontHaveAccount
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
    viewModel: AuthViewModel,
    onSubmit: () -> Unit,
    focusManager: FocusManager,
) {
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = viewModel.loginEmail,
        onValueChange = { viewModel.updateLoginEmail(it) },
        label = { Text(strings.email) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
        ),
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )

    OutlinedTextField(
        value = viewModel.loginPassword,
        onValueChange = { viewModel.updateLoginPassword(it) },
        label = { Text(strings.password) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                )
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onSubmit() },
        ),
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    ) {
        Text(strings.login)
    }
}

@Composable
private fun RegisterForm(
    viewModel: AuthViewModel,
    onSubmit: () -> Unit,
    focusManager: FocusManager,
) {
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = viewModel.registerEmail,
        onValueChange = { viewModel.updateRegisterEmail(it) },
        label = { Text(strings.email) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
        ),
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )

    OutlinedTextField(
        value = viewModel.registerUsername,
        onValueChange = { viewModel.updateRegisterUsername(it) },
        label = { Text(strings.username) },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
        ),
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )

    OutlinedTextField(
        value = viewModel.registerPassword,
        onValueChange = { viewModel.updateRegisterPassword(it) },
        label = { Text(strings.password) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                )
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
        ),
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )

    OutlinedTextField(
        value = viewModel.registerConfirmPassword,
        onValueChange = { viewModel.updateRegisterConfirmPassword(it) },
        label = { Text(strings.confirmPassword) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                Icon(
                    if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                )
            }
        },
        visualTransformation = if (confirmPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onSubmit() },
        ),
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    ) {
        Text(strings.register)
    }
}
