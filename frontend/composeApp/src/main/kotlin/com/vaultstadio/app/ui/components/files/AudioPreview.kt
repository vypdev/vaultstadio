package com.vaultstadio.app.ui.components.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.platform.AudioPlayer
import org.koin.compose.koinInject

/**
 * Audio preview component.
 *
 * Uses platform-specific AudioPlayer implementation via expect/actual.
 */
@Composable
internal fun AudioPreview(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val urls: GetStorageUrlsUseCase = koinInject()
    val audioUrl = urls.downloadUrl(item.id)

    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // Header with file info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatPreviewFileSize(item.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Audio player
        AudioPlayer(
            url = audioUrl,
            modifier = Modifier.fillMaxSize().weight(1f),
        )
    }
}
