/**
 * VaultStadio Android Application
 *
 * Application class for initializing dependencies and global state.
 */

package com.vaultstadio.app

import android.app.Application
import com.vaultstadio.app.di.VaultStadioApp
import com.vaultstadio.app.di.runtimeModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.koin.plugin.module.dsl.startKoin

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
        startKoin<VaultStadioApp> {
            androidLogger(Level.INFO)
            androidContext(this@VaultStadioApplication)
            modules(
                runtimeModules(getServerUrl()) + listOf(androidModule),
            )
        }
    }

    private fun getServerUrl(): String {
        val prefs = getSharedPreferences("vaultstadio", MODE_PRIVATE)
        return prefs.getString("server_url", "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"
    }
}
