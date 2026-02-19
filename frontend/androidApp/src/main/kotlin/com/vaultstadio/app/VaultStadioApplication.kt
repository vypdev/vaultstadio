/**
 * VaultStadio Android Application
 *
 * Application class for initializing dependencies and global state.
 */

package com.vaultstadio.app

import android.app.Application
import com.vaultstadio.app.data.activity.di.activityModule
import com.vaultstadio.app.data.admin.di.adminModule
import com.vaultstadio.app.data.auth.di.authModule
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
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.koin.core.context.startKoin

/**
 * Application class for VaultStadio Android app.
 *
 * Initializes:
 * - Koin dependency injection
 * - Shared module configuration
 * - Android-specific services
 */
class VaultStadioApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initializeKoin()
    }

    private fun initializeKoin() {
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@VaultStadioApplication)
            modules(
                runtimeModules(getServerUrl()) + listOf(androidModule, activityModule, adminModule, authModule, aiModule, collaborationModule, configModule, shareModule, pluginModule, storageModule, metadataModule, syncModule, federationModule, versionModule),
            )
        }
    }

    private fun getServerUrl(): String {
        val prefs = getSharedPreferences("vaultstadio", MODE_PRIVATE)
        return prefs.getString("server_url", "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"
    }
}
