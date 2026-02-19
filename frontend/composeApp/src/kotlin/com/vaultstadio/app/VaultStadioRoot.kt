/**
 * VaultStadio Root Application
 *
 * Main entry point for the application using Decompose navigation and Koin DI.
 */

package com.vaultstadio.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.vaultstadio.app.config.LocalApiBaseUrl
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import com.vaultstadio.app.feature.upload.UploadManager
import com.vaultstadio.app.i18n.LocalStrings
import com.vaultstadio.app.i18n.Strings
import com.vaultstadio.app.navigation.DefaultRootComponent
import com.vaultstadio.app.navigation.RootComponent
import com.vaultstadio.app.navigation.RootContent
import com.vaultstadio.app.platform.DragDropEvent
import com.vaultstadio.app.platform.DragDropState
import com.vaultstadio.app.platform.getInitialPath
import com.vaultstadio.app.platform.initializeDragDrop
import com.vaultstadio.app.ui.components.layout.DragOverlay
import com.vaultstadio.app.ui.theme.ThemeSettings
import com.vaultstadio.app.ui.theme.VaultStadioTheme
import org.koin.compose.KoinContext

/**
 * Configuration for the VaultStadio application.
 */
data class VaultStadioConfig(
    val apiBaseUrl: String = "http://localhost:8080/api",
)

/**
 * Creates and remembers the root component for the application.
 */
@Composable
private fun rememberRootComponent(): RootComponent {
    return remember {
        val lifecycle = LifecycleRegistry()
        val componentContext = DefaultComponentContext(lifecycle = lifecycle)
        val initialPath = getInitialPath()

        DefaultRootComponent(
            componentContext = componentContext,
            initialPath = initialPath,
        )
    }
}

/**
 * Main VaultStadio application composable using Decompose architecture and Koin DI.
 *
 * Koin must be started by the platform (Android Application, Desktop main(), iOS KoinHelper)
 * before this composable runs. This binds the existing GlobalContext to Compose so that
 * KoinComponent and koinInject() resolve correctly.
 *
 * @param config Application configuration
 */
@Composable
fun VaultStadioRoot(
    config: VaultStadioConfig = VaultStadioConfig(),
) {
    KoinContext {
        VaultStadioContent(config)
    }
}

@Composable
private fun VaultStadioContent(config: VaultStadioConfig) {
    val rootComponent = rememberRootComponent()
    val uploadManager: UploadManager = org.koin.compose.koinInject()

    // Drag and drop state
    val isDragging by DragDropState.isDragging.collectAsState(initial = false)
    val dropEvent by DragDropState.events.collectAsState(initial = null)

    // Initialize drag and drop (no-op if not supported on platform)
    LaunchedEffect(Unit) {
        initializeDragDrop { droppedFiles ->
            DragDropState.emitEvent(DragDropEvent.Drop(droppedFiles))
        }
    }

    // Handle drop events: add dropped files to upload queue (to current folder when one is set)
    LaunchedEffect(dropEvent) {
        when (val event = dropEvent) {
            is DragDropEvent.Drop -> {
                val entries = event.files
                    .filter { it.data.isNotEmpty() }
                    .map { f ->
                        UploadQueueEntry.WithData(f.name, f.size, f.mimeType, f.data)
                    }
                if (entries.isNotEmpty()) {
                    val parentId = uploadManager.getCurrentDestinationFolderId()
                    uploadManager.addEntries(entries, parentId = parentId)
                }
                DragDropState.clearEvent()
            }
            else -> { /* Ignore other events */ }
        }
    }

    VaultStadioTheme(themeMode = ThemeSettings.themeMode) {
        CompositionLocalProvider(
            LocalStrings provides Strings.resources,
            LocalApiBaseUrl provides config.apiBaseUrl.removeSuffix("/api"),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                RootContent(
                    component = rootComponent,
                    modifier = Modifier.fillMaxSize(),
                )

                // Drag overlay (shown when dragging files over the app)
                if (isDragging) {
                    DragOverlay()
                }
            }
        }
    }
}

/**
 * Application entry point used by iOS and other platforms.
 * Delegates to VaultStadioRoot with default config.
 */
@Composable
fun VaultStadioApp() {
    VaultStadioRoot()
}

/**
 * Legacy entry point for backward compatibility.
 *
 * @deprecated Use VaultStadioRoot() instead
 */
@Deprecated(
    message = "Use VaultStadioRoot() instead",
    replaceWith = ReplaceWith("VaultStadioRoot()"),
)
@Composable
fun VaultStadioAppNew() {
    VaultStadioRoot()
}
