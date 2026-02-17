/**
 * VaultStadio Plugins Components
 *
 * Reusable UI components for the Plugins screen.
 */

package com.vaultstadio.app.ui.screens.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.PluginInfo
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SamplePluginInfo = PluginInfo(
    id = "plugin-1",
    name = "Image Metadata Extractor",
    version = "1.2.3",
    description = "Extracts EXIF and other metadata from image files",
    author = "VaultStadio Team",
    isEnabled = true,
    state = "active",
)

private val SamplePluginInfoDisabled = PluginInfo(
    id = "plugin-2",
    name = "Video Metadata Extractor",
    version = "2.0.0",
    description = "Extracts metadata from video files including duration, codec, and resolution",
    author = "Community Contributor",
    isEnabled = false,
    state = "inactive",
)

/**
 * Card displaying a plugin.
 */
@Composable
fun PluginCard(
    plugin: PluginInfo,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plugin.name, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "v${plugin.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "By ${plugin.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onConfigure) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Configure",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Switch(checked = plugin.isEnabled, onCheckedChange = onToggle)
            }
        }
    }
}

/**
 * Card for adding new plugins.
 */
@Composable
fun AddPluginCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Add Plugin", fontWeight = FontWeight.Medium)
                Text(
                    "Install a new plugin from file or URL",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Empty state for no plugins.
 */
@Composable
fun EmptyPluginsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Build,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "No plugins installed",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// region Previews

@Preview
@Composable
internal fun PluginCardPreview() {
    VaultStadioPreview {
        PluginCard(
            plugin = SamplePluginInfo,
            onToggle = {},
            onConfigure = {},
        )
    }
}

@Preview
@Composable
internal fun PluginCardDisabledPreview() {
    VaultStadioPreview {
        PluginCard(
            plugin = SamplePluginInfoDisabled,
            onToggle = {},
            onConfigure = {},
        )
    }
}

@Preview
@Composable
internal fun AddPluginCardPreview() {
    VaultStadioPreview {
        AddPluginCard(onClick = {})
    }
}

@Preview
@Composable
internal fun EmptyPluginsStatePreview() {
    VaultStadioPreview {
        EmptyPluginsState()
    }
}

// endregion
