/**
 * Koin application entry point for the Compiler Plugin.
 *
 * AppModule scans "com.vaultstadio.app". AuthModule declares auth beans in :data:auth.
 * Runtime module createCoreModule(apiBaseUrl) is added in startKoin { } at each platform entry.
 */
package com.vaultstadio.app.di

import com.vaultstadio.app.data.auth.di.AuthModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(modules = [AppModule::class, AuthModule::class])
class VaultStadioApp
