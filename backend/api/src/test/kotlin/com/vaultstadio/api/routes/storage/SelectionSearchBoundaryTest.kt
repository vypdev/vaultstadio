package com.vaultstadio.api.routes.storage

import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.domain.common.exception.DatabaseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectionSearchBoundaryTest {
    private val f = SelectionIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest
    @ValueSource(strings = ["fromDate", "toDate"])
    fun invalidDateIsSafeBadRequestBeforeAnySelection(field: String) = testApplication {
        application { f.install(this) }
        val response = client.post("/api/v1/search/advanced") {
            bearerAuth("owner")
            contentType(ContentType.Application.Json)
            setBody("{\"query\":\"prefix\",\"$field\":\"private-invalid-date\"}")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(response.bodyAsText().contains("private-invalid-date"))
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error").jsonObject
        assertEquals("VALIDATION_ERROR", error.getValue("code").jsonPrimitive.content)
        assertEquals("Invalid $field", error.getValue("message").jsonPrimitive.content)
        verifyNoSelection()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", ",\"limit\":0", ",\"offset\":-1", ",\"fromDate\":\"invalid\""])
    fun unavailableContentSearchNeverBecomesAFakeSuccess(extra: String) = testApplication {
        application { f.install(this) }
        val response = client.post("/api/v1/search/advanced") {
            bearerAuth("owner")
            contentType(ContentType.Application.Json)
            setBody("{\"query\":\"prefix\",\"searchContent\":true$extra}")
        }
        assertEquals(if (extra.isEmpty()) HttpStatusCode.NotImplemented else HttpStatusCode.BadRequest, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("false", body.getValue("success").jsonPrimitive.content)
        val code = body.getValue("error").jsonObject.getValue("code").jsonPrimitive.content
        assertEquals(if (extra.isEmpty()) "CONTENT_SEARCH_UNAVAILABLE" else "VALIDATION_ERROR", code)
        verifyNoSelection()
    }

    @ParameterizedTest
    @CsvSource(
        "2026-10-27T12:00:00Z,2026-10-27T12:00:00Z,1",
        "2026-10-27T12:00:01Z,2026-10-27T12:00:02Z,0",
        "2026-10-27T11:59:58Z,2026-10-27T11:59:59Z,0",
    )
    fun dateProjectionRetainsInclusiveBounds(from: String, to: String, expected: Int) = testApplication {
        f.storage.seedStoredFile(timestamp = Instant.parse("2026-10-27T12:00:00Z"))
        application { f.install(this) }
        val response = client.post("/api/v1/search/advanced") {
            bearerAuth("owner")
            contentType(ContentType.Application.Json)
            setBody("{\"query\":\"prefix\",\"fromDate\":\"$from\",\"toDate\":\"$to\"}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
        assertEquals(expected, data.getValue("items").jsonArray.size)
    }

    @Test fun realMetadataRowsExposeOnlyTheOwnerProjection() = testApplication {
        val ownerItem = f.storage.seedStoredFile()
        val foreignItem = f.storage.seed(owner = f.storage.bob)
        for (item in listOf(ownerItem, foreignItem)) {
            f.metadata.save(metadata(item.id)).fold({ throw it }, {})
        }
        application { f.install(this) }
        val response = client.get("/api/v1/search/by-metadata?key=kind&limit=1000") { bearerAuth("owner") }
        assertEquals(HttpStatusCode.OK, response.status)
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
        assertEquals("1", data.getValue("total").jsonPrimitive.content)
        assertEquals("1", data.getValue("totalPages").jsonPrimitive.content)
        assertEquals(
            ownerItem.id,
            data.getValue("items").jsonArray.single().jsonObject["itemId"]?.jsonPrimitive?.content,
        )
        coVerify(exactly = 1) { f.repository.findById(foreignItem.id) }
    }

    @Test fun missingMetadataItemIsExcludedWithoutDisclosingItsRecord() = testApplication {
        coEvery { f.metadata.searchByKeyValue(any(), any(), any(), any()) } returns
            listOf(metadata("missing-selection-item")).right()
        application { f.install(this) }
        val response = client.get("/api/v1/search/by-metadata?key=kind") { bearerAuth("owner") }
        assertEquals(HttpStatusCode.OK, response.status)
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
        assertEquals(0, data.getValue("items").jsonArray.size)
        assertEquals("0", data.getValue("totalPages").jsonPrimitive.content)
    }

    @Test fun metadataItemProviderFailureIsNotSilentlyExcluded() = testApplication {
        val item = f.storage.seedStoredFile()
        f.metadata.save(metadata(item.id)).fold({ throw it }, {})
        coEvery { f.repository.findById(item.id) } returns DatabaseException("selection read unavailable").left()
        application { f.install(this) }
        val response = client.get("/api/v1/search/by-metadata?key=kind") { bearerAuth("owner") }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error").jsonObject
        assertEquals("DATABASE_ERROR", error.getValue("code").jsonPrimitive.content)
    }

    private fun metadata(itemId: String) = StorageItemMetadata(
        itemId = itemId,
        pluginId = "selection-test",
        key = "kind",
        value = "file",
        createdAt = Instant.parse("2026-10-27T12:00:00Z"),
        updatedAt = Instant.parse("2026-10-27T12:00:00Z"),
    )

    private fun verifyNoSelection() {
        coVerify(exactly = 0) {
            f.repository.query(any())
            f.repository.findById(any())
            f.repository.count(any())
            f.metadata.searchByKeyValue(any(), any(), any(), any())
            f.backend.retrieve(any())
        }
        assertEquals(0L, f.storage.rowCount())
        assertEquals(0L, f.storage.byteFileCount())
    }
}
