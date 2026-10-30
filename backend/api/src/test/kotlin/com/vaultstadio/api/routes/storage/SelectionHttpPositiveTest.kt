package com.vaultstadio.api.routes.storage

import arrow.core.left
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
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
class SelectionHttpPositiveTest {
    private val f = SelectionIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "P91-http-default {0}")
    @CsvSource(
        "/storage/folder,100",
        "/storage/recent,20",
        "/search?q=prefix,50",
        "/search/suggestions?prefix=prefix,10",
        "/search/by-metadata?key=kind,50",
    )
    fun defaultsReachProductionSelectionPorts(path: String, expectedLimit: Int) = testApplication {
        application { f.install(this) }
        val response = client.get("/api/v1$path") { bearerAuth("owner") }
        assertEquals(HttpStatusCode.OK, response.status)
        if (path.contains("by-metadata")) {
            coVerify(exactly = 1) { f.metadata.searchByKeyValue("kind", "%", null, expectedLimit) }
            val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
            assertEquals(1L, data.getValue("page").jsonPrimitive.long)
            assertEquals(0L, data.getValue("totalPages").jsonPrimitive.long)
        } else {
            coVerify(exactly = 1) {
                f.repository.query(
                    match {
                        it.limit == expectedLimit &&
                            it.offset == 0 &&
                            it.ownerId == f.storage.alice.id
                    },
                )
            }
        }
    }

    @ParameterizedTest(name = "P91-http-range {0}/{1}")
    @CsvSource("1,0", "1000,0", "1,2147483647")
    fun exactPageMetadataIncludesLargestOffset(limit: Int, offset: Int) = testApplication {
        f.storage.seedStoredFile("first")
        f.storage.seedStoredFile("second")
        application { f.install(this) }
        val response = client.get("/api/v1/storage/folder?limit=$limit&offset=$offset") { bearerAuth("owner") }
        assertEquals(HttpStatusCode.OK, response.status)
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
        assertEquals(2L, data.getValue("total").jsonPrimitive.long)
        assertEquals(offset.toLong() / limit + 1L, data.getValue("page").jsonPrimitive.long)
        assertEquals(if (limit == 1) 2L else 1L, data.getValue("totalPages").jsonPrimitive.long)
        assertEquals(if (offset > 0) 0 else minOf(limit, 2), data.getValue("items").jsonArray.size)
        if (offset > 0) assertEquals("false", data.getValue("hasMore").jsonPrimitive.content)
    }

    @ParameterizedTest(name = "P91-http-parent-authority {0}")
    @ValueSource(strings = ["foreign", "missing", "file"])
    fun validInputRetainsParentAuthorizationAndTypeErrors(case: String) = testApplication {
        val id = when (case) {
            "foreign" -> f.storage.seed(owner = f.storage.bob).id
            "file" -> f.storage.seedStoredFile().id
            else -> "missing-parent"
        }
        application { f.install(this) }
        val response = client.get("/api/v1/storage/folder/$id?limit=1") { bearerAuth("owner") }
        assertEquals(
            when (case) {
                "foreign" -> HttpStatusCode.Forbidden
                "file" -> HttpStatusCode.BadRequest
                else -> HttpStatusCode.NotFound
            },
            response.status,
        )
        coVerify(exactly = 0) { f.repository.query(any()) }
        coVerify(exactly = 1) { f.repository.findById(id) }
    }

    @ParameterizedTest(name = "P91-http-provider-identity {0}")
    @ValueSource(strings = ["folder", "recent", "search-get", "search-post", "advanced", "suggestions", "metadata"])
    fun providerFailureStaysAnExplicitDatabaseFailure(case: String) = testApplication {
        coEvery { f.repository.query(any()) } returns DatabaseException("selection unavailable").left()
        coEvery { f.metadata.searchByKeyValue(any(), any(), any(), any()) } returns
            DatabaseException("metadata unavailable").left()
        application { f.install(this) }
        val response = when (case) {
            "search-post", "advanced" -> client.post("/api/v1/search" + if (case == "advanced") "/advanced" else "") {
                bearerAuth("owner")
                contentType(ContentType.Application.Json)
                setBody("{\"query\":\"prefix\",\"limit\":1}")
            }
            else -> client.get(
                "/api/v1" + when (case) {
                    "folder" -> "/storage/folder?limit=1"
                    "recent" -> "/storage/recent?limit=1"
                    "search-get" -> "/search?q=prefix&limit=1"
                    "suggestions" -> "/search/suggestions?prefix=prefix&limit=1"
                    else -> "/search/by-metadata?key=kind&limit=1"
                },
            ) { bearerAuth("owner") }
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error").jsonObject
        assertEquals("DATABASE_ERROR", error.getValue("code").jsonPrimitive.content)
    }

    @ParameterizedTest(name = "P91-http-auth-first {0}")
    @ValueSource(strings = ["/storage/folder?limit=-1", "/search?q=prefix&limit=-1"])
    fun authenticationStillPrecedesInputHandling(path: String) = testApplication {
        application { f.install(this) }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1$path").status)
        coVerify(exactly = 0) {
            f.repository.query(any())
            f.repository.findById(any())
        }
    }

    @Test fun shortSuggestionPrefixWithValidInputIsStillAnEmptySuccess() = testApplication {
        application { f.install(this) }
        val response = client.get("/api/v1/search/suggestions?prefix=x&limit=1") { bearerAuth("owner") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(0, Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonArray.size)
        coVerify(exactly = 0) { f.repository.query(any()) }
    }

    @Test fun unknownBodyMembersRetainSharedSerializerCompatibility() = testApplication {
        application { f.install(this) }
        val response = client.post("/api/v1/search") {
            bearerAuth("owner")
            contentType(ContentType.Application.Json)
            setBody("{\"query\":\"prefix\",\"limit\":1,\"future\":{\"flag\":true}}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertFalse(response.bodyAsText().contains("future"))
        coVerify(exactly = 1) { f.repository.query(match { it.limit == 1 }) }
    }
}
