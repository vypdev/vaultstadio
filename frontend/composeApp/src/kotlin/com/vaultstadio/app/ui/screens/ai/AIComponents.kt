/**
 * VaultStadio AI Components
 *
 * Reusable UI components for the AI screen.
 */

package com.vaultstadio.app.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.i18n.StringResources
import com.vaultstadio.app.i18n.Strings
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Empty state display for AI features.
 */
@Composable
fun EmptyAIState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Card displaying AI operation results.
 */
@Composable
fun ResultCard(
    title: String,
    content: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Card displaying generated tags.
 */
@Composable
fun TagsResultCard(
    tags: List<String>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    strings: StringResources = Strings.resources,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.aiGeneratedTags,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.take(10).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            tag,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

// region Previews

@Preview
@Composable
internal fun EmptyAIStatePreview() {
    VaultStadioPreview {
        EmptyAIState(
            icon = Icons.Default.SmartToy,
            title = "Start a Conversation",
            description = "Ask questions, get help with your files, or explore AI features.",
        )
    }
}

@Preview
@Composable
internal fun ResultCardPreview() {
    VaultStadioPreview {
        ResultCard(
            title = "Image Description",
            content = "A beautiful sunset over the ocean with orange and purple hues.",
            onClear = {},
        )
    }
}

@Preview
@Composable
internal fun TagsResultCardPreview() {
    VaultStadioPreview {
        TagsResultCard(
            tags = listOf("sunset", "ocean", "nature", "landscape", "peaceful"),
            onClear = {},
        )
    }
}

// endregion
