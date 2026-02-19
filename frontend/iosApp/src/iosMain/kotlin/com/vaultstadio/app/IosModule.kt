/**
 * VaultStadio iOS Koin Module
 *
 * iOS-specific dependency injection configuration.
 */

package com.vaultstadio.app

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.repository.TokenStorage
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific Koin module providing platform implementations.
 */
val iosModule = module {

    // HTTP engine for iOS (Darwin)
    single<HttpClientEngineFactory<*>> { Darwin }

    // Persistent token storage using NSUserDefaults
    single<TokenStorage> { IosTokenStorage() }

    // API configuration
    single {
        val defaults = NSUserDefaults.standardUserDefaults
        val baseUrl = defaults.stringForKey("server_url") ?: "http://localhost:8080"
        ApiClientConfig(
            baseUrl = baseUrl,
            timeout = 30_000,
        )
    }
}

/**
 * iOS implementation of TokenStorage using NSUserDefaults.
 *
 * For production, consider using Keychain for secure storage.
 */
class IosTokenStorage : TokenStorage {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val accessTokenKey = "vaultstadio_access_token"
    private val refreshTokenKey = "vaultstadio_refresh_token"

    override fun getAccessToken(): String? {
        return defaults.stringForKey(accessTokenKey)
    }

    override fun setAccessToken(token: String?) {
        if (token != null) {
            defaults.setObject(token, accessTokenKey)
        } else {
            defaults.removeObjectForKey(accessTokenKey)
        }
        defaults.synchronize()
    }

    override fun getRefreshToken(): String? {
        return defaults.stringForKey(refreshTokenKey)
    }

    override fun setRefreshToken(token: String?) {
        if (token != null) {
            defaults.setObject(token, refreshTokenKey)
        } else {
            defaults.removeObjectForKey(refreshTokenKey)
        }
        defaults.synchronize()
    }

    override fun clear() {
        defaults.removeObjectForKey(accessTokenKey)
        defaults.removeObjectForKey(refreshTokenKey)
        defaults.synchronize()
    }
}
