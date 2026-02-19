/**
 * VaultStadio Koin Dependency Injection Configuration
 */

package com.vaultstadio.api.config

import com.vaultstadio.api.application.usecase.auth.LoginUseCase
import com.vaultstadio.api.application.usecase.auth.LoginUseCaseImpl
import com.vaultstadio.api.application.usecase.auth.LogoutUseCase
import com.vaultstadio.api.application.usecase.auth.LogoutUseCaseImpl
import com.vaultstadio.api.application.usecase.auth.RefreshSessionUseCase
import com.vaultstadio.api.application.usecase.auth.RefreshSessionUseCaseImpl
import com.vaultstadio.api.application.usecase.auth.RegisterUseCase
import com.vaultstadio.api.application.usecase.auth.RegisterUseCaseImpl
import com.vaultstadio.api.application.usecase.share.AccessShareUseCase
import com.vaultstadio.api.application.usecase.share.AccessShareUseCaseImpl
import com.vaultstadio.api.application.usecase.share.CreateShareUseCase
import com.vaultstadio.api.application.usecase.share.CreateShareUseCaseImpl
import com.vaultstadio.api.application.usecase.share.DeleteShareUseCase
import com.vaultstadio.api.application.usecase.share.DeleteShareUseCaseImpl
import com.vaultstadio.api.application.usecase.share.GetShareUseCase
import com.vaultstadio.api.application.usecase.share.GetShareUseCaseImpl
import com.vaultstadio.api.application.usecase.share.GetSharesByItemUseCase
import com.vaultstadio.api.application.usecase.share.GetSharesByItemUseCaseImpl
import com.vaultstadio.api.application.usecase.share.GetSharesByUserUseCase
import com.vaultstadio.api.application.usecase.share.GetSharesByUserUseCaseImpl
import com.vaultstadio.api.application.usecase.activity.GetRecentActivityByItemUseCase
import com.vaultstadio.api.application.usecase.activity.GetRecentActivityByItemUseCaseImpl
import com.vaultstadio.api.application.usecase.activity.GetRecentActivityByUserUseCase
import com.vaultstadio.api.application.usecase.activity.GetRecentActivityByUserUseCaseImpl
import com.vaultstadio.api.application.usecase.admin.DeleteUserUseCase
import com.vaultstadio.api.application.usecase.admin.DeleteUserUseCaseImpl
import com.vaultstadio.api.application.usecase.admin.GetAdminStatisticsUseCase
import com.vaultstadio.api.application.usecase.admin.GetAdminStatisticsUseCaseImpl
import com.vaultstadio.api.application.usecase.admin.ListUsersUseCase
import com.vaultstadio.api.application.usecase.admin.ListUsersUseCaseImpl
import com.vaultstadio.api.application.usecase.admin.UpdateQuotaUseCase
import com.vaultstadio.api.application.usecase.admin.UpdateQuotaUseCaseImpl
import com.vaultstadio.api.application.usecase.share.GetSharesSharedWithUserUseCase
import com.vaultstadio.api.application.usecase.share.GetSharesSharedWithUserUseCaseImpl
import com.vaultstadio.api.application.usecase.user.ChangePasswordUseCase
import com.vaultstadio.api.application.usecase.user.ChangePasswordUseCaseImpl
import com.vaultstadio.api.application.usecase.user.GetQuotaUseCase
import com.vaultstadio.api.application.usecase.user.GetQuotaUseCaseImpl
import com.vaultstadio.api.application.usecase.user.GetUserInfoUseCase
import com.vaultstadio.api.application.usecase.user.GetUserInfoUseCaseImpl
import com.vaultstadio.api.application.usecase.user.LogoutAllUseCase
import com.vaultstadio.api.application.usecase.user.LogoutAllUseCaseImpl
import com.vaultstadio.api.application.usecase.user.UpdateUserUseCase
import com.vaultstadio.api.application.usecase.user.UpdateUserUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.CopyItemUseCase
import com.vaultstadio.api.application.usecase.storage.CopyItemUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.CreateFolderUseCase
import com.vaultstadio.api.application.usecase.storage.CreateFolderUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.DeleteItemUseCase
import com.vaultstadio.api.application.usecase.storage.DeleteItemUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.DownloadFileUseCase
import com.vaultstadio.api.application.usecase.storage.DownloadFileUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.GetBreadcrumbsUseCase
import com.vaultstadio.api.application.usecase.storage.GetBreadcrumbsUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.GetItemUseCase
import com.vaultstadio.api.application.usecase.storage.GetItemUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.GetRecentItemsUseCase
import com.vaultstadio.api.application.usecase.storage.GetRecentItemsUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.GetStarredItemsUseCase
import com.vaultstadio.api.application.usecase.storage.GetStarredItemsUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.GetTrashItemsUseCase
import com.vaultstadio.api.application.usecase.storage.GetTrashItemsUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.ListFolderUseCase
import com.vaultstadio.api.application.usecase.storage.ListFolderUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.MoveItemUseCase
import com.vaultstadio.api.application.usecase.storage.MoveItemUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.RenameItemUseCase
import com.vaultstadio.api.application.usecase.storage.RenameItemUseCaseImpl
import com.vaultstadio.api.application.usecase.metadata.GetItemMetadataUseCase
import com.vaultstadio.api.application.usecase.metadata.GetItemMetadataUseCaseImpl
import com.vaultstadio.api.application.usecase.metadata.GetMetadataByItemIdAndPluginUseCase
import com.vaultstadio.api.application.usecase.metadata.GetMetadataByItemIdAndPluginUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.GetOrCreateFolderUseCase
import com.vaultstadio.api.application.usecase.storage.GetOrCreateFolderUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.RestoreItemUseCase
import com.vaultstadio.api.application.usecase.storage.RestoreItemUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.SearchUseCase
import com.vaultstadio.api.application.usecase.storage.SearchUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.SetStarUseCase
import com.vaultstadio.api.application.usecase.storage.SetStarUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.ToggleStarUseCase
import com.vaultstadio.api.application.usecase.storage.ToggleStarUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.TrashItemUseCase
import com.vaultstadio.api.application.usecase.storage.TrashItemUseCaseImpl
import com.vaultstadio.api.application.usecase.storage.UploadFileUseCase
import com.vaultstadio.api.application.usecase.storage.UploadFileUseCaseImpl
import com.vaultstadio.api.application.usecase.chunkedupload.CancelChunkedUploadUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.CancelChunkedUploadUseCaseImpl
import com.vaultstadio.api.application.usecase.chunkedupload.CompleteChunkedUploadUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.CompleteChunkedUploadUseCaseImpl
import com.vaultstadio.api.application.usecase.chunkedupload.GetChunkedUploadStatusUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.GetChunkedUploadStatusUseCaseImpl
import com.vaultstadio.api.application.usecase.chunkedupload.InitChunkedUploadUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.InitChunkedUploadUseCaseImpl
import com.vaultstadio.api.application.usecase.chunkedupload.UploadChunkUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.UploadChunkUseCaseImpl
import com.vaultstadio.api.application.usecase.health.GetDetailedHealthUseCase
import com.vaultstadio.api.application.usecase.health.GetDetailedHealthUseCaseImpl
import com.vaultstadio.api.application.usecase.health.GetReadinessUseCase
import com.vaultstadio.api.application.usecase.health.GetReadinessUseCaseImpl
import com.vaultstadio.api.application.usecase.version.ApplyRetentionPolicyUseCase
import com.vaultstadio.api.application.usecase.version.ApplyRetentionPolicyUseCaseImpl
import com.vaultstadio.api.application.usecase.version.CompareVersionsUseCase
import com.vaultstadio.api.application.usecase.version.CompareVersionsUseCaseImpl
import com.vaultstadio.api.application.usecase.version.DeleteVersionUseCase
import com.vaultstadio.api.application.usecase.version.DeleteVersionUseCaseImpl
import com.vaultstadio.api.application.usecase.version.GetVersionHistoryUseCase
import com.vaultstadio.api.application.usecase.version.GetVersionHistoryUseCaseImpl
import com.vaultstadio.api.application.usecase.version.GetVersionUseCase
import com.vaultstadio.api.application.usecase.version.GetVersionUseCaseImpl
import com.vaultstadio.api.application.usecase.version.RestoreVersionUseCase
import com.vaultstadio.api.application.usecase.version.RestoreVersionUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.DeactivateDeviceUseCase
import com.vaultstadio.api.application.usecase.sync.DeactivateDeviceUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.GenerateFileSignatureUseCase
import com.vaultstadio.api.application.usecase.sync.GenerateFileSignatureUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.GetPendingConflictsUseCase
import com.vaultstadio.api.application.usecase.sync.GetPendingConflictsUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.ListDevicesUseCase
import com.vaultstadio.api.application.usecase.sync.ListDevicesUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.RecordChangeUseCase
import com.vaultstadio.api.application.usecase.sync.RecordChangeUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.RegisterDeviceUseCase
import com.vaultstadio.api.application.usecase.sync.RegisterDeviceUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.RemoveDeviceUseCase
import com.vaultstadio.api.application.usecase.sync.RemoveDeviceUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.ResolveConflictUseCase
import com.vaultstadio.api.application.usecase.sync.ResolveConflictUseCaseImpl
import com.vaultstadio.api.application.usecase.sync.SyncPullUseCase
import com.vaultstadio.api.application.usecase.sync.SyncPullUseCaseImpl
import com.vaultstadio.api.application.usecase.plugin.DisablePluginUseCase
import com.vaultstadio.api.application.usecase.plugin.DisablePluginUseCaseImpl
import com.vaultstadio.api.application.usecase.plugin.EnablePluginUseCase
import com.vaultstadio.api.application.usecase.plugin.EnablePluginUseCaseImpl
import com.vaultstadio.api.application.usecase.plugin.GetPluginEndpointsUseCase
import com.vaultstadio.api.application.usecase.plugin.GetPluginEndpointsUseCaseImpl
import com.vaultstadio.api.application.usecase.plugin.GetPluginStateUseCase
import com.vaultstadio.api.application.usecase.plugin.GetPluginStateUseCaseImpl
import com.vaultstadio.api.application.usecase.plugin.GetPluginUseCase
import com.vaultstadio.api.application.usecase.plugin.GetPluginUseCaseImpl
import com.vaultstadio.api.application.usecase.plugin.HandlePluginEndpointUseCase
import com.vaultstadio.api.application.usecase.plugin.HandlePluginEndpointUseCaseImpl
import com.vaultstadio.api.application.usecase.plugin.ListPluginsUseCase
import com.vaultstadio.api.application.usecase.plugin.ListPluginsUseCaseImpl
import com.vaultstadio.api.application.usecase.ai.AIServiceUseCase
import com.vaultstadio.api.application.usecase.ai.AIServiceUseCaseImpl
import com.vaultstadio.api.plugins.PluginManager
import com.vaultstadio.api.plugins.PluginManagerImpl
import com.vaultstadio.api.routes.storage.S3Operations
import com.vaultstadio.api.routes.storage.WebDAVOperations
import com.vaultstadio.api.service.InMemoryThumbnailCache
import com.vaultstadio.api.service.InMemoryUploadSessionManager
import com.vaultstadio.api.service.ThumbnailCache
import com.vaultstadio.api.service.UploadSessionManager
import com.vaultstadio.core.ai.AIService
import com.vaultstadio.core.ai.AIServiceImpl
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.model.FederationCapability
import com.vaultstadio.core.domain.repository.ActivityRepository
import com.vaultstadio.core.domain.repository.ApiKeyRepository
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.domain.repository.SessionRepository
import com.vaultstadio.core.domain.repository.ShareRepository
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.domain.repository.SyncRepository
import com.vaultstadio.core.domain.repository.UserRepository
import com.vaultstadio.core.domain.service.ActivityLogger
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.core.domain.service.FederationCryptoService
import com.vaultstadio.core.domain.service.FederationService
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.domain.service.InMemoryLockManager
import com.vaultstadio.core.domain.service.InMemoryMultipartUploadManager
import com.vaultstadio.core.domain.service.InstanceConfig
import com.vaultstadio.core.domain.service.LockManager
import com.vaultstadio.core.domain.service.MultipartUploadManagerInterface
import com.vaultstadio.core.domain.service.PasswordHasher
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.domain.service.TransactionManager
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.infrastructure.persistence.ExposedActivityRepository
import com.vaultstadio.infrastructure.persistence.ExposedApiKeyRepository
import com.vaultstadio.infrastructure.persistence.ExposedCollaborationRepository
import com.vaultstadio.infrastructure.persistence.ExposedFederationRepository
import com.vaultstadio.infrastructure.persistence.ExposedFileVersionRepository
import com.vaultstadio.infrastructure.persistence.ExposedMetadataRepository
import com.vaultstadio.infrastructure.persistence.ExposedSessionRepository
import com.vaultstadio.infrastructure.persistence.ExposedShareRepository
import com.vaultstadio.infrastructure.persistence.ExposedStorageItemRepository
import com.vaultstadio.infrastructure.persistence.ExposedSyncRepository
import com.vaultstadio.infrastructure.persistence.ExposedTransactionManager
import com.vaultstadio.infrastructure.persistence.ExposedUserRepository
import com.vaultstadio.infrastructure.security.BCryptPasswordHasher
import com.vaultstadio.infrastructure.storage.LocalStorageBackend
import com.vaultstadio.infrastructure.storage.S3StorageBackend
import com.vaultstadio.infrastructure.storage.S3StorageConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Configures Koin dependency injection with application configuration.
 */
