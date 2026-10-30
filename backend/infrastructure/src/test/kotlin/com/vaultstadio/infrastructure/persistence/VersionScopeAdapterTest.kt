package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** A two-column schema makes a full Exposed version projection fail: metadata must be unnecessary. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionScopeAdapterTest {
    private val postgres = PostgreSQLContainer("postgres:16-alpine")
    private lateinit var database: Database
    private val repository = ExposedVersionItemScopeRepository()

    @BeforeAll
    fun start() {
        postgres.start()
        database = Database.connect(
            postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password,
        )
    }

    @AfterAll fun close() = postgres.stop()

    @BeforeEach
    fun reset() {
        transaction(database) {
            exec("DROP TABLE IF EXISTS file_versions")
            exec("CREATE TABLE file_versions (id VARCHAR(36) PRIMARY KEY, item_id VARCHAR(36) NOT NULL)")
            exec("INSERT INTO file_versions VALUES ('version-one', 'item-one'), ('version-two', 'item-two')")
        }
    }

    @Test
    fun `resolve exact scope without any version metadata columns`() = runTest {
        assertEquals("item-one", assertIs<Either.Right<*>>(repository.findItemId("version-one")).value)
        assertEquals("item-two", assertIs<Either.Right<*>>(repository.findItemId("version-two")).value)
    }

    @Test
    fun `missing scope is nullable absence`() = runTest {
        assertEquals(null, assertIs<Either.Right<*>>(repository.findItemId("missing")).value)
    }

    @Test
    fun `SQL failure remains a typed error rather than absence`() = runTest {
        transaction(database) { exec("DROP TABLE file_versions") }
        assertIs<DatabaseException>(assertIs<Either.Left<*>>(repository.findItemId("version-one")).value)
    }

    @Test
    fun `cancelled scope lookup remains cancellation instead of a database failure`() = runTest {
        var observedCancellation = false
        val operation = async(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext().cancel()
            try {
                repository.findItemId("version-one")
            } catch (cancelled: CancellationException) {
                observedCancellation = true
                throw cancelled
            }
        }
        operation.join()
        assertTrue(operation.isCancelled)
        assertTrue(observedCancellation)
    }
}
