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
import com.vaultstadio.app.di.VaultStadioApp
import com.vaultstadio.app.di.runtimeModules
import org.koin.plugin.module.dsl.startKoin

fun main() {
    startKoin<VaultStadioApp> {
        modules(runtimeModules("http://localhost:8080/api"))
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