fun Application.configureKoin(appConfig: AppConfig? = null) {
    val config = appConfig ?: AppConfig.fromEnvironment()

    install(Koin) {
        printLogger(Level.INFO)
        modules(
            configModule(config),
            coreModule(config),
            repositoryModule,
            serviceModule,
            storageUseCaseModule,
            metadataUseCaseModule,
            chunkedUploadUseCaseModule,
            healthUseCaseModule,
            versionUseCaseModule,
            syncUseCaseModule,
            pluginUseCaseModule,
            aiUseCaseModule,
            authUseCaseModule,
            shareUseCaseModule,
            activityUseCaseModule,
            userUseCaseModule,
            adminUseCaseModule,
            pluginModule,
        )
    }

    // Eagerly initialize components
    val koin = org.koin.core.context.GlobalContext.get()
    koin.get<DatabaseInitializer>()
    koin.get<ActivityLogger>() // Start activity logging
}

/**
 * Configuration module - provides AppConfig as singleton.
 */
fun configModule(config: AppConfig) = module {
    single { config }
    single { config.server }
    single { config.database }
    single { config.storage }
    single { config.security }
    single { config.plugins }
}

/**
 * Core infrastructure module.
 */
fun coreModule(config: AppConfig) = module {
    // Event Bus
    single { EventBus() }

    // Transaction Manager
    single<TransactionManager> { ExposedTransactionManager() }

    // Password Hasher
    single<PasswordHasher> {
        BCryptPasswordHasher(cost = config.security.bcryptRounds)
    }

    // Storage Backend
    single<StorageBackend> {
        when (config.storage.type) {
            StorageType.LOCAL -> {
                LocalStorageBackend(Paths.get(config.storage.localPath))
            }
            StorageType.S3, StorageType.MINIO -> {
                val s3Config = config.storage.s3
                    ?: throw IllegalStateException("S3 configuration required for ${config.storage.type} storage")
                val storageConfig = S3StorageConfig(
                    bucket = s3Config.bucket,
                    region = s3Config.region,
                    endpoint = s3Config.endpoint.takeIf { it.isNotBlank() },
                    accessKeyId = s3Config.accessKey,
                    secretAccessKey = s3Config.secretKey,
                    usePathStyle = s3Config.usePathStyle,
                    prefix = "",
                )
                val client = runBlocking { S3StorageBackend.createClient(storageConfig) }
                S3StorageBackend(storageConfig, client)
            }
        }
    }

    // Database Initializer
    single {
        val dbConfig = DatabaseConfig(
            url = config.database.url,
            user = config.database.user,
            password = config.database.password,
            driver = config.database.driver,
            maxPoolSize = config.database.maxPoolSize,
            minIdle = config.database.minIdle,
            runMigrations = config.database.runMigrations,
        )
        DatabaseInitializer(dbConfig).also { it.initialize() }
    }
}

