package com.vaultstadio.api.routes.auth

import com.vaultstadio.api.config.configureSecurity
import com.vaultstadio.api.middleware.configureErrorHandling
import com.vaultstadio.api.routes.storage.storageRoutes
import com.vaultstadio.application.di.applicationAuthModule
import com.vaultstadio.application.di.applicationStorageModule
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserSession
import com.vaultstadio.domain.auth.repository.SessionRepository
import com.vaultstadio.infrastructure.persistence.ExposedSessionRepository
import com.vaultstadio.infrastructure.persistence.ExposedStorageItemRepository
import com.vaultstadio.infrastructure.persistence.ExposedUserRepository
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import com.vaultstadio.infrastructure.persistence.entities.UserSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.UsersTable
import com.vaultstadio.infrastructure.security.BCryptPasswordHasher
import com.vaultstadio.infrastructure.storage.LocalStorageBackend
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.testcontainers.containers.PostgreSQLContainer
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Comparator

/** All policy, credentials, SQL, routes and bytes are real; the event delivery boundary is inert. */
internal class Auth119Fixture : AutoCloseable {
    private val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
        withDatabaseName("auth119_test")
        withUsername("test")
        withPassword("test")
    }
    private lateinit var database: Database
    val users = ExposedUserRepository()
    val sessions = ExposedSessionRepository()
    val hasher = BCryptPasswordHasher()
    private val events = mockk<EventBus>(relaxed = true)
    var afterRefreshRead: suspend () -> Unit = {}
    private val observedSessions = object : SessionRepository by sessions {
        override suspend fun findByRefreshTokenHash(refreshTokenHash: String) =
            sessions.findByRefreshTokenHash(refreshTokenHash).also { afterRefreshRead() }
    }
    val userService = UserService(users, observedSessions, hasher, events)
    val contentPath = Files.createTempDirectory("vaultstadio-auth119-")
    val items = ExposedStorageItemRepository()
    private val storage = StorageService(items, LocalStorageBackend(contentPath), events)

    fun start() {
        postgres.start()
        Flyway.configure().dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration").load().migrate()
        database =
            Database.connect(
                postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password,
            )
    }
    fun reset() {
        afterRefreshRead = {}
        transaction(database) {
            UserSessionsTable.deleteAll()
            StorageItemsTable.deleteAll()
            UsersTable.deleteAll()
        }
        Files.walk(contentPath).use { paths ->
            paths.sorted(Comparator.reverseOrder()).filter { it != contentPath }.forEach { Files.delete(it) }
        }
    }
    fun install(application: Application) = with(application) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                },
            )
        }
        install(Koin) {
            modules(
                module {
                    single { userService }
                    single { storage }
                },
                applicationAuthModule(),
                applicationStorageModule(),
            )
        }
        configureErrorHandling()
        configureSecurity()
        routing {
            route("/api/v1") {
                authRoutes()
                authenticate("auth-bearer") { storageRoutes() }
            }
        }
    }
    fun userCount(): Long = transaction(database) { UsersTable.selectAll().count() }
    fun sessionCount(): Long = transaction(database) { UserSessionsTable.selectAll().count() }
    fun storageCount(): Long = transaction(database) { StorageItemsTable.selectAll().count() }
    fun user(email: String = "owner119@audit.invalid"): User = runBlocking {
        users.findByEmail(email).fold({ throw it }, { checkNotNull(it) })
    }
    fun tokenHash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun session(token: String): UserSession = runBlocking {
        val hash = tokenHash(token)
        sessions.findByTokenHash(hash).fold({ throw it }, { checkNotNull(it) })
    }
    fun item(id: String) = runBlocking { items.findById(id).fold({ throw it }, { checkNotNull(it) }) }
    fun contentHashes(): Map<String, String> = Files.walk(contentPath).use { paths ->
        paths.filter { Files.isRegularFile(it) }.toList().associate {
            contentPath.relativize(it).toString() to MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(it)).joinToString("") { byte -> "%02x".format(byte) }
        }
    }
    suspend fun <T> storageUnavailable(block: suspend () -> T): T {
        transaction(database) { exec("ALTER TABLE storage_items RENAME TO auth119_unavailable_items") }
        try {
            return block()
        } finally {
            transaction(database) { exec("ALTER TABLE auth119_unavailable_items RENAME TO storage_items") }
        }
    }
    suspend fun <T> sessionsUnavailable(block: suspend () -> T): T {
        transaction(database) { exec("ALTER TABLE user_sessions RENAME TO auth119_unavailable_sessions") }
        try {
            return block()
        } finally {
            transaction(database) { exec("ALTER TABLE auth119_unavailable_sessions RENAME TO user_sessions") }
        }
    }
    override fun close() {
        postgres.stop()
        Files.walk(contentPath).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
    }
}
