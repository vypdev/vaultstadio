package com.vaultstadio.api.routes.storage

import arrow.core.left
import arrow.core.right
import com.vaultstadio.api.config.configureSecurity
import com.vaultstadio.api.config.configureSerialization
import com.vaultstadio.api.config.repositoryModule
import com.vaultstadio.api.config.serviceModule
import com.vaultstadio.api.middleware.configureErrorHandling
import com.vaultstadio.api.routes.metadata.searchRoutes
import com.vaultstadio.application.di.applicationStorageModule
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.AuthenticationException
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.infrastructure.persistence.ExposedMetadataRepository
import com.vaultstadio.plugins.api.PluginManager
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

/** Real selection composition, migrated PostgreSQL and bytes; credentials/plugins are test seams. */
internal class SelectionIntegrationFixture : AutoCloseable {
    val storage = StorageIntegrationFixture()
    val repository = spyk(storage.repository)
    val metadata = spyk(ExposedMetadataRepository())
    val backend = spyk(storage.backend)
    val plugins = mockk<PluginManager>(relaxed = true)

    fun start() = storage.start()

    fun reset() {
        storage.reset()
        clearMocks(repository, metadata, backend, plugins)
    }

    fun install(application: Application) = with(application) {
        val users = mockk<UserService>()
        coEvery { users.validateSession(any()) } answers {
            when (firstArg<String>()) {
                "owner" -> storage.alice.right()
                "other" -> storage.bob.right()
                else -> AuthenticationException().left()
            }
        }
        install(Koin) {
            modules(
                repositoryModule,
                serviceModule,
                applicationStorageModule(),
                module {
                    single<UserService> { users }
                    single<StorageItemRepository> { repository }
                    single<MetadataRepository> { metadata }
                    single<StorageBackend> { backend }
                    single<EventBus> { storage.events }
                    single<PluginManager> { plugins }
                },
            )
        }
        configureSerialization()
        configureSecurity()
        configureErrorHandling()
        routing {
            route("/api/v1") {
                authenticate("auth-bearer") {
                    storageRoutes()
                    searchRoutes()
                }
            }
        }
    }

    override fun close() = storage.close()
}
