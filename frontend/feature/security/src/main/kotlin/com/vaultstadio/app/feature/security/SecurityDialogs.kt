package com.vaultstadio.app.feature.security

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.vaultstadio.app.domain.auth.model.ActiveSession

@Composable
fun RevokeSessionDialog(
    session: ActiveSession,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Revoke Device Access") },
        text = {
            Text(
                "Are you sure you want to revoke access for " +
                    "\"${session.deviceName}\"? " +
                    "This device will be signed out immediately.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Revoke", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
