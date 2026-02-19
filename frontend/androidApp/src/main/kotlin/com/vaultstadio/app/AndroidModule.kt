/**
 * VaultStadio Android Koin Module
 *
 * Android-specific dependency injection configuration.
 */

package com.vaultstadio.app

import android.content.Context
import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.TokenStorage
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module providing platform implementations.
 */
val androidModule = module {

    // HTTP engine for Android
    single<HttpClientEngineFactory<*>> { Android }

    // Persistent token storage using SharedPreferences
    single<TokenStorage> { AndroidTokenStorage(androidContext()) }

    // API configuration (can be overridden by settings)
    single {
        ApiClientConfig(
            baseUrl = getBaseUrl(androidContext()),
            timeout = 30_000,
        )
    }
}

/**
 * Get the base URL from SharedPreferences or use default.
 */
private fun getBaseUrl(context: Context): String {
    val prefs = context.getSharedPreferences("vaultstadio", Context.MODE_PRIVATE)
    return prefs.getString("server_url", "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"
}

/**
 * Android implementation of TokenStorage using SharedPreferences.
 */
class AndroidTokenStorage(private val context: Context) : TokenStorage {

    private val prefs by lazy {
        context.getSharedPreferences("vaultstadio_auth", Context.MODE_PRIVATE)
    }

    override fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    override fun setAccessToken(token: String?) {
        prefs.edit().apply {
            if (token != null) {
                putString("access_token", token)
            } else {
                remove("access_token")
            }
            apply()
        }
    }

    override fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    override fun setRefreshToken(token: String?) {
        prefs.edit().apply {
            if (token != null) {
                putString("refresh_token", token)
            } else {
                remove("refresh_token")
            }
            apply()
        }
    }

    override fun clear() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .apply()
    }
}
