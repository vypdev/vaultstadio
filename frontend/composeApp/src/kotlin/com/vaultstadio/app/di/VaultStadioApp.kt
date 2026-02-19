/**
 * Koin application entry point for the Compiler Plugin.
 *
 * AppModule scans "com.vaultstadio.app" (includes data.storage). Runtime modules
 * (createCoreModule, authModule) are added in startKoin { } at each platform entry.
 */
package com.vaultstadio.app.di

import org.koin.core.annotation.KoinApplication

@KoinApplication(modules = [AppModule::class])
class VaultStadioApp
