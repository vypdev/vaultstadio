package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import com.vaultstadio.domain.collaboration.repository.CollaborationSessionScope
import com.vaultstadio.domain.common.exception.DatabaseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Minimal schemas omit all full-model columns: a broad projection fails these contracts. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationScopeAdapterTest {
    private val postgres = PostgreSQLContainer("postgres:16-alpine")
    private lateinit var database: Database
    private val repository = ExposedCollaborationScopeRepository()

    @BeforeAll fun start() {
        postgres.start()
        database =
            Database.connect(
                postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password,
            )
    }

    @AfterAll fun close() = postgres.stop()

    @BeforeEach fun reset() {
        transaction(database) {
            exec("DROP TABLE IF EXISTS collaboration_sessions, collaboration_participants, document_comments")
            exec("CREATE TABLE collaboration_sessions (id VARCHAR(36), item_id VARCHAR(36), closed_at TIMESTAMP NULL)")
            exec("CREATE TABLE collaboration_participants (id VARCHAR(36), session_id VARCHAR(36))")
            exec("CREATE TABLE document_comments (id VARCHAR(36), item_id VARCHAR(36))")
            exec("INSERT INTO collaboration_sessions VALUES ('known', 'item', NULL)")
            exec("INSERT INTO collaboration_participants VALUES ('known', 'session')")
            exec("INSERT INTO document_comments VALUES ('known', 'item')")
        }
    }

    @ParameterizedTest(name = "C89-SQL {0} positive")
    @CsvSource("session", "participant", "comment")
    fun `scope reads no foreign model columns`(projection: String) = runTest {
        val expected = when (projection) {
            "session" -> CollaborationSessionScope("item", false)
            "participant" -> "session"
            else -> "item"
        }
        assertEquals(expected, assertIs<Either.Right<*>>(lookup(projection, "known")).value)
    }

    @ParameterizedTest(name = "C89-SQL {0} missing")
    @CsvSource("session", "participant", "comment")
    fun `missing scope is nullable absence`(projection: String) = runTest {
        assertEquals(null, assertIs<Either.Right<*>>(lookup(projection, "missing")).value)
    }

    @ParameterizedTest(name = "C89-SQL {0} SQL-failure")
    @CsvSource("session,collaboration_sessions", "participant,collaboration_participants", "comment,document_comments")
    fun `real SQL failure is typed`(projection: String, table: String) = runTest {
        transaction(database) { exec("DROP TABLE $table") }
        assertIs<DatabaseException>(assertIs<Either.Left<*>>(lookup(projection, "known")).value)
    }

    @ParameterizedTest(name = "C89-SQL {0} cancellation")
    @CsvSource("session", "participant", "comment")
    fun `scope cancellation is preserved`(projection: String) = runTest {
        var observed = false
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext().cancel()
            try {
                lookup(projection, "known")
            } catch (error: CancellationException) {
                observed = true
                throw error
            }
        }
        request.join()
        assertTrue(request.isCancelled)
        assertTrue(observed)
    }
    private suspend fun lookup(projection: String, id: String): Either<*, *> = when (projection) {
        "session" -> repository.session(id)
        "participant" -> repository.participantSession(id)
        "comment" -> repository.commentItem(id)
        else -> error("Unenumerated projection")
    }
}
