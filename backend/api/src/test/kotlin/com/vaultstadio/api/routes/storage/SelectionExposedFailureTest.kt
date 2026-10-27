package com.vaultstadio.api.routes.storage

import com.vaultstadio.domain.common.exception.DatabaseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectionExposedFailureTest {
    private val f = SelectionIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @Test fun actualExposedDuplicateKeyRetainsTypedErrorAndTheOriginalSqlCause() = testApplication {
        val original = f.storage.seedStoredFile("retained after rollback")
        val failure = f.repository.create(original).fold({ it }, { error("Duplicate primary key succeeded") })
        assertIs<DatabaseException>(failure)
        assertEquals("Failed to create storage item", failure.message)
        val wrapper = assertIs<ExposedSQLException>(failure.cause)
        assertEquals(true, SQLException::class.java.isInstance(wrapper))
        val jdbc = assertIs<SQLException>(assertNotNull(wrapper.cause))
        assertEquals("23505", jdbc.sqlState)
        assertEquals(original, f.repository.findById(original.id).fold({ throw it }, { it }))
        assertEquals(1L, f.storage.rowCount())
        assertEquals(1L, f.storage.byteFileCount())
    }

    @Test fun actualExposedSelectionFailureIsDatabase500AndRecoversAfterTheFixtureFault() = testApplication {
        f.storage.seedStoredFile("retained through selection failure")
        application { f.install(this) }
        // Only this test's migrated PostgreSQL container is affected; restore even if an assertion fails.
        transaction { exec("ALTER TABLE storage_items RENAME TO selection_failure_fixture") }
        try {
            val response = client.get("/api/v1/storage/folder?limit=1") { bearerAuth("owner") }
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val body = response.bodyAsText()
            val error = Json.parseToJsonElement(body).jsonObject.getValue("error").jsonObject
            assertEquals("DATABASE_ERROR", error.getValue("code").jsonPrimitive.content)
            assertEquals("Failed to query storage items", error.getValue("message").jsonPrimitive.content)
            assertFalse(body.contains("selection_failure_fixture"))
            assertFalse(body.contains("PSQLException"))
        } finally {
            transaction { exec("ALTER TABLE selection_failure_fixture RENAME TO storage_items") }
        }
        assertEquals(
            HttpStatusCode.OK,
            client.get("/api/v1/storage/folder?limit=1") { bearerAuth("owner") }.status,
        )
        assertEquals(1L, f.storage.rowCount())
        assertEquals(1L, f.storage.byteFileCount())
    }
}
