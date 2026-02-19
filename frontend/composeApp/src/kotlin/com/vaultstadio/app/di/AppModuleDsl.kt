/**
 * Application module defined with Koin DSL (no annotations).
 *
 * Replaces @ComponentScan("com.vaultstadio.app") and all @Single / @Factory / @KoinViewModel.
 * Auth beans are provided by authModule from :data:auth.
 */

package com.vaultstadio.app.di

import com.vaultstadio.app.data.api.AIApi
import com.vaultstadio.app.data.api.CollaborationApi
import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.data.repository.AIRepositoryImpl
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.data.repository.CollaborationRepositoryImpl
import com.vaultstadio.app.data.service.AIService
import com.vaultstadio.app.data.service.CollaborationService
import com.vaultstadio.app.domain.usecase.ai.AIChatUseCase
import com.vaultstadio.app.domain.usecase.ai.AIChatUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.ClassifyContentUseCase
import com.vaultstadio.app.domain.usecase.ai.ClassifyContentUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.ConfigureAIProviderUseCase
import com.vaultstadio.app.domain.usecase.ai.ConfigureAIProviderUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.DeleteAIProviderUseCase
import com.vaultstadio.app.domain.usecase.ai.DeleteAIProviderUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.DescribeImageUseCase
import com.vaultstadio.app.domain.usecase.ai.DescribeImageUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.GetAIModelsUseCase
import com.vaultstadio.app.domain.usecase.ai.GetAIModelsUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.GetAIProvidersUseCase
import com.vaultstadio.app.domain.usecase.ai.GetAIProvidersUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.GetAIProviderStatusUseCase
import com.vaultstadio.app.domain.usecase.ai.GetAIProviderStatusUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.GetProviderModelsUseCase
import com.vaultstadio.app.domain.usecase.ai.GetProviderModelsUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.SetActiveAIProviderUseCase
import com.vaultstadio.app.domain.usecase.ai.SetActiveAIProviderUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.SummarizeTextUseCase
import com.vaultstadio.app.domain.usecase.ai.SummarizeTextUseCaseImpl
import com.vaultstadio.app.domain.usecase.ai.TagImageUseCase
import com.vaultstadio.app.domain.usecase.ai.TagImageUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.CreateDocumentCommentUseCase
import com.vaultstadio.app.domain.usecase.collaboration.CreateDocumentCommentUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.DeleteDocumentCommentUseCase
import com.vaultstadio.app.domain.usecase.collaboration.DeleteDocumentCommentUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.GetCollaborationSessionUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetCollaborationSessionUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.GetDocumentCommentsUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetDocumentCommentsUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.GetDocumentStateUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetDocumentStateUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.GetSessionParticipantsUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetSessionParticipantsUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.GetUserPresenceUseCase
import com.vaultstadio.app.domain.usecase.collaboration.GetUserPresenceUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.JoinCollaborationSessionUseCase
import com.vaultstadio.app.domain.usecase.collaboration.JoinCollaborationSessionUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.LeaveCollaborationSessionUseCase
import com.vaultstadio.app.domain.usecase.collaboration.LeaveCollaborationSessionUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.ResolveDocumentCommentUseCase
import com.vaultstadio.app.domain.usecase.collaboration.ResolveDocumentCommentUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.SaveDocumentUseCase
import com.vaultstadio.app.domain.usecase.collaboration.SaveDocumentUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.SetOfflineUseCase
import com.vaultstadio.app.domain.usecase.collaboration.SetOfflineUseCaseImpl
import com.vaultstadio.app.domain.usecase.collaboration.UpdatePresenceUseCase
import com.vaultstadio.app.domain.usecase.collaboration.UpdatePresenceUseCaseImpl
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
import com.vaultstadio.app.feature.activity.ActivityViewModel
import com.vaultstadio.app.feature.admin.AdminViewModel
import com.vaultstadio.app.feature.ai.AIViewModel
import com.vaultstadio.app.feature.auth.AuthSuccessCallback
import com.vaultstadio.app.feature.auth.AuthViewModel
import com.vaultstadio.app.feature.changepassword.ChangePasswordViewModel
import com.vaultstadio.app.feature.collaboration.CollaborationViewModel
import com.vaultstadio.app.feature.federation.FederationViewModel
import com.vaultstadio.app.feature.files.FilesViewModel
import com.vaultstadio.app.feature.main.MainComponent
import com.vaultstadio.app.feature.plugins.PluginsViewModel
import com.vaultstadio.app.feature.profile.ProfileViewModel
import com.vaultstadio.app.feature.security.SecurityViewModel
import com.vaultstadio.app.feature.settings.SettingsViewModel
import com.vaultstadio.app.feature.shares.SharesViewModel
import com.vaultstadio.app.feature.sharedwithme.SharedWithMeViewModel
import com.vaultstadio.app.domain.sync.usecase.DeactivateDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.GetConflictsUseCase
import com.vaultstadio.app.domain.sync.usecase.GetDevicesUseCase
import com.vaultstadio.app.domain.sync.usecase.PullChangesUseCase
import com.vaultstadio.app.domain.sync.usecase.RegisterDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.RemoveDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.ResolveConflictUseCase
import com.vaultstadio.app.feature.sync.SyncViewModel
import com.vaultstadio.app.feature.upload.UploadManager
import com.vaultstadio.app.feature.versionhistory.VersionHistoryViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val appModule = module {

    // --- APIs (depend on HttpClient) ---
    single { AIApi(get()) }
    single { CollaborationApi(get()) }

    // --- Services (depend on APIs) ---
    single { AIService(get()) }
    single { CollaborationService(get()) }

    // --- Repositories ---
    single<AIRepository> { AIRepositoryImpl(get()) }
    single<CollaborationRepository> { CollaborationRepositoryImpl(get()) }

    // --- AI use cases ---
    factory<GetAIProviderStatusUseCase> { GetAIProviderStatusUseCaseImpl(get()) }
    factory<SummarizeTextUseCase> { SummarizeTextUseCaseImpl(get()) }
    factory<ClassifyContentUseCase> { ClassifyContentUseCaseImpl(get()) }
    factory<GetAIModelsUseCase> { GetAIModelsUseCaseImpl(get()) }
    factory<GetAIProvidersUseCase> { GetAIProvidersUseCaseImpl(get()) }
    factory<ConfigureAIProviderUseCase> { ConfigureAIProviderUseCaseImpl(get()) }
    factory<SetActiveAIProviderUseCase> { SetActiveAIProviderUseCaseImpl(get()) }
    factory<DescribeImageUseCase> { DescribeImageUseCaseImpl(get()) }
    factory<DeleteAIProviderUseCase> { DeleteAIProviderUseCaseImpl(get()) }
    factory<AIChatUseCase> { AIChatUseCaseImpl(get()) }
    factory<TagImageUseCase> { TagImageUseCaseImpl(get()) }
    factory<GetProviderModelsUseCase> { GetProviderModelsUseCaseImpl(get()) }

    // --- Collaboration use cases ---
    factory<UpdatePresenceUseCase> { UpdatePresenceUseCaseImpl(get()) }
    factory<ResolveDocumentCommentUseCase> { ResolveDocumentCommentUseCaseImpl(get()) }
    factory<GetDocumentStateUseCase> { GetDocumentStateUseCaseImpl(get()) }
    factory<LeaveCollaborationSessionUseCase> { LeaveCollaborationSessionUseCaseImpl(get()) }
    factory<GetCollaborationSessionUseCase> { GetCollaborationSessionUseCaseImpl(get()) }
    factory<CreateDocumentCommentUseCase> { CreateDocumentCommentUseCaseImpl(get()) }
    factory<JoinCollaborationSessionUseCase> { JoinCollaborationSessionUseCaseImpl(get()) }
    factory<DeleteDocumentCommentUseCase> { DeleteDocumentCommentUseCaseImpl(get()) }
    factory<GetSessionParticipantsUseCase> { GetSessionParticipantsUseCaseImpl(get()) }
    factory<GetDocumentCommentsUseCase> { GetDocumentCommentsUseCaseImpl(get()) }
    factory<SaveDocumentUseCase> { SaveDocumentUseCaseImpl(get()) }
    factory<SetOfflineUseCase> { SetOfflineUseCaseImpl(get()) }
    factory<GetUserPresenceUseCase> { GetUserPresenceUseCaseImpl(get()) }

    // --- Plugin use cases ---
    // --- Upload manager ---
    single { UploadManager(get()) }

    // --- ViewModels (no params) ---
    viewModel { ProfileViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { SecurityViewModel(get(), get(), get(), get()) }
    viewModel { SharesViewModel(get(), get(), get()) }
    viewModel { SharedWithMeViewModel(get(), get(), get(), get()) }
    viewModel {
        SyncViewModel(
            get<GetDevicesUseCase>(),
            get<GetConflictsUseCase>(),
            get<RegisterDeviceUseCase>(),
            get<DeactivateDeviceUseCase>(),
            get<RemoveDeviceUseCase>(),
            get<ResolveConflictUseCase>(),
            get<PullChangesUseCase>(),
        )
    }
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
    viewModel { AdminViewModel(get(), get(), get(), get()) }
    viewModel { ActivityViewModel(get()) }
    viewModel { PluginsViewModel(get(), get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }

    // --- ViewModels (with params) ---
    viewModel { (callback: AuthSuccessCallback) ->
        AuthViewModel(get(), get(), callback)
    }
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
