/**
 * VaultStadio Koin Dependency Injection Configuration
 */

package com.vaultstadio.api.config

import com.vaultstadio.api.plugins.PluginManagerImpl
import com.vaultstadio.application.di.applicationActivityModule
import com.vaultstadio.application.di.applicationAdminModule
import com.vaultstadio.application.di.applicationAiModule
import com.vaultstadio.application.di.applicationAuthModule
import com.vaultstadio.application.di.applicationChunkeduploadModule
import com.vaultstadio.application.di.applicationHealthModule
import com.vaultstadio.application.di.applicationMetadataModule
import com.vaultstadio.application.di.applicationPluginModule
import com.vaultstadio.application.di.applicationShareModule
import com.vaultstadio.application.di.applicationStorageModule
import com.vaultstadio.application.di.applicationSyncModule
import com.vaultstadio.application.di.applicationUserModule
import com.vaultstadio.application.di.applicationVersionModule
import com.vaultstadio.plugins.api.PluginManager
import com.vaultstadio.api.routes.storage.S3Operations
import com.vaultstadio.api.routes.storage.WebDAVOperations
import com.vaultstadio.api.service.InMemoryThumbnailCache
import com.vaultstadio.api.service.InMemoryUploadSessionManager
import com.vaultstadio.api.service.ThumbnailCache
import com.vaultstadio.core.domain.service.UploadSessionManager
import com.vaultstadio.core.ai.AIService
import com.vaultstadio.core.ai.AIServiceImpl
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.model.FederationCapability
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.domain.repository.SyncRepository
import com.vaultstadio.domain.activity.repository.ActivityRepository
import com.vaultstadio.domain.auth.repository.ApiKeyRepository
import com.vaultstadio.domain.auth.repository.SessionRepository
import com.vaultstadio.domain.auth.repository.UserRepository
import com.vaultstadio.domain.share.repository.ShareRepository
import com.vaultstadio.domain.storage.repository.StorageItemRepository
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
            applicationAuthModule(),
            applicationStorageModule(),
            applicationShareModule(),
            applicationUserModule(),
            applicationAdminModule(),
            applicationActivityModule(),
            applicationMetadataModule(),
            applicationVersionModule(),
            applicationSyncModule(),
            applicationPluginModule(),
            applicationChunkeduploadModule(),
            applicationHealthModule(),
            applicationAiModule(),
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
