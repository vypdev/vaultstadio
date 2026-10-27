package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import com.vaultstadio.domain.common.exception.DatabaseException
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

/** Minimal schemas prohibit accidental full item/version hydration during content liveness checks. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionContentReferenceAdapterTest {
    private val postgres = PostgreSQLContainer("postgres:16-alpine")
    private lateinit var database: Database
    private val repository = ExposedVersionContentReferenceRepository()

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
            exec("DROP TABLE IF EXISTS file_versions, storage_items")
            exec("CREATE TABLE file_versions (id VARCHAR(36) PRIMARY KEY, storage_key VARCHAR(500) NOT NULL)")
            exec("CREATE TABLE storage_items (id VARCHAR(36) PRIMARY KEY, storage_key VARCHAR(500))")
            exec("INSERT INTO file_versions VALUES ('target', 'shared')")
        }
    }

    @Test
    fun `the excluded version alone does not retain its content`() = runTest {
        assertEquals(false, assertIs<Either.Right<*>>(repository.hasOtherReferences("shared", setOf("target"))).value)
        assertEquals(false, assertIs<Either.Right<*>>(repository.hasOtherReferences("absent", setOf("target"))).value)
    }

    @Test
    fun `another version retains content without requiring other metadata columns`() = runTest {
        transaction(database) { exec("INSERT INTO file_versions VALUES ('peer', 'shared')") }
        assertEquals(true, assertIs<Either.Right<*>>(repository.hasOtherReferences("shared", setOf("target"))).value)
    }

    @Test
    fun `any current item retains content without requiring owner or name columns`() = runTest {
        transaction(database) { exec("INSERT INTO storage_items VALUES ('active', 'shared')") }
        assertEquals(true, assertIs<Either.Right<*>>(repository.hasOtherReferences("shared", setOf("target"))).value)
    }

    @Test
    fun `SQL failure is not interpreted as unreferenced content`() = runTest {
        transaction(database) { exec("DROP TABLE file_versions") }
        assertIs<DatabaseException>(
            assertIs<Either.Left<*>>(repository.hasOtherReferences("shared", setOf("target"))).value,
        )
    }

    @Test
    fun `the complete deleted batch does not retain shared content`() = runTest {
        transaction(database) { exec("INSERT INTO file_versions VALUES ('peer', 'shared')") }
        val result = repository.hasOtherReferences("shared", setOf("target", "peer"))
        assertEquals(false, assertIs<Either.Right<*>>(result).value)
    }

    @Test
    fun `a version outside the deletion batch still retains content`() = runTest {
        transaction(database) {
            exec("INSERT INTO file_versions VALUES ('peer', 'shared'), ('outside', 'shared')")
        }
        val result = repository.hasOtherReferences("shared", setOf("target", "peer"))
        assertEquals(true, assertIs<Either.Right<*>>(result).value)
    }

    @Test
    fun `empty deletion batch retains every existing reference`() = runTest {
        val result = repository.hasOtherReferences("shared", emptySet())
        assertEquals(true, assertIs<Either.Right<*>>(result).value)
    }
}
