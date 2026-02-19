/**
 * VaultStadio Web Application Entry Point
 *
 * Koin must be started before the first composable so that KoinComponent and
 * koinInject() resolve via GlobalContext. The API base URL can be overridden
 * via query param or window config in the future.
 */

package com.vaultstadio.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.vaultstadio.app.di.VaultStadioApp
import com.vaultstadio.app.di.runtimeModules
import com.vaultstadio.app.di.wasmJsModule
import org.koin.plugin.module.dsl.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Default API URL; can be made configurable via host page (e.g. window.__VAULTSTADIO_API_URL__)
    val apiBaseUrl = "http://localhost:8080/api"
    startKoin<VaultStadioApp> {
        modules(runtimeModules(apiBaseUrl) + wasmJsModule())
    }
    ComposeViewport(content = {
            VaultStadioRoot(config = VaultStadioConfig(apiBaseUrl = apiBaseUrl))
        })
}
