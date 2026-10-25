/**
 * VaultStadio WASM/Web Koin Module
 *
 * Overrides TokenStorage with PersistentTokenStorage so the session
 * persists in localStorage across page refreshes.
 */

package com.vaultstadio.app.di

import com.vaultstadio.app.data.network.TokenStorage
import com.vaultstadio.app.platform.PersistentTokenStorage
import org.koin.dsl.module

/**
 * WASM-specific module. Overrides TokenStorage with PersistentTokenStorage
 * so auth token is stored in localStorage and survives page refresh.
 */
fun wasmJsModule() = module {
    // Loaded after createCoreModule so this overrides TokenStorage (Koin 3.x: last wins)
    single<TokenStorage> { PersistentTokenStorage() }
}