/**
 * Repository module.
 */
val repositoryModule = module {
    // Core repositories
    single<StorageItemRepository> { ExposedStorageItemRepository() }
    single<UserRepository> { ExposedUserRepository() }
    single<MetadataRepository> { ExposedMetadataRepository() }
    single<ShareRepository> { ExposedShareRepository() }
    single<ActivityRepository> { ExposedActivityRepository() }
    single<SessionRepository> { ExposedSessionRepository() }
    single<ApiKeyRepository> { ExposedApiKeyRepository() }

    // Phase 6: Advanced Features repositories
    single<FileVersionRepository> { ExposedFileVersionRepository() }
    single<SyncRepository> { ExposedSyncRepository() }
    single<FederationRepository> { ExposedFederationRepository() }
    single<CollaborationRepository> { ExposedCollaborationRepository() }
}

/**
 * Service module.
 */
val serviceModule = module {
    // Core services
    single {
        StorageService(
            storageItemRepository = get(),
            storageBackend = get(),
            eventBus = get(),
        )
    }

    single {
        UserService(
            userRepository = get(),
            sessionRepository = get(),
            passwordHasher = get(),
            eventBus = get(),
        )
    }

    single {
        ShareService(
            shareRepository = get(),
            storageItemRepository = get(),
            passwordHasher = get(),
            eventBus = get(),
        )
    }

    // AI Service
    single<AIService> { AIServiceImpl() }

    // Upload Session Manager
    single<UploadSessionManager> { InMemoryUploadSessionManager() }

    // Thumbnail Cache
    single<ThumbnailCache> { InMemoryThumbnailCache() }

    // Lock Manager (for WebDAV and distributed locking)
    single<LockManager> { InMemoryLockManager() }

    // Multipart Upload Manager (for S3-compatible API)
    single<MultipartUploadManagerInterface> { InMemoryMultipartUploadManager() }

    // WebDAV / S3 route facades (resolved in routes via Koin; no service injection in Routing)
    single { WebDAVOperations(get(), get()) }
    single { S3Operations(get(), get()) }

    // Phase 6: Advanced Features services

    // File Versioning Service
    single {
        FileVersionService(
            versionRepository = get(),
            itemRepository = get(),
            storageBackend = get(),
        )
    }

    // Sync Service
    single {
        SyncService(
            syncRepository = get(),
        )
    }

    // Federation Service
    single {
        val config: AppConfig = get()

        // Get federation keys from config or generate new ones
        val publicKey: String
        val privateKey: String
        if (
            config.security.federationPublicKey != null &&
            config.security.federationPrivateKey != null
        ) {
            // Use configured keys
            publicKey = config.security.federationPublicKey
            privateKey = config.security.federationPrivateKey
        } else {
            // Generate new keys (logs warning - should be persisted in production)
            val cryptoService = FederationCryptoService()
            val generatedPublic = cryptoService.getPublicKeyBase64()
            val generatedPrivate = cryptoService.getPrivateKeyBase64()
            if (generatedPublic != null && generatedPrivate != null) {
                logger.warn(
                    "Generated new federation keys. For production, set FEDERATION_PUBLIC_KEY and " +
                        "FEDERATION_PRIVATE_KEY environment variables to persist keys across restarts.",
                )
                publicKey = generatedPublic
                privateKey = generatedPrivate
            } else {
                // Fallback - should not happen in practice
                logger.error("Failed to generate federation keys")
                publicKey = ""
                privateKey = ""
            }
        }

        val instanceConfig = InstanceConfig(
            domain = config.server.host,
            name = "VaultStadio Instance",
            version = "2.0.0",
            publicKey = publicKey,
            privateKey = privateKey,
            capabilities = listOf(
                FederationCapability.SEND_SHARES,
                FederationCapability.RECEIVE_SHARES,
                FederationCapability.FEDERATED_IDENTITY,
                FederationCapability.FEDERATED_SEARCH,
            ),
        )
        FederationService(
            federationRepository = get(),
            instanceConfig = instanceConfig,
        )
    }

    // Collaboration Service
    single {
        CollaborationService(
            collaborationRepository = get(),
        )
    }

    // Activity Logger - automatically logs events to activity repository
    single {
        ActivityLogger(
            eventBus = get(),
            activityRepository = get(),
        ).also { it.start() }
    }
}

