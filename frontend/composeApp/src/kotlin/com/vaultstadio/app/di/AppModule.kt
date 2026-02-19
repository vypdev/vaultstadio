/**
 * Koin App Module
 *
 * Main module using Koin Annotations for automatic dependency injection.
 * The @ComponentScan annotation scans the entire package tree and automatically
 * registers all classes annotated with @Single, @Factory, etc.
 */

package com.vaultstadio.app.di

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import com.vaultstadio.app.data.network.TokenStorage
import com.vaultstadio.app.di.InMemoryTokenStorage
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
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
 * Main application module with component scanning.
 *
 * Uses @ComponentScan to automatically discover and register:
 * - APIs annotated with @Single
 * - Services annotated with @Single
 * - Repositories annotated with @Single (binds to interface)
 * - Use Cases annotated with @Factory
 */
@Module
@ComponentScan("com.vaultstadio.app")
class AppModule

/**
 * Runtime-only modules (core config, auth). Annotation modules are loaded via startKoin<VaultStadioApp>().
 */
fun runtimeModules(apiBaseUrl: String) = listOf(
    createCoreModule(apiBaseUrl),
    authModule(),
)

/** @deprecated Use runtimeModules(apiBaseUrl) with startKoin<VaultStadioApp> { modules(runtimeModules(apiBaseUrl)) } */
fun allModules(apiBaseUrl: String) = runtimeModules(apiBaseUrl)

/**
 * Alias for use by iOS (and other platforms) that expect a "shared" module list.
 * Call from native code with the desired API base URL (e.g. from Swift/App config).
 */
fun sharedModule(apiBaseUrl: String) = allModules(apiBaseUrl)
