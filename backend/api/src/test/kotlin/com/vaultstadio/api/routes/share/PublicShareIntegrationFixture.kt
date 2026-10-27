package com.vaultstadio.api.routes.share

import arrow.core.left
import arrow.core.right
import com.vaultstadio.api.config.configureSecurity
import com.vaultstadio.api.config.configureSerialization
import com.vaultstadio.api.config.repositoryModule
import com.vaultstadio.api.config.serviceModule
import com.vaultstadio.api.middleware.configureErrorHandling
import com.vaultstadio.api.middleware.configureLogging
import com.vaultstadio.api.routes.collaboration.collaborationRoutes
import com.vaultstadio.api.routes.storage.StorageIntegrationFixture
import com.vaultstadio.api.routes.storage.storageRoutes
import com.vaultstadio.api.routes.version.versionRoutes
import com.vaultstadio.application.di.applicationShareModule
import com.vaultstadio.application.di.applicationStorageModule
import com.vaultstadio.application.di.applicationVersionModule
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.service.PasswordHasher
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.AuthenticationException
import com.vaultstadio.domain.share.access.PublicShareClock
import com.vaultstadio.domain.share.access.ShareContentReader
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.infrastructure.persistence.ExposedPublicShareAccess
import com.vaultstadio.infrastructure.persistence.ExposedShareRepository
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import com.vaultstadio.infrastructure.security.BCryptPasswordHasher
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import org.koin.ktor.ext.get as koinGet

/** Production repositories/composition/policy/BCrypt/routes + migrated PostgreSQL + local byte provider. */
internal class PublicShareIntegrationFixture : AutoCloseable {
    val storage = StorageIntegrationFixture()
    val now = Instant.parse("2026-10-27T00:00:00Z")
    val time = AtomicReference(now)
    val clock = PublicShareClock { time.get() }
    val repository = ExposedPublicShareAccess(clock)
    val shares = ExposedShareRepository()
    val hasher = BCryptPasswordHasher(cost = 4)
    lateinit var item: StorageItem
    lateinit var share: ShareLink

    fun start() = storage.start()

    fun reset() = runBlocking {
        transaction { ShareLinksTable.deleteAll() }
        storage.reset()
        time.set(now)
        item = storage.seedStoredFile("shared bytes ☃\n")
        share = ShareLink(
            id = UUID.randomUUID().toString(),
            itemId = item.id,
            token = "CaSe_" + UUID.randomUUID(),
            createdBy = storage.alice.id,
            createdAt = now,
        )
        shares.create(share).fold({ throw it }, {})
    }

    suspend fun update(value: ShareLink) {
        shares.update(value).fold({ throw it }, {})
        share = shares.findById(value.id).fold({ throw it }, { checkNotNull(it) })
    }

    suspend fun count(): Int = shares.findById(share.id).fold({ throw it }, { it?.downloadCount ?: 0 })

    fun install(
        application: Application,
        contentOverride: ShareContentReader? = null,
        backendOverride: StorageBackend? = null,
    ) = with(application) {
        val users = mockk<UserService>()
        coEvery { users.validateSession(any()) } answers {
            when (firstArg<String>()) {
                "owner" -> storage.alice.right()
                "unrelated" -> storage.bob.right()
                else -> AuthenticationException().left()
            }
        }
        install(Koin) {
            modules(
                repositoryModule,
                serviceModule,
                applicationShareModule(),
                applicationStorageModule(),
                applicationVersionModule(),
                module {
                    single<UserService> { users }
                    single<StorageBackend> { backendOverride ?: storage.backend }
                    single<EventBus> { storage.events }
                    single<PasswordHasher> { hasher }
                    single<PublicShareClock> { clock }
                    if (contentOverride != null) single<ShareContentReader> { contentOverride }
                },
            )
        }
        configureSerialization()
        install(WebSockets)
        configureSecurity()
        configureErrorHandling()
        configureLogging()
        routing {
            route("/api/v1") {
                publicShareRoutes()
                authenticate("auth-bearer") {
                    shareRoutes()
                    storageRoutes()
                }
            }
            versionRoutes()
            collaborationRoutes(application.koinGet())
        }
    }

    override fun close() = storage.close()
}