/**
 * Storage use case module (application layer).
 * Routes depend on these use cases instead of StorageService directly.
 */
val storageUseCaseModule = module {
    single<ListFolderUseCase> { ListFolderUseCaseImpl(get()) }
    single<GetItemUseCase> { GetItemUseCaseImpl(get()) }
    single<CreateFolderUseCase> { CreateFolderUseCaseImpl(get()) }
    single<UploadFileUseCase> { UploadFileUseCaseImpl(get()) }
    single<DownloadFileUseCase> { DownloadFileUseCaseImpl(get()) }
    single<RenameItemUseCase> { RenameItemUseCaseImpl(get()) }
    single<MoveItemUseCase> { MoveItemUseCaseImpl(get()) }
    single<CopyItemUseCase> { CopyItemUseCaseImpl(get()) }
    single<ToggleStarUseCase> { ToggleStarUseCaseImpl(get()) }
    single<TrashItemUseCase> { TrashItemUseCaseImpl(get()) }
    single<RestoreItemUseCase> { RestoreItemUseCaseImpl(get()) }
    single<DeleteItemUseCase> { DeleteItemUseCaseImpl(get()) }
    single<GetTrashItemsUseCase> { GetTrashItemsUseCaseImpl(get()) }
    single<GetStarredItemsUseCase> { GetStarredItemsUseCaseImpl(get()) }
    single<GetRecentItemsUseCase> { GetRecentItemsUseCaseImpl(get()) }
    single<GetBreadcrumbsUseCase> { GetBreadcrumbsUseCaseImpl(get()) }
    single<SetStarUseCase> { SetStarUseCaseImpl(get()) }
    single<SearchUseCase> { SearchUseCaseImpl(get()) }
    single<GetOrCreateFolderUseCase> { GetOrCreateFolderUseCaseImpl(get()) }
}

