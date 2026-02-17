/**
 * VaultStadio Android Media Player Implementation
 *
 * Uses AndroidView with native media players.
 */

package com.vaultstadio.app.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Video player composable.
 */
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onError: (String) -> Unit,
) {
    // Android would use ExoPlayer or Media3 here
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Video Player: $url")
    }
}

/**
 * Audio player composable.
 */
@Composable
actual fun AudioPlayer(
    url: String,
    modifier: Modifier,
    onError: (String) -> Unit,
) {
    // Android would use ExoPlayer or Media3 here
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Audio Player: $url")
    }
}

/**
 * PDF viewer composable.
 */
@Composable
actual fun PdfViewer(
    url: String,
    modifier: Modifier,
    onError: (String) -> Unit,
) {
    // Android would use PdfRenderer or a PDF library here
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PDF Viewer: $url")
    }
}
