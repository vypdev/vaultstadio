/**
 * VaultStadio Desktop Application Entry Point
 *
 * Koin must be started globally before the first composable runs, so that
 * KoinComponent (used by RootComponent, MainComponent, etc.) can resolve
 * dependencies via GlobalContext. KoinApplication in Compose only provides
 * Koin via CompositionLocal and does not set GlobalContext.
 */

package com.vaultstadio.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaultstadio.app.data.activity.di.activityModule
import com.vaultstadio.app.data.admin.di.adminModule
import com.vaultstadio.app.data.auth.di.authModule
import com.vaultstadio.app.data.config.di.configModule
import com.vaultstadio.app.data.share.di.shareModule
import com.vaultstadio.app.data.plugin.di.pluginModule
import com.vaultstadio.app.data.storage.di.storageModule
import com.vaultstadio.app.data.version.di.versionModule
import com.vaultstadio.app.di.runtimeModules
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(runtimeModules("http://localhost:8080/api") + activityModule + adminModule + authModule + configModule + shareModule + pluginModule + storageModule + versionModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "VaultStadio",
            state = rememberWindowState(
                width = 1200.dp,
                height = 800.dp,
            ),
        ) {
            VaultStadioRoot()
        }
    }
}