/**
 * Health use case module.
 */
val healthUseCaseModule = module {
    single<GetReadinessUseCase> { GetReadinessUseCaseImpl(get(), get()) }
    single<GetDetailedHealthUseCase> { GetDetailedHealthUseCaseImpl(get()) }
}

/**
 * Version use case module.
 */
val versionUseCaseModule = module {
    single<GetVersionHistoryUseCase> { GetVersionHistoryUseCaseImpl(get()) }
    single<GetVersionUseCase> { GetVersionUseCaseImpl(get()) }
    single<RestoreVersionUseCase> { RestoreVersionUseCaseImpl(get()) }
    single<CompareVersionsUseCase> { CompareVersionsUseCaseImpl(get()) }
    single<DeleteVersionUseCase> { DeleteVersionUseCaseImpl(get()) }
    single<ApplyRetentionPolicyUseCase> { ApplyRetentionPolicyUseCaseImpl(get()) }
}

/**
 * Sync use case module.
 */
val syncUseCaseModule = module {
    single<RegisterDeviceUseCase> { RegisterDeviceUseCaseImpl(get()) }
    single<ListDevicesUseCase> { ListDevicesUseCaseImpl(get()) }
    single<DeactivateDeviceUseCase> { DeactivateDeviceUseCaseImpl(get()) }
    single<RemoveDeviceUseCase> { RemoveDeviceUseCaseImpl(get()) }
    single<SyncPullUseCase> { SyncPullUseCaseImpl(get()) }
    single<RecordChangeUseCase> { RecordChangeUseCaseImpl(get()) }
    single<GetPendingConflictsUseCase> { GetPendingConflictsUseCaseImpl(get()) }
    single<ResolveConflictUseCase> { ResolveConflictUseCaseImpl(get()) }
    single<GenerateFileSignatureUseCase> { GenerateFileSignatureUseCaseImpl(get()) }
}

