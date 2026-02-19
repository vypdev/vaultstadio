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
import com.vaultstadio.app.data.activity.di.activityModule
import com.vaultstadio.app.data.admin.di.adminModule
import com.vaultstadio.app.data.auth.di.authModule
import com.vaultstadio.app.feature.auth.di.featureAuthModule
import com.vaultstadio.app.data.config.di.configModule
import com.vaultstadio.app.data.share.di.shareModule
import com.vaultstadio.app.data.plugin.di.pluginModule
import com.vaultstadio.app.data.storage.di.storageModule
import com.vaultstadio.app.data.federation.di.federationModule
import com.vaultstadio.app.data.ai.di.aiModule
import com.vaultstadio.app.data.collaboration.di.collaborationModule
import com.vaultstadio.app.data.metadata.di.metadataModule
import com.vaultstadio.app.data.sync.di.syncModule
import com.vaultstadio.app.data.version.di.versionModule
import com.vaultstadio.app.di.runtimeModules
import com.vaultstadio.app.di.wasmJsModule
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Default API URL; can be made configurable via host page (e.g. window.__VAULTSTADIO_API_URL__)
    val apiBaseUrl = "http://localhost:8080/api"
    startKoin {
        modules(runtimeModules(apiBaseUrl) + wasmJsModule() + activityModule + adminModule + authModule + featureAuthModule + aiModule + collaborationModule + configModule + shareModule + pluginModule + storageModule + metadataModule + syncModule + federationModule + versionModule)
    }
    ComposeViewport(content = {
            VaultStadioRoot(config = VaultStadioConfig(apiBaseUrl = apiBaseUrl))
        })
}
