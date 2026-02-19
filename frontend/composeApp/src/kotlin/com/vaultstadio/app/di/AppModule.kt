/**
 * Koin App Module
 *
 * Core module (createCoreModule) and runtime module list. Application beans are in appModule (DSL).
 * Auth beans are in authModule from :data:auth. Use startKoin { modules(runtimeModules(url) + authModule + appModule + ...) }.
 */

package com.vaultstadio.app.di

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import com.vaultstadio.app.data.network.TokenStorage
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Core module for network and storage configuration.
 *
 * This module provides the base dependencies that require runtime configuration
 * (like apiBaseUrl). These cannot be auto-generated because they need runtime values.
 */
fun createCoreModule(apiBaseUrl: String) = module {
    // Core configuration
    single { ApiClientConfig(baseUrl = apiBaseUrl) }

    // Token storage
    single<TokenStorage> { InMemoryTokenStorage() }

    // Token provider
    single<TokenProvider> {
        TokenProvider { get<TokenStorage>().getAccessToken() }
    }

    // HTTP Client
    single<HttpClient> { HttpClientFactory.create(get(), get()) }
}

/**
 * Runtime modules: core config + app DSL module. Add authModule and platform module at each entry point.
 */
fun runtimeModules(apiBaseUrl: String) = listOf(
    createCoreModule(apiBaseUrl),
    appModule,
)

/** @deprecated Use runtimeModules(apiBaseUrl) with startKoin { modules(runtimeModules(apiBaseUrl) + authModule + ...) } */
fun allModules(apiBaseUrl: String) = runtimeModules(apiBaseUrl)

/**
 * Alias for use by iOS (and other platforms) that expect a "shared" module list.
 * Call from native code with the desired API base URL (e.g. from Swift/App config).
 */
fun sharedModule(apiBaseUrl: String) = allModules(apiBaseUrl)
