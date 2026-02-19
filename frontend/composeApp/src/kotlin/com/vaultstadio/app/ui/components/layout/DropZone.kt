/**
 * VaultStadio Drop Zone Component
 *
 * Provides a drop zone UI for drag and drop file uploads.
 */

package com.vaultstadio.app.ui.components.layout

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.i18n.LocalStrings

/**
 * Drop zone state.
 */
@Composable
fun DropZone(
    isDragging: Boolean,
    onSelectFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    val borderColor by animateColorAsState(
        targetValue = if (isDragging) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        label = "borderColor",
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isDragging) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        label = "backgroundColor",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            ),
        color = backgroundColor,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isDragging) {
                    "Drop files here"
                } else {
                    "Drag files here or"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
            )

            if (!isDragging) {
                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(onClick = onSelectFiles) {
                    Text(strings.actionSelectFiles)
                }
            }
        }
    }
}
