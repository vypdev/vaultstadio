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
import com.vaultstadio.app.feature.auth.di.featureAuthModule
import com.vaultstadio.app.feature.activity.di.featureActivityModule
import com.vaultstadio.app.feature.admin.di.featureAdminModule
import com.vaultstadio.app.feature.changepassword.di.featureChangePasswordModule
import com.vaultstadio.app.feature.profile.di.featureProfileModule
import com.vaultstadio.app.feature.security.di.featureSecurityModule
import com.vaultstadio.app.feature.settings.di.featureSettingsModule
import com.vaultstadio.app.feature.sharedwithme.di.featureSharedWithMeModule
import com.vaultstadio.app.feature.shares.di.featureSharesModule
import com.vaultstadio.app.feature.versionhistory.di.featureVersionHistoryModule
import com.vaultstadio.app.feature.collaboration.di.featureCollaborationModule
import com.vaultstadio.app.feature.federation.di.featureFederationModule
import com.vaultstadio.app.feature.ai.di.featureAIModule
import com.vaultstadio.app.feature.sync.di.featureSyncModule
import com.vaultstadio.app.feature.plugins.di.featurePluginsModule
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
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(
            runtimeModules("http://localhost:8080/api") +
                activityModule + adminModule + authModule +
                featureAuthModule + featureActivityModule + featureAdminModule + featureSettingsModule +
                featureProfileModule + featureSecurityModule + featureChangePasswordModule + featureSyncModule +
                featurePluginsModule + featureSharesModule + featureSharedWithMeModule + featureVersionHistoryModule + featureCollaborationModule + featureFederationModule + featureAIModule +
                aiModule + collaborationModule + configModule + shareModule + pluginModule +
                storageModule + metadataModule + syncModule + federationModule + versionModule,
        )
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
