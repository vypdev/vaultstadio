/**
 * VaultStadio iOS Koin Helper
 *
 * Helper functions to initialize Koin from Swift.
 */

package com.vaultstadio.app

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

/** Default API base URL when not provided by Swift. */
private const val DEFAULT_API_BASE_URL = "http://localhost:8080/api"

/**
 * Helper object for initializing Koin from iOS (Swift).
 *
 * Usage from Swift:
 * ```swift
 * KoinHelperKt.doInitKoin()
 * // or with custom URL:
 * KoinHelperKt.doInitKoin(apiBaseUrl: "https://myserver.com/api")
 * ```
 */
object KoinHelper {

    /**
     * Initialize Koin with shared and iOS modules.
     *
     * Call this from AppDelegate.application(_:didFinishLaunchingWithOptions:)
     * or from SwiftUI App.init().
     *
     * @param apiBaseUrl Backend API base URL (including /api). Defaults to localhost.
     */
    fun initKoin(apiBaseUrl: String = DEFAULT_API_BASE_URL) {
        startKoin {
            modules(
                runtimeModules(apiBaseUrl) + listOf(
                    iosModule, activityModule, adminModule, authModule,
                    featureAuthModule, featureActivityModule, featureAdminModule, featureSettingsModule,
                    featureProfileModule, featureSecurityModule, featureChangePasswordModule,
                    featureSyncModule, featurePluginsModule, featureSharesModule, featureSharedWithMeModule,
                    featureVersionHistoryModule,
                    featureCollaborationModule,
                    featureFederationModule,
                    featureAIModule,
                    aiModule, collaborationModule, configModule, shareModule, pluginModule,
                    storageModule, metadataModule, syncModule, federationModule, versionModule,
                ),
            )
        }
    }

    /**
     * Stop Koin when the app terminates.
     *
     * Usually not needed on iOS, but available for testing.
     */
    fun stopKoin() {
        org.koin.core.context.stopKoin()
    }
}

/**
 * Top-level function to initialize Koin from Swift.
 *
 * @param apiBaseUrl Optional backend API base URL. Omit to use default.
 */
fun doInitKoin(apiBaseUrl: String? = null) {
    KoinHelper.initKoin(apiBaseUrl = apiBaseUrl ?: DEFAULT_API_BASE_URL)
}
