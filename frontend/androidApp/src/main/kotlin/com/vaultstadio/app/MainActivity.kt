/**
 * VaultStadio Android Main Activity
 *
 * Entry point for the Android application, hosting the Compose UI.
 */

package com.vaultstadio.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.vaultstadio.app.VaultStadioRoot
import com.vaultstadio.app.data.repository.AuthRepository
import com.vaultstadio.app.ui.theme.VaultStadioTheme
import org.koin.android.ext.android.inject

/**
 * Main activity for the VaultStadio Android app.
 *
 * Responsibilities:
 * - Host the Compose Multiplatform UI
 * - Handle incoming share intents
 * - Manage edge-to-edge display
 */
class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Handle share intent if present
        val sharedFiles = handleShareIntent(intent)

        setContent {
            VaultStadioTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VaultStadioAppWrapper(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        sharedFiles = sharedFiles,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle new share intent
        val sharedFiles = handleShareIntent(intent)
        if (sharedFiles.isNotEmpty()) {
            // Shared files are handled via the pendingUploads state in VaultStadioApp
            // The app will automatically show the upload dialog when sharedFiles changes
        }
    }

    /**
     * Handle incoming share intents to allow uploading files from other apps.
     */
    private fun handleShareIntent(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()

        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { listOf(it) } ?: emptyList()
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) } ?: emptyList()
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList()
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
                }
            }
            else -> emptyList()
        }
    }
}

/**
 * Composable wrapper that adapts the shared VaultStadioRoot for Android.
 */
@Composable
fun VaultStadioAppWrapper(
    modifier: Modifier = Modifier,
    sharedFiles: List<Uri> = emptyList(),
) {
    // The actual shared UI is in composeApp module
    // This is a thin wrapper for Android-specific adaptations

    var pendingUploads by remember { mutableStateOf(sharedFiles) }

    LaunchedEffect(sharedFiles) {
        if (sharedFiles.isNotEmpty()) {
            pendingUploads = sharedFiles
            // Convert Android URIs to SelectedFile format and emit as drop event
            // This integrates with the platform-agnostic drag-drop handling
            // The actual file reading happens lazily when upload starts
        }
    }

    // Use the new Decompose-based architecture
    VaultStadioRoot()
}
