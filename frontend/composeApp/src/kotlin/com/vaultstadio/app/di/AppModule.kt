/**
 * Koin App Module
 *
 * runtimeModules(apiBaseUrl) = createCoreModule + appModule. Each entry point (desktop, wasm, android, ios)
 * must also load data modules (authModule, storageModule, ...) and feature modules (featureAuthModule, ...).
 * See FRONTEND_MODULARISATION_AND_STANDALONE_BUILDS.md § Koin module ownership.
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
 * Runtime modules: core config + app DSL. Entry points add data modules, feature modules, and platform module.
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
