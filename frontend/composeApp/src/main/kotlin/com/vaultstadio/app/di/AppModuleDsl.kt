/**
 * Application-level Koin module (appModule).
 *
 * Each feature/data/domain/core module should register its own Koin module (e.g. featureAuthModule,
 * authModule) declaring the beans it consumes and exposes. Entry points load runtimeModules() plus
 * all data/feature modules.
 *
 * This module only holds ViewModels and beans that still live in composeApp and are not yet
 * in a dedicated feature/data module. As we migrate a feature to :feature:xxx, move its ViewModel
 * registration to that module's di/ and remove it from here.
 *
 * Already moved: AuthViewModel -> featureAuthModule (:feature:auth).
 */

package com.vaultstadio.app.di

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.feature.files.FilesViewModel
import com.vaultstadio.app.feature.main.MainComponent
import com.vaultstadio.app.feature.upload.UploadManager
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val appModule = module {

    // --- AI and Collaboration beans are in aiModule and collaborationModule ---

    // --- Plugin use cases ---
    // --- Upload manager ---
    single { UploadManager(get()) }

    // --- ViewModels (no params) ---
    // ProfileViewModel is in featureProfileModule (:feature:profile)
    // SettingsViewModel is in featureSettingsModule (:feature:settings)
    // SecurityViewModel is in featureSecurityModule (:feature:security)
    // SharesViewModel is in featureSharesModule (:feature:shares)
    // SharedWithMeViewModel is in featureSharedWithMeModule (:feature:sharedwithme)
    // SyncViewModel is in featureSyncModule (:feature:sync)
    // FederationViewModel is in featureFederationModule (:feature:federation)
    // AIViewModel is in featureAIModule (:feature:ai)
    // AdminViewModel is in featureAdminModule (:feature:admin)
    // ActivityViewModel is in featureActivityModule (:feature:activity)
    // PluginsViewModel is in featurePluginsModule (:feature:plugins)
    // ChangePasswordViewModel is in featureChangePasswordModule (:feature:changepassword)

    // --- ViewModels (with params) ---
    // AuthViewModel is in featureAuthModule (:feature:auth)
    // VersionHistoryViewModel is in featureVersionHistoryModule (:feature:versionhistory)
    // CollaborationViewModel is in featureCollaborationModule (:feature:collaboration)
    viewModel { (mode: MainComponent.FilesMode) ->
        FilesViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), mode,
        )
    }
}