/**
 * Plugin use case module.
 */
val pluginUseCaseModule = module {
    single<ListPluginsUseCase> { ListPluginsUseCaseImpl(get()) }
    single<GetPluginUseCase> { GetPluginUseCaseImpl(get()) }
    single<GetPluginStateUseCase> { GetPluginStateUseCaseImpl(get()) }
    single<EnablePluginUseCase> { EnablePluginUseCaseImpl(get()) }
    single<DisablePluginUseCase> { DisablePluginUseCaseImpl(get()) }
    single<GetPluginEndpointsUseCase> { GetPluginEndpointsUseCaseImpl(get()) }
    single<HandlePluginEndpointUseCase> { HandlePluginEndpointUseCaseImpl(get()) }
}

/**
 * AI use case module.
 */
val aiUseCaseModule = module {
    single<AIServiceUseCase> { AIServiceUseCaseImpl(get()) }
}

/**
 * Chunked upload use case module.
 */
val chunkedUploadUseCaseModule = module {
    single<InitChunkedUploadUseCase> { InitChunkedUploadUseCaseImpl(get()) }
    single<GetChunkedUploadStatusUseCase> { GetChunkedUploadStatusUseCaseImpl(get()) }
    single<UploadChunkUseCase> { UploadChunkUseCaseImpl(get()) }
    single<CompleteChunkedUploadUseCase> { CompleteChunkedUploadUseCaseImpl(get(), get()) }
    single<CancelChunkedUploadUseCase> { CancelChunkedUploadUseCaseImpl(get()) }
}

