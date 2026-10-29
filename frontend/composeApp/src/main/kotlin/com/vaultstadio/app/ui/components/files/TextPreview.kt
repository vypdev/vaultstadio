package com.vaultstadio.app.ui.components.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import org.koin.compose.koinInject

/**
 * Text/Code preview component.
 *
 * Uses the authenticated domain download port and displays at most 100 KiB.
 * The download port currently buffers the full file; this is a display limit only.
 * For code files, basic syntax highlighting based on file extension.
 */
@Composable
internal fun TextPreview(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val download: DownloadFileUseCase = koinInject()
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id, download) {
        isLoading = true
        error = null
        textContent = null
        when (val result = loadTextPreview(download, item.id)) {
            is Result.Success -> textContent = result.data
            else -> error = "Failed to load content. Try again later."
        }
        isLoading = false
    }

    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // Header with file info
        TextPreviewHeading(item)

        Spacer(modifier = Modifier.height(8.dp))

        // Content area
        TextPreviewBody(textContent, isLoading, error)
    }
}
