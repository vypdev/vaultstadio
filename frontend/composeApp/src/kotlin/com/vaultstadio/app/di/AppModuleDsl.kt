/**
 * Application module defined with Koin DSL (no annotations).
 *
 * Replaces @ComponentScan("com.vaultstadio.app") and all @Single / @Factory / @KoinViewModel.
 * Auth beans are provided by authModule from :data:auth.
 */

package com.vaultstadio.app.di

import com.vaultstadio.app.data.api.AIApi
import com.vaultstadio.app.data.api.CollaborationApi
import com.vaultstadio.app.data.api.FederationApi
import com.vaultstadio.app.data.api.MetadataApi
import com.vaultstadio.app.data.api.PluginApi
import com.vaultstadio.app.data.api.SyncApi
import com.vaultstadio.app.data.api.VersionApi
import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.data.repository.AIRepositoryImpl
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.data.repository.CollaborationRepositoryImpl
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.data.repository.FederationRepositoryImpl
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.data.repository.MetadataRepositoryImpl
import com.vaultstadio.app.data.repository.PluginRepository
import com.vaultstadio.app.data.repository.PluginRepositoryImpl
import com.vaultstadio.app.data.repository.SyncRepository
import com.vaultstadio.app.data.repository.SyncRepositoryImpl
import com.vaultstadio.app.data.repository.VersionRepository
import com.vaultstadio.app.data.repository.VersionRepositoryImpl
import com.vaultstadio.app.data.service.AIService
import com.vaultstadio.app.data.service.CollaborationService
import com.vaultstadio.app.data.service.FederationService
import com.vaultstadio.app.data.service.MetadataService
import com.vaultstadio.app.data.service.PluginService
import com.vaultstadio.app.data.service.SyncService
import com.vaultstadio.app.data.service.VersionService
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
import com.vaultstadio.app.domain.usecase.federation.AcceptFederatedShareUseCase
import com.vaultstadio.app.domain.usecase.federation.AcceptFederatedShareUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.BlockInstanceUseCase
import com.vaultstadio.app.domain.usecase.federation.BlockInstanceUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.CreateFederatedShareUseCase
import com.vaultstadio.app.domain.usecase.federation.CreateFederatedShareUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.DeclineFederatedShareUseCase
import com.vaultstadio.app.domain.usecase.federation.DeclineFederatedShareUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.GetFederatedActivitiesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetFederatedActivitiesUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.GetFederatedIdentitiesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetFederatedIdentitiesUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.GetFederatedInstanceUseCase
import com.vaultstadio.app.domain.usecase.federation.GetFederatedInstanceUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.GetFederatedInstancesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetFederatedInstancesUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.GetIncomingFederatedSharesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetIncomingFederatedSharesUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.GetOutgoingFederatedSharesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetOutgoingFederatedSharesUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.LinkIdentityUseCase
import com.vaultstadio.app.domain.usecase.federation.LinkIdentityUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.RemoveInstanceUseCase
import com.vaultstadio.app.domain.usecase.federation.RemoveInstanceUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.RequestFederationUseCase
import com.vaultstadio.app.domain.usecase.federation.RequestFederationUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.RevokeFederatedShareUseCase
import com.vaultstadio.app.domain.usecase.federation.RevokeFederatedShareUseCaseImpl
import com.vaultstadio.app.domain.usecase.federation.UnlinkIdentityUseCase
import com.vaultstadio.app.domain.usecase.federation.UnlinkIdentityUseCaseImpl
import com.vaultstadio.app.domain.usecase.metadata.AdvancedSearchUseCase
import com.vaultstadio.app.domain.usecase.metadata.AdvancedSearchUseCaseImpl
import com.vaultstadio.app.domain.usecase.metadata.GetDocumentMetadataUseCase
import com.vaultstadio.app.domain.usecase.metadata.GetDocumentMetadataUseCaseImpl
import com.vaultstadio.app.domain.usecase.metadata.GetFileMetadataUseCase
import com.vaultstadio.app.domain.usecase.metadata.GetFileMetadataUseCaseImpl
import com.vaultstadio.app.domain.usecase.metadata.GetImageMetadataUseCase
import com.vaultstadio.app.domain.usecase.metadata.GetImageMetadataUseCaseImpl
import com.vaultstadio.app.domain.usecase.metadata.GetSearchSuggestionsUseCase
import com.vaultstadio.app.domain.usecase.metadata.GetSearchSuggestionsUseCaseImpl
import com.vaultstadio.app.domain.usecase.metadata.GetVideoMetadataUseCase
import com.vaultstadio.app.domain.usecase.metadata.GetVideoMetadataUseCaseImpl
import com.vaultstadio.app.domain.usecase.metadata.SearchByMetadataUseCase
import com.vaultstadio.app.domain.usecase.metadata.SearchByMetadataUseCaseImpl
import com.vaultstadio.app.domain.usecase.plugin.DisablePluginUseCase
import com.vaultstadio.app.domain.usecase.plugin.DisablePluginUseCaseImpl
import com.vaultstadio.app.domain.usecase.plugin.EnablePluginUseCase
import com.vaultstadio.app.domain.usecase.plugin.EnablePluginUseCaseImpl
import com.vaultstadio.app.domain.usecase.plugin.GetPluginsUseCase
import com.vaultstadio.app.domain.usecase.plugin.GetPluginsUseCaseImpl
import com.vaultstadio.app.domain.usecase.sync.DeactivateDeviceUseCase
import com.vaultstadio.app.domain.usecase.sync.DeactivateDeviceUseCaseImpl
import com.vaultstadio.app.domain.usecase.sync.GetConflictsUseCase
import com.vaultstadio.app.domain.usecase.sync.GetConflictsUseCaseImpl
import com.vaultstadio.app.domain.usecase.sync.GetDevicesUseCase
import com.vaultstadio.app.domain.usecase.sync.GetDevicesUseCaseImpl
import com.vaultstadio.app.domain.usecase.sync.PullChangesUseCase
import com.vaultstadio.app.domain.usecase.sync.PullChangesUseCaseImpl
import com.vaultstadio.app.domain.usecase.sync.RegisterDeviceUseCase
import com.vaultstadio.app.domain.usecase.sync.RegisterDeviceUseCaseImpl
import com.vaultstadio.app.domain.usecase.sync.RemoveDeviceUseCase
import com.vaultstadio.app.domain.usecase.sync.RemoveDeviceUseCaseImpl
import com.vaultstadio.app.domain.usecase.sync.ResolveConflictUseCase
import com.vaultstadio.app.domain.usecase.sync.ResolveConflictUseCaseImpl
import com.vaultstadio.app.domain.usecase.version.CompareVersionsUseCase
import com.vaultstadio.app.domain.usecase.version.CompareVersionsUseCaseImpl
import com.vaultstadio.app.domain.usecase.version.CleanupVersionsUseCase
import com.vaultstadio.app.domain.usecase.version.CleanupVersionsUseCaseImpl
import com.vaultstadio.app.domain.usecase.version.DeleteVersionUseCase
import com.vaultstadio.app.domain.usecase.version.DeleteVersionUseCaseImpl
import com.vaultstadio.app.domain.usecase.version.GetVersionHistoryUseCase
import com.vaultstadio.app.domain.usecase.version.GetVersionHistoryUseCaseImpl
import com.vaultstadio.app.domain.usecase.version.GetVersionUseCase
import com.vaultstadio.app.domain.usecase.version.GetVersionUseCaseImpl
import com.vaultstadio.app.domain.usecase.version.RestoreVersionUseCase
import com.vaultstadio.app.domain.usecase.version.RestoreVersionUseCaseImpl
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
import com.vaultstadio.app.feature.sync.SyncViewModel
import com.vaultstadio.app.feature.upload.UploadManager
import com.vaultstadio.app.feature.versionhistory.VersionHistoryViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val appModule = module {

    // --- APIs (depend on HttpClient) ---
    single { VersionApi(get()) }
    single { PluginApi(get()) }
    single { AIApi(get()) }
    single { SyncApi(get()) }
    single { FederationApi(get()) }
    single { MetadataApi(get()) }
    single { CollaborationApi(get()) }

    // --- Services (depend on APIs) ---
    single { VersionService(get()) }
    single { PluginService(get()) }
    single { AIService(get()) }
    single { SyncService(get()) }
    single { FederationService(get()) }
    single { MetadataService(get()) }
    single { CollaborationService(get()) }

    // --- Repositories ---
    single<VersionRepository> { VersionRepositoryImpl(get(), get(), get()) }
    single<PluginRepository> { PluginRepositoryImpl(get()) }
    single<AIRepository> { AIRepositoryImpl(get()) }
    single<SyncRepository> { SyncRepositoryImpl(get()) }
    single<FederationRepository> { FederationRepositoryImpl(get()) }
    single<MetadataRepository> { MetadataRepositoryImpl(get()) }
    single<CollaborationRepository> { CollaborationRepositoryImpl(get()) }

    // --- Metadata use cases ---
    factory<GetDocumentMetadataUseCase> { GetDocumentMetadataUseCaseImpl(get()) }
    factory<AdvancedSearchUseCase> { AdvancedSearchUseCaseImpl(get()) }
    factory<SearchByMetadataUseCase> { SearchByMetadataUseCaseImpl(get()) }
    factory<GetSearchSuggestionsUseCase> { GetSearchSuggestionsUseCaseImpl(get()) }
    factory<GetImageMetadataUseCase> { GetImageMetadataUseCaseImpl(get()) }
    factory<GetFileMetadataUseCase> { GetFileMetadataUseCaseImpl(get()) }
    factory<GetVideoMetadataUseCase> { GetVideoMetadataUseCaseImpl(get()) }

    // --- Sync use cases ---
    factory<ResolveConflictUseCase> { ResolveConflictUseCaseImpl(get()) }
    factory<RegisterDeviceUseCase> { RegisterDeviceUseCaseImpl(get()) }
    factory<GetDevicesUseCase> { GetDevicesUseCaseImpl(get()) }
    factory<RemoveDeviceUseCase> { RemoveDeviceUseCaseImpl(get()) }
    factory<DeactivateDeviceUseCase> { DeactivateDeviceUseCaseImpl(get()) }
    factory<GetConflictsUseCase> { GetConflictsUseCaseImpl(get()) }
    factory<PullChangesUseCase> { PullChangesUseCaseImpl(get()) }

    // --- Federation use cases ---
    factory<GetFederatedActivitiesUseCase> { GetFederatedActivitiesUseCaseImpl(get()) }
    factory<BlockInstanceUseCase> { BlockInstanceUseCaseImpl(get()) }
    factory<RequestFederationUseCase> { RequestFederationUseCaseImpl(get()) }
    factory<LinkIdentityUseCase> { LinkIdentityUseCaseImpl(get()) }
    factory<GetFederatedIdentitiesUseCase> { GetFederatedIdentitiesUseCaseImpl(get()) }
    factory<RevokeFederatedShareUseCase> { RevokeFederatedShareUseCaseImpl(get()) }
    factory<AcceptFederatedShareUseCase> { AcceptFederatedShareUseCaseImpl(get()) }
    factory<GetFederatedInstancesUseCase> { GetFederatedInstancesUseCaseImpl(get()) }
    factory<UnlinkIdentityUseCase> { UnlinkIdentityUseCaseImpl(get()) }
    factory<RemoveInstanceUseCase> { RemoveInstanceUseCaseImpl(get()) }
    factory<GetFederatedInstanceUseCase> { GetFederatedInstanceUseCaseImpl(get()) }
    factory<GetOutgoingFederatedSharesUseCase> { GetOutgoingFederatedSharesUseCaseImpl(get()) }
    factory<GetIncomingFederatedSharesUseCase> { GetIncomingFederatedSharesUseCaseImpl(get()) }
    factory<DeclineFederatedShareUseCase> { DeclineFederatedShareUseCaseImpl(get()) }
    factory<CreateFederatedShareUseCase> { CreateFederatedShareUseCaseImpl(get()) }

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

    // --- Version use cases ---
    factory<GetVersionHistoryUseCase> { GetVersionHistoryUseCaseImpl(get()) }
    factory<RestoreVersionUseCase> { RestoreVersionUseCaseImpl(get()) }
    factory<GetVersionUseCase> { GetVersionUseCaseImpl(get()) }
    factory<CleanupVersionsUseCase> { CleanupVersionsUseCaseImpl(get()) }
    factory<DeleteVersionUseCase> { DeleteVersionUseCaseImpl(get()) }
    factory<CompareVersionsUseCase> { CompareVersionsUseCaseImpl(get()) }

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
    factory<GetPluginsUseCase> { GetPluginsUseCaseImpl(get()) }
    factory<EnablePluginUseCase> { EnablePluginUseCaseImpl(get()) }
    factory<DisablePluginUseCase> { DisablePluginUseCaseImpl(get()) }

    // --- Upload manager ---
    single { UploadManager(get()) }

    // --- ViewModels (no params) ---
    viewModel { ProfileViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { SecurityViewModel(get(), get(), get(), get()) }
    viewModel { SharesViewModel(get(), get(), get()) }
    viewModel { SharedWithMeViewModel(get(), get(), get(), get()) }
    viewModel { SyncViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel {
        FederationViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
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
