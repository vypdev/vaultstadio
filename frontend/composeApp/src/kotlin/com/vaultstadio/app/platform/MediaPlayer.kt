/**
 * VaultStadio Media Player - Platform abstraction
 *
 * Provides expect/actual definitions for video and audio playback
 * across different platforms (Web, Desktop, Android, iOS).
 */

package com.vaultstadio.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Video player component.
 * Platform-specific implementations should provide actual video playback.
 *
 * @param url The URL to the video file
 * @param modifier Compose modifier
 * @param onError Callback when an error occurs
 */
@Composable
expect fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {},
)

/**
 * Audio player component.
 * Platform-specific implementations should provide actual audio playback.
 *
 * @param url The URL to the audio file
 * @param modifier Compose modifier
 * @param onError Callback when an error occurs
 */
@Composable
expect fun AudioPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {},
)

/**
 * PDF viewer component.
 * Platform-specific implementations should provide PDF rendering.
 *
 * @param url The URL to the PDF file
 * @param modifier Compose modifier
 * @param onError Callback when an error occurs
 */
@Composable
expect fun PdfViewer(
    url: String,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {},
)
