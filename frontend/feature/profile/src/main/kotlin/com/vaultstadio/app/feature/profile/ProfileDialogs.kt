package com.vaultstadio.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.InputLabel
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.inputLabel
import com.vaultstadio.app.core.ui.inputSemantics

@Composable
fun EditProfileDialog(
    currentUsername: String,
    isSaving: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var username by remember { mutableStateOf(currentUsername) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(strings.authUsername) },
                    modifier = Modifier.fillMaxWidth().inputSemantics("profile.username", strings.authUsername),
                    singleLine = true,
                    enabled = !isSaving,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (username.isNotBlank()) onSave(username) },
                enabled = username.isNotBlank() && !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun ChangePasswordDialog(
    isSaving: Boolean,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val passwordsMatch = confirmPassword.isEmpty() || confirmPassword == newPassword
    val canSubmit = currentPassword.isNotBlank() &&
        newPassword.isNotBlank() &&
        newPassword == confirmPassword &&
        !isSaving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(strings.inputLabel(InputLabel.CURRENT_PASSWORD)) },
                    modifier = Modifier.fillMaxWidth().inputSemantics(
                        "profile.current-password",
                        strings.inputLabel(InputLabel.CURRENT_PASSWORD),
                    ),
                    singleLine = true,
                    enabled = !isSaving,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(strings.inputLabel(InputLabel.NEW_PASSWORD)) },
                    modifier = Modifier.fillMaxWidth().inputSemantics(
                        "profile.new-password",
                        strings.inputLabel(InputLabel.NEW_PASSWORD),
                    ),
                    singleLine = true,
                    enabled = !isSaving,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(strings.authConfirmPassword) },
                    modifier = Modifier.fillMaxWidth().inputSemantics(
                        "profile.confirm-password",
                        strings.authConfirmPassword,
                    ),
                    singleLine = true,
                    enabled = !isSaving,
                    isError = !passwordsMatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                )
                if (!passwordsMatch) {
                    Text(
                        "Passwords do not match",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onChangePassword(currentPassword, newPassword) },
                enabled = canSubmit,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Change Password")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
