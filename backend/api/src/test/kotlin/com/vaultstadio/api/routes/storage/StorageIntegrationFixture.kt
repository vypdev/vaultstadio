package com.vaultstadio.api.routes.storage

import arrow.core.left
import arrow.core.right
import com.vaultstadio.api.config.configureSecurity
import com.vaultstadio.api.middleware.configureErrorHandling
import com.vaultstadio.application.di.applicationStorageModule
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.common.exception.AuthenticationException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.infrastructure.persistence.ExposedStorageItemRepository
import com.vaultstadio.infrastructure.persistence.ExposedUserRepository
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import com.vaultstadio.infrastructure.persistence.entities.UsersTable
import com.vaultstadio.infrastructure.storage.LocalStorageBackend
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.testcontainers.containers.PostgreSQLContainer
import java.nio.file.Files
import java.util.Comparator
import java.util.UUID

/** Real migrated PostgreSQL + real local bytes; only credential validation is a test seam. */
internal class StorageIntegrationFixture : AutoCloseable {
    val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
        withDatabaseName("storage_ownership_test")
        withUsername("test")
        withPassword("test")
    }
    val contentPath = Files.createTempDirectory("vaultstadio-storage-ownership-")
    val repository = ExposedStorageItemRepository()
    val backend = LocalStorageBackend(contentPath)
    val events = mockk<EventBus>(relaxed = true)
    val service = StorageService(repository, backend, events)
    val alice = testUser("alice")
    val bob = testUser("bob")
    private lateinit var database: Database

    fun start() {
        postgres.start()
        Flyway.configure().dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration").load().migrate()
        database = Database.connect(postgres.jdbcUrl, driver = "org.postgresql.Driver", user = postgres.username, password = postgres.password)
    }

    fun reset() = runBlocking {
        transaction(database) {
            StorageItemsTable.deleteAll()
            UsersTable.deleteAll()
        }
        Files.walk(contentPath).use { paths ->
            paths.sorted(Comparator.reverseOrder()).filter { it != contentPath }.forEach { Files.delete(it) }
        }
        for (user in listOf(alice, bob)) ExposedUserRepository().create(user).fold({ throw it }, {})
        clearMocks(events)
    }

    suspend fun seed(
        owner: User = alice,
        visibility: Visibility = Visibility.PRIVATE,
        parentId: String? = null,
        type: ItemType = ItemType.FOLDER,
        trashed: Boolean = false,
    ): StorageItem {
        val id = UUID.randomUUID().toString()
        val now = Clock.System.now()
        val item = StorageItem(
            id = id, name = id, path = "/$id", type = type, ownerId = owner.id,
            visibility = visibility, parentId = parentId, isTrashed = trashed,
            createdAt = now, updatedAt = now,
        )
        return repository.create(item).fold({ throw it }, { it })
    }

    fun rowCount(): Long = transaction(database) { StorageItemsTable.selectAll().count() }
    fun byteFileCount(): Long = Files.walk(contentPath).use { paths -> paths.filter { Files.isRegularFile(it) }.count() }

    fun install(application: Application) = with(application) {
        val users = mockk<UserService>()
        coEvery { users.validateSession(any()) } answers {
            when (firstArg<String>()) {
                "alice-test-session" -> alice.right()
                "bob-test-session" -> bob.right()
                else -> AuthenticationException().left()
            }
        }
        install(Koin) {
            modules(module { single { service }; single<UserService> { users } }, applicationStorageModule())
        }
        install(ContentNegotiation) { json() }
        configureSecurity()
        configureErrorHandling()
        routing {
            route("/api/v1") {
                authenticate("auth-bearer") {
                    storageRoutes()
                    folderUploadRoutes()
                }
            }
        }
    }

    override fun close() {
        postgres.stop()
        Files.walk(contentPath).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
    }

    private fun testUser(name: String): User {
        val now = Clock.System.now()
        return User(
            id = UUID.randomUUID().toString(), email = "$name@audit.invalid", username = name,
            passwordHash = "unused-session-validation-seam", role = UserRole.USER, status = UserStatus.ACTIVE,
            createdAt = now, updatedAt = now,
        )
    }
}
