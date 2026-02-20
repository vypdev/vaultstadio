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
import com.vaultstadio.app.domain.ai.usecase.GetAIProviderStatusUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProvidersUseCase
import com.vaultstadio.app.domain.ai.usecase.GetProviderModelsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentCommentsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentStateUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetSessionParticipantsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetUserPresenceUseCase
import com.vaultstadio.app.domain.collaboration.usecase.JoinCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.LeaveCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.SaveDocumentUseCase
import com.vaultstadio.app.domain.federation.usecase.AcceptFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.BlockInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.DeclineFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedActivitiesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedIdentitiesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedInstancesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetIncomingFederatedSharesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetOutgoingFederatedSharesUseCase
import com.vaultstadio.app.domain.federation.usecase.LinkIdentityUseCase
import com.vaultstadio.app.domain.federation.usecase.RemoveInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.RequestFederationUseCase
import com.vaultstadio.app.domain.federation.usecase.RevokeFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.UnlinkIdentityUseCase
import com.vaultstadio.app.feature.ai.AIViewModel
import com.vaultstadio.app.feature.collaboration.CollaborationViewModel
import com.vaultstadio.app.feature.federation.FederationViewModel
import com.vaultstadio.app.feature.files.FilesViewModel
import com.vaultstadio.app.feature.main.MainComponent
import com.vaultstadio.app.feature.upload.UploadManager
import com.vaultstadio.app.feature.versionhistory.VersionHistoryViewModel
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
    viewModel {
        FederationViewModel(
            get<GetFederatedInstancesUseCase>(),
            get<GetFederatedInstanceUseCase>(),
            get<RequestFederationUseCase>(),
            get<BlockInstanceUseCase>(),
            get<RemoveInstanceUseCase>(),
            get<GetOutgoingFederatedSharesUseCase>(),
            get<GetIncomingFederatedSharesUseCase>(),
            get<AcceptFederatedShareUseCase>(),
            get<DeclineFederatedShareUseCase>(),
            get<RevokeFederatedShareUseCase>(),
            get<GetFederatedIdentitiesUseCase>(),
            get<LinkIdentityUseCase>(),
            get<UnlinkIdentityUseCase>(),
            get<GetFederatedActivitiesUseCase>(),
        )
    }
    viewModel {
        AIViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    // AdminViewModel is in featureAdminModule (:feature:admin)
    // ActivityViewModel is in featureActivityModule (:feature:activity)
    // PluginsViewModel is in featurePluginsModule (:feature:plugins)
    // ChangePasswordViewModel is in featureChangePasswordModule (:feature:changepassword)

    // --- ViewModels (with params) ---
    // AuthViewModel is in featureAuthModule (:feature:auth)
    viewModel { (itemId: String) ->
        VersionHistoryViewModel(get(), get(), get(), get(), get(), get(), get(), itemId)
    }
    viewModel { (mode: MainComponent.FilesMode) ->
        FilesViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), mode,
        )
    }
    viewModel { (itemId: String) ->
        CollaborationViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), itemId,
        )
    }
}
