/**
 * VaultStadio AI Provider Components
 *
 * Provider management UI components for the AI screen.
 */

package com.vaultstadio.app.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.AIModel
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

private val SampleProviderAlt = AIProviderInfo(
    type = AIProviderType.LM_STUDIO,
    baseUrl = "http://localhost:1234",
    model = "mistral",
    hasApiKey = false,
)

/**
 * Card displaying AI provider information and controls.
 */
@Composable
fun ProviderCard(
    provider: AIProviderInfo,
    isActive: Boolean,
    providerStatus: Map<String, Boolean>,
    providerModels: List<AIModel>,
    onCheckStatus: () -> Unit,
    onLoadModels: () -> Unit,
    onDelete: () -> Unit,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProviderHeader(
                provider = provider,
                isActive = isActive,
                isAdmin = isAdmin,
                onDelete = onDelete,
            )

            Spacer(Modifier.height(8.dp))

            ProviderInfo(provider = provider)

            Spacer(Modifier.height(12.dp))

            ProviderActions(
                onCheckStatus = onCheckStatus,
                onLoadModels = onLoadModels,
            )

            if (providerStatus.isNotEmpty()) {
                ProviderStatusSection(providerStatus = providerStatus)
            }

            if (providerModels.isNotEmpty()) {
                ProviderModelsSection(providerModels = providerModels)
            }
        }
    }
}

@Composable
private fun ProviderHeader(
    provider: AIProviderInfo,
    isActive: Boolean,
    isAdmin: Boolean,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    provider.type.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (isActive) {
                    Text(
                        "Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (isAdmin) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete provider",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ProviderInfo(provider: AIProviderInfo) {
    provider.baseUrl?.let { url ->
        Text(
            "URL: $url",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    provider.model?.let { model ->
        Text(
            "Model: $model",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProviderActions(
    onCheckStatus: () -> Unit,
    onLoadModels: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onCheckStatus,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Check Status", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            onClick = onLoadModels,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.List, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Models", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProviderStatusSection(providerStatus: Map<String, Boolean>) {
    Spacer(Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val isConnected = providerStatus["connected"] ?: false
        Icon(
            if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isConnected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (isConnected) "Connected" else "Connection failed",
            style = MaterialTheme.typography.bodySmall,
            color = if (isConnected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun ProviderModelsSection(providerModels: List<AIModel>) {
    Spacer(Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    Text(
        "Available Models:",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(4.dp))
    providerModels.take(5).forEach { model ->
        Text(
            "• ${model.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (providerModels.size > 5) {
        Text(
            "...and ${providerModels.size - 5} more",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// region Previews

@Preview
@Composable
internal fun ProviderCardActivePreview() {
    VaultStadioPreview {
        ProviderCard(
            provider = SampleProvider,
            isActive = true,
            providerStatus = emptyMap(),
            providerModels = emptyList(),
            onCheckStatus = {},
            onLoadModels = {},
            onDelete = {},
            isAdmin = true,
        )
    }
}

@Preview
@Composable
internal fun ProviderCardInactivePreview() {
    VaultStadioPreview {
        ProviderCard(
            provider = SampleProviderAlt,
            isActive = false,
            providerStatus = emptyMap(),
            providerModels = emptyList(),
            onCheckStatus = {},
            onLoadModels = {},
            onDelete = {},
            isAdmin = false,
        )
    }
}

@Preview
@Composable
internal fun ProviderCardWithStatusPreview() {
    VaultStadioPreview {
        ProviderCard(
            provider = SampleProvider,
            isActive = true,
            providerStatus = mapOf("connected" to true),
            providerModels = listOf(
                AIModel("llama2", "Llama 2", AIProviderType.OLLAMA),
                AIModel("mistral", "Mistral", AIProviderType.OLLAMA),
            ),
            onCheckStatus = {},
            onLoadModels = {},
            onDelete = {},
            isAdmin = true,
        )
    }
}

// endregion
