/**
 * VaultStadio iOS Koin Helper
 *
 * Helper functions to initialize Koin from Swift.
 */

package com.vaultstadio.app

import com.vaultstadio.app.di.VaultStadioApp
import com.vaultstadio.app.di.runtimeModules
import org.koin.plugin.module.dsl.startKoin

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
        startKoin<VaultStadioApp> {
            modules(
                runtimeModules(apiBaseUrl) + listOf(iosModule),
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
