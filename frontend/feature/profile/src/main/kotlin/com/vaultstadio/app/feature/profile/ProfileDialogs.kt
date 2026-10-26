package com.vaultstadio.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.form.AccessibleTextField
import com.vaultstadio.app.core.form.PasswordTextField
import com.vaultstadio.app.core.resources.InputLabel
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.inputLabel

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
                AccessibleTextField(
                    identifier = "profile.username",
                    accessibleName = strings.authUsername,

                    value = username,
                    onValueChange = { username = it },
                    label = { Text(strings.authUsername) },
                    modifier = Modifier.fillMaxWidth(),
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
                PasswordTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    identifier = "profile.current-password",
                    label = strings.inputLabel(InputLabel.CURRENT_PASSWORD),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,

                )
                PasswordTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    identifier = "profile.new-password",
                    label = strings.inputLabel(InputLabel.NEW_PASSWORD),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,

                )
                PasswordTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    identifier = "profile.confirm-password",
                    label = strings.authConfirmPassword,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = !passwordsMatch,

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