/**
 * Metadata use case module.
 */
val metadataUseCaseModule = module {
    single<GetItemMetadataUseCase> { GetItemMetadataUseCaseImpl(get(), get()) }
    single<GetMetadataByItemIdAndPluginUseCase> { GetMetadataByItemIdAndPluginUseCaseImpl(get()) }
}

/**
 * Auth use case module.
 */
val authUseCaseModule = module {
    single<RegisterUseCase> { RegisterUseCaseImpl(get()) }
    single<LoginUseCase> { LoginUseCaseImpl(get()) }
    single<RefreshSessionUseCase> { RefreshSessionUseCaseImpl(get()) }
    single<LogoutUseCase> { LogoutUseCaseImpl(get()) }
}

/**
 * Activity use case module.
 */
val activityUseCaseModule = module {
    single<GetRecentActivityByUserUseCase> { GetRecentActivityByUserUseCaseImpl(get()) }
    single<GetRecentActivityByItemUseCase> { GetRecentActivityByItemUseCaseImpl(get()) }
}

/**
 * User use case module.
 */
val userUseCaseModule = module {
    single<GetQuotaUseCase> { GetQuotaUseCaseImpl(get()) }
    single<UpdateUserUseCase> { UpdateUserUseCaseImpl(get()) }
    single<ChangePasswordUseCase> { ChangePasswordUseCaseImpl(get()) }
    single<LogoutAllUseCase> { LogoutAllUseCaseImpl(get()) }
    single<GetUserInfoUseCase> { GetUserInfoUseCaseImpl(get()) }
}

/**
 * Admin use case module.
 */
val adminUseCaseModule = module {
    single<ListUsersUseCase> { ListUsersUseCaseImpl(get()) }
    single<GetAdminStatisticsUseCase> { GetAdminStatisticsUseCaseImpl(get()) }
    single<UpdateQuotaUseCase> { UpdateQuotaUseCaseImpl(get()) }
    single<DeleteUserUseCase> { DeleteUserUseCaseImpl(get()) }
}

/**
 * Share use case module.
 */
val shareUseCaseModule = module {
    single<GetSharesByUserUseCase> { GetSharesByUserUseCaseImpl(get()) }
    single<GetSharesSharedWithUserUseCase> { GetSharesSharedWithUserUseCaseImpl(get()) }
    single<CreateShareUseCase> { CreateShareUseCaseImpl(get()) }
    single<AccessShareUseCase> { AccessShareUseCaseImpl(get()) }
    single<GetSharesByItemUseCase> { GetSharesByItemUseCaseImpl(get()) }
    single<GetShareUseCase> { GetShareUseCaseImpl(get()) }
    single<DeleteShareUseCase> { DeleteShareUseCaseImpl(get()) }
}

/**
 * Plugin management module.
 */
val pluginModule = module {
    single<PluginManager> {
        PluginManagerImpl(
            eventBus = get(),
            storageItemRepository = get(),
            metadataRepository = get(),
            userRepository = get(),
            storageBackend = get(),
            aiService = get(),
        )
    }
}
