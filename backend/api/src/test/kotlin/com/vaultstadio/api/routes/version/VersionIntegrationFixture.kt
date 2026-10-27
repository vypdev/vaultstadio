package com.vaultstadio.api.routes.version

import arrow.core.left
import arrow.core.right
import com.vaultstadio.api.config.configureSecurity
import com.vaultstadio.api.config.versionServicesModule
import com.vaultstadio.api.middleware.configureErrorHandling
import com.vaultstadio.api.routes.storage.StorageIntegrationFixture
import com.vaultstadio.application.di.applicationVersionModule
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.common.exception.AuthenticationException
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.infrastructure.persistence.ExposedFileVersionRepository
import com.vaultstadio.infrastructure.persistence.ExposedShareRepository
import com.vaultstadio.infrastructure.persistence.ExposedUserRepository
import com.vaultstadio.infrastructure.persistence.entities.FileVersionsTable
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID

/** Real migrated PostgreSQL, production ports/use cases/routes and actual filesystem bytes. */
internal class VersionIntegrationFixture : AutoCloseable {
    val storage = StorageIntegrationFixture()
    val repository = ExposedFileVersionRepository()
    val shared = storage.bob.copy(
        id = UUID.randomUUID().toString(),
        email = "shared@audit.invalid",
        username = "shared",
    )
    val admin = storage.bob.copy(
        id = UUID.randomUUID().toString(),
        email = "admin@audit.invalid",
        username = "admin",
        role = UserRole.ADMIN,
    )
    val actors = mapOf("owner" to storage.alice, "unrelated" to storage.bob, "shared" to shared, "admin" to admin)
    lateinit var item: StorageItem
    lateinit var old: FileVersion
    lateinit var latest: FileVersion

    fun start() {
        storage.postgres.withCommand("postgres", "-c", "log_statement=all")
        storage.start()
    }

    fun reset(visibility: Visibility = Visibility.PRIVATE) = runBlocking {
        transaction {
            FileVersionsTable.deleteAll()
            ShareLinksTable.deleteAll()
        }
        storage.reset()
        for (user in listOf(shared, admin)) ExposedUserRepository().create(user).fold({ throw it }, {})
        val seeded = storage.seedStoredFile("new\n")
        storage.repository.update(
            seeded.copy(name = "sensitive-history.txt", mimeType = "text/plain", visibility = visibility),
        ).fold({
            throw it
        }, {})
        item = storage.repository.findById(seeded.id).fold({ throw it }, { checkNotNull(it) })
        val key = storage.backend.store("old\n".byteInputStream(), 4, "text/plain").fold({ throw it }, { it })
        old = FileVersion(
            UUID.randomUUID().toString(), item.id, 1, 4, digest("old\n".toByteArray()), key,
            storage.bob.id, item.createdAt, isLatest = false,
        )
        latest =
            old.copy(
                id = UUID.randomUUID().toString(),
                versionNumber = 2,
                checksum = digest("new\n".toByteArray()),
                storageKey = checkNotNull(item.storageKey),
                isLatest = true,
            )
        repository.create(old).fold({ throw it }, {})
        repository.create(latest).fold({ throw it }, {})
        ExposedShareRepository().create(
            ShareLink(
                itemId = item.id,
                token = UUID.randomUUID().toString(),
                createdBy = storage.alice.id,
                createdAt = item.createdAt,
                sharedWithUsers = listOf(shared.id),
            ),
        ).fold({
            throw it
        }, {})
    }

    fun snapshot(): List<Any> {
        fun rows(table: Table) = transaction {
            table.selectAll().map { row ->
                table.columns.associate { column -> column.name to row[column]?.toString() }
            }
                .sortedBy { it["id"] }
        }
        val bytes = Files.walk(storage.contentPath).use { paths ->
            paths.filter { Files.isRegularFile(it) }.toList().associate { path ->
                storage.contentPath.relativize(path).toString() to digest(Files.readAllBytes(path))
            }
        }
        return listOf(rows(StorageItemsTable), rows(FileVersionsTable), rows(ShareLinksTable), bytes)
    }

    fun install(application: Application) = with(application) {
        val sessions = mockk<UserService>()
        coEvery { sessions.validateSession(any()) } answers {
            actors[firstArg<String>()]?.right() ?: AuthenticationException().left()
        }
        install(Koin) {
            modules(
                module {
                    single<StorageBackend> { storage.backend }
                    single<StorageItemRepository> { storage.repository }
                    single<UserService> { sessions }
                },
                versionServicesModule(),
                applicationVersionModule(),
            )
        }
        install(ContentNegotiation) { json() }
        configureSecurity()
        configureErrorHandling()
        routing { versionRoutes() }
    }

    override fun close() = storage.close()

    companion object {
        fun digest(
            bytes: ByteArray,
        ) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
