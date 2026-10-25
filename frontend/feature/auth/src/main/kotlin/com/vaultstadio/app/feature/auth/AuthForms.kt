/**
 * Authentication screen content (Login/Register).
 */

package com.vaultstadio.app.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.confirmPassword
import com.vaultstadio.app.core.resources.email
import com.vaultstadio.app.core.resources.login
import com.vaultstadio.app.core.resources.password
import com.vaultstadio.app.core.resources.register
import com.vaultstadio.app.core.resources.username
import com.vaultstadio.app.core.ui.inputSemantics

@Composable
fun LoginForm(
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
        modifier = Modifier.fillMaxWidth().inputSemantics("auth.login.email", strings.email),
        enabled = !viewModel.isLoading,
    )

    OutlinedTextField(
        value = viewModel.loginPassword,
        onValueChange = { viewModel.updateLoginPassword(it) },
        label = { Text(strings.password) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            PasswordVisibilityToggle(passwordVisible) { passwordVisible = !passwordVisible }
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
        modifier = Modifier.fillMaxWidth().inputSemantics("auth.login.password", strings.password),
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
        modifier = Modifier.fillMaxWidth().inputSemantics("auth.register.email", strings.email),
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
        modifier = Modifier.fillMaxWidth().inputSemantics("auth.register.username", strings.username),
        enabled = !viewModel.isLoading,
    )
}

@Composable
private fun RegisterPasswordField(viewModel: AuthViewModel, focusManager: FocusManager) {
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = viewModel.registerPassword,
        onValueChange = { viewModel.updateRegisterPassword(it) },
        label = { Text(strings.password) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            PasswordVisibilityToggle(passwordVisible) { passwordVisible = !passwordVisible }
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
        modifier = Modifier.fillMaxWidth().inputSemantics("auth.register.password", strings.password),
        enabled = !viewModel.isLoading,
    )
}

@Composable
private fun RegisterConfirmationField(viewModel: AuthViewModel, onSubmit: () -> Unit) {
    val strings = LocalStrings.current
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = viewModel.registerConfirmPassword,
        onValueChange = { viewModel.updateRegisterConfirmPassword(it) },
        label = { Text(strings.confirmPassword) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            PasswordVisibilityToggle(confirmPasswordVisible) { confirmPasswordVisible = !confirmPasswordVisible }
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
        modifier = Modifier.fillMaxWidth().inputSemantics("auth.register.confirm-password", strings.confirmPassword),
        enabled = !viewModel.isLoading,
    )
}
