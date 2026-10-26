/**
 * Authentication screen content (Login/Register).
 */

package com.vaultstadio.app.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.form.AccessibleTextField
import com.vaultstadio.app.core.form.PasswordTextField
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.confirmPassword
import com.vaultstadio.app.core.resources.email
import com.vaultstadio.app.core.resources.login
import com.vaultstadio.app.core.resources.password
import com.vaultstadio.app.core.resources.register
import com.vaultstadio.app.core.resources.username

@Composable
fun LoginForm(
    viewModel: AuthViewModel,
    onSubmit: () -> Unit,
    focusManager: FocusManager,
) {
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }

    AccessibleTextField(
        identifier = "auth.login.email",
        accessibleName = strings.email,

        value = viewModel.loginEmail,
        onValueChange = { viewModel.updateLoginEmail(it) },
        label = { Text(strings.email) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )

    PasswordTextField(
        value = viewModel.loginPassword,
        onValueChange = { viewModel.updateLoginPassword(it) },
        identifier = "auth.login.password",
        label = strings.password,
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            PasswordVisibilityToggle(passwordVisible) { passwordVisible = !passwordVisible }
        },
        passwordVisible = passwordVisible,
        imeAction = ImeAction.Done,
        onImeAction = { onSubmit() },
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
fun RegisterForm(
    viewModel: AuthViewModel,
    onSubmit: () -> Unit,
    focusManager: FocusManager,
) {
    val strings = LocalStrings.current
    RegisterIdentityFields(viewModel, focusManager)
    RegisterPasswordField(viewModel, focusManager)
    RegisterConfirmationField(viewModel, onSubmit)

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    ) {
        Text(strings.register)
    }
}

@Composable
private fun RegisterIdentityFields(viewModel: AuthViewModel, focusManager: FocusManager) {
    val strings = LocalStrings.current
    AccessibleTextField(
        identifier = "auth.register.email",
        accessibleName = strings.email,

        value = viewModel.registerEmail,
        onValueChange = { viewModel.updateRegisterEmail(it) },
        label = { Text(strings.email) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )

    AccessibleTextField(
        identifier = "auth.register.username",
        accessibleName = strings.username,

        value = viewModel.registerUsername,
        onValueChange = { viewModel.updateRegisterUsername(it) },
        label = { Text(strings.username) },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
        ),
        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )
}

@Composable
private fun RegisterPasswordField(viewModel: AuthViewModel, focusManager: FocusManager) {
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }
    PasswordTextField(
        value = viewModel.registerPassword,
        onValueChange = { viewModel.updateRegisterPassword(it) },
        identifier = "auth.register.password",
        label = strings.password,
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            PasswordVisibilityToggle(passwordVisible) { passwordVisible = !passwordVisible }
        },
        passwordVisible = passwordVisible,
        imeAction = ImeAction.Next,
        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )
}

@Composable
private fun RegisterConfirmationField(viewModel: AuthViewModel, onSubmit: () -> Unit) {
    val strings = LocalStrings.current
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    PasswordTextField(
        value = viewModel.registerConfirmPassword,
        onValueChange = { viewModel.updateRegisterConfirmPassword(it) },
        identifier = "auth.register.confirm-password",
        label = strings.confirmPassword,
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            PasswordVisibilityToggle(confirmPasswordVisible) { confirmPasswordVisible = !confirmPasswordVisible }
        },
        passwordVisible = confirmPasswordVisible,
        imeAction = ImeAction.Done,
        onImeAction = { onSubmit() },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading,
    )
}
