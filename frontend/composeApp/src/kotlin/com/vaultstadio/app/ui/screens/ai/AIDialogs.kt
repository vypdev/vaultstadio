/**
 * VaultStadio AI Dialogs
 *
 * Dialog components for the AI screen.
 */

package com.vaultstadio.app.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.AIProviderInfo
import com.vaultstadio.app.domain.model.AIProviderType
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SampleProvider = AIProviderInfo(
    type = AIProviderType.OLLAMA,
    baseUrl = "http://localhost:11434",
    model = "llama2",
    hasApiKey = false,
)

/**
 * Dialog for confirming provider deletion.
 */
@Composable
fun DeleteProviderDialog(
    provider: AIProviderInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Provider") },
        text = {
            Text(
                "Are you sure you want to delete the ${provider.type.name} " +
                    "provider configuration? This action cannot be undone.",
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Dialog for configuring AI providers.
 */
@Composable
fun ProviderConfigDialog(
    providersCount: Int,
    activeProviderName: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure AI Provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configure your AI provider settings to enable AI features.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Currently configured providers: $providersCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (activeProviderName != null) {
                    Text(
                        "Active: $activeProviderName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Contact your administrator to configure AI providers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

/**
 * Dialog for displaying errors.
 */
@Composable
fun AIErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

// region Previews

@Preview
@Composable
internal fun DeleteProviderDialogPreview() {
    VaultStadioPreview {
        DeleteProviderDialog(
            provider = SampleProvider,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun ProviderConfigDialogPreview() {
    VaultStadioPreview {
        ProviderConfigDialog(
            providersCount = 2,
            activeProviderName = "Ollama",
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun AIErrorDialogPreview() {
    VaultStadioPreview {
        AIErrorDialog(
            message = "Failed to connect to AI provider. Please check your configuration.",
            onDismiss = {},
        )
    }
}

// endregion
