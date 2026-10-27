package com.vaultstadio.api.routes.collaboration

import arrow.core.left
import arrow.core.right
import com.vaultstadio.api.config.configureSecurity
import com.vaultstadio.api.config.repositoryModule
import com.vaultstadio.api.config.serviceModule
import com.vaultstadio.api.middleware.configureErrorHandling
import com.vaultstadio.api.routes.storage.StorageIntegrationFixture
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.model.CommentAnchor
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.common.exception.AuthenticationException
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.infrastructure.persistence.ExposedCollaborationRepository
import com.vaultstadio.infrastructure.persistence.ExposedShareRepository
import com.vaultstadio.infrastructure.persistence.ExposedUserRepository
import com.vaultstadio.infrastructure.persistence.entities.CollaborationOperationsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationParticipantsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.CommentRepliesTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentCommentsTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import com.vaultstadio.infrastructure.persistence.entities.UserPresenceTable
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
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
import org.koin.ktor.ext.get as koinGet

/** Actual production composition/routes, migrated PostgreSQL and local bytes; disposable token seam only. */
internal class CollaborationIntegrationFixture : AutoCloseable {
    val storage = StorageIntegrationFixture()
    val repository = ExposedCollaborationRepository()
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
    lateinit var service: CollaborationService
    lateinit var item: StorageItem
    val tables = listOf(
        CommentRepliesTable,
        DocumentCommentsTable,
        CollaborationOperationsTable,
        DocumentStatesTable,
        CollaborationParticipantsTable,
        CollaborationSessionsTable,
        UserPresenceTable,
        ShareLinksTable,
    )

    fun start() {
        storage.start()
    }

    fun reset(visibility: Visibility = Visibility.PRIVATE) = runBlocking {
        transaction { for (table in tables) table.deleteAll() }
        storage.reset()
        for (user in listOf(shared, admin)) ExposedUserRepository().create(user).fold({ throw it }, {})
        val seeded = storage.seedStoredFile("owned text\n")
        storage.repository.update(seeded.copy(mimeType = "text/plain", visibility = visibility)).fold({ throw it }, {})
        item = storage.repository.findById(seeded.id).fold({ throw it }, { checkNotNull(it) })
        ExposedShareRepository().create(
            ShareLink(
                itemId = item.id,
                token = UUID.randomUUID().toString(),
                createdBy = storage.alice.id,
                createdAt = item.createdAt,
                sharedWithUsers = listOf(shared.id),
            ),
        ).fold({ throw it }, {})
        // Existing working-state fixture: no claim that the missing upload-to-collaboration bootstrap exists.
        repository.saveDocumentState(DocumentState(item.id, 1, "owned text\n", lastModified = item.updatedAt))
            .fold({ throw it }, {})
    }

    suspend fun seedComment(): DocumentComment = repository.createComment(
        DocumentComment(
            id = UUID.randomUUID().toString(),
            itemId = item.id,
            userId = storage.alice.id,
            content = "seed comment",
            anchor = CommentAnchor(0, 0, 1, 1, "quote"),
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        ),
    ).fold({ throw it }, { it })

    fun snapshot(): List<Any> {
        fun rows(table: Table) = transaction {
            table.selectAll().map { row -> table.columns.associate { it.name to row[it]?.toString() } }
                .sortedBy { it["id"] ?: it["user_id"] }
        }
        val bytes = Files.walk(storage.contentPath).use { paths ->
            paths.filter { Files.isRegularFile(it) }.toList().associate {
                storage.contentPath.relativize(it).toString() to
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(it))
                        .joinToString("") { byte -> "%02x".format(byte) }
            }
        }
        return (listOf(StorageItemsTable) + tables).map { rows(it) } + listOf(bytes)
    }

    fun install(application: Application) = with(application) {
        val users = mockk<UserService>()
        coEvery { users.validateSession(any()) } answers {
            when (firstArg<String>()) {
                "owner" -> storage.alice.right()
                "unrelated" -> storage.bob.right()
                "shared" -> shared.right()
                "admin" -> admin.right()
                else -> AuthenticationException().left()
            }
        }
        install(Koin) {
            modules(
                repositoryModule,
                serviceModule,
                module {
                    single<UserService> { users }
                    single<StorageBackend> { storage.backend }
                    single<StorageItemRepository> { storage.repository }
                    single<EventBus> { storage.events }
                },
            )
        }
        install(ContentNegotiation) { json() }
        install(WebSockets)
        configureSecurity()
        configureErrorHandling()
        service = application.koinGet()
        routing { collaborationRoutes(service) }
    }

    override fun close() = storage.close()
}
