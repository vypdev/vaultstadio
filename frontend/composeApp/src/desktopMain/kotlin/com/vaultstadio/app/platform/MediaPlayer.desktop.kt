/**
 * VaultStadio Media Player - Desktop Implementation
 *
 * Uses system applications for media playback on desktop platforms.
 */

package com.vaultstadio.app.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Video player for Desktop.
 * Opens video in system default media player.
 */
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onError: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.VideoFile,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Video Player",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Click to open in system player",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    }
                } catch (e: IOException) {
                    onError(e.message ?: "Failed to open video")
                } catch (e: URISyntaxException) {
                    onError(e.message ?: "Invalid video URL")
                }
            }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Play Video")
            }
        }
    }
}

/**
 * Audio player for Desktop.
 * Opens audio in system default media player.
 */
@Composable
actual fun AudioPlayer(
    url: String,
    modifier: Modifier,
    onError: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Audio Player",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Click to open in system player",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    }
                } catch (e: IOException) {
                    onError(e.message ?: "Failed to open audio")
                } catch (e: URISyntaxException) {
                    onError(e.message ?: "Invalid audio URL")
                }
            }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Play Audio")
            }
        }
    }
}

/**
 * PDF viewer for Desktop.
 * Opens PDF in system default PDF viewer.
 */
@Composable
actual fun PdfViewer(
    url: String,
    modifier: Modifier,
    onError: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "PDF Document",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Click to open in system viewer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    }
                } catch (e: IOException) {
                    onError(e.message ?: "Failed to open PDF")
                } catch (e: URISyntaxException) {
                    onError(e.message ?: "Invalid PDF URL")
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Open PDF")
            }
        }
    }
}
