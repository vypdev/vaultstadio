package com.vaultstadio.api.routes.storage

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectionHttpRejectionTest {
    private val f = SelectionIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "P91-http-invalid-get {0}/{1}")
    @MethodSource("invalidGets")
    fun invalidQueryCannotReachAnySelection(surface: String, query: String) = testApplication {
        application { f.install(this) }
        val separator = if ('?' in surface) '&' else '?'
        rejected(client.get("/api/v1$surface$separator$query") { bearerAuth("owner") })
    }

    @ParameterizedTest(name = "P91-http-invalid-body {0}/{1}")
    @MethodSource("invalidBodies")
    fun actualJsonValuesAreValidatedBeforeCoercionOrPluginChecks(path: String, member: String) = testApplication {
        application { f.install(this) }
        rejected(
            client.post("/api/v1$path") {
                bearerAuth("owner")
                contentType(ContentType.Application.Json)
                setBody("{\"query\":\"prefix\",\"searchContent\":true,$member}")
            },
        )
    }

    @ParameterizedTest(name = "P91-http-invalid-json {0}/{1}")
    @MethodSource("invalidJson")
    fun malformedOrNonobjectJsonIsBounded(path: String, body: String) = testApplication {
        application { f.install(this) }
        rejected(
            client.post("/api/v1$path") {
                bearerAuth("owner")
                contentType(ContentType.Application.Json)
                setBody(body)
            },
        )
    }

    @ParameterizedTest(name = "P91-http-unsupported-control {0}/{1}")
    @CsvSource(
        "/storage/recent,offset",
        "/search/suggestions?prefix=x,offset",
        "/search/by-metadata?key=kind,sortBy",
        "/search?q=prefix,sortOrder",
    )
    fun unsupportedSelectionControlsAreNotIgnored(path: String, field: String) = testApplication {
        application { f.install(this) }
        val separator = if ('?' in path) '&' else '?'
        rejected(client.get("/api/v1$path$separator$field=0") { bearerAuth("owner") })
    }

    private suspend fun rejected(response: HttpResponse) {
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val text = response.bodyAsText()
        val body = Json.parseToJsonElement(text).jsonObject
        assertEquals("VALIDATION_ERROR", body.getValue("error").jsonObject.getValue("code").jsonPrimitive.content)
        assertFalse(text.contains("SensitiveQueryMarker91"))
        coVerify(exactly = 0) {
            f.repository.query(any())
            f.repository.findById(any())
            f.repository.count(any())
            f.metadata.searchByKeyValue(any(), any(), any(), any())
            f.plugins.isPluginEnabled(any())
        }
        verify {
            f.backend wasNot Called
            f.storage.events wasNot Called
            f.metadata wasNot Called
            f.plugins wasNot
                Called
        }
        assertEquals(0L, f.storage.rowCount())
        assertEquals(0L, f.storage.byteFileCount())
    }

    companion object {
        @JvmStatic fun invalidGets(): Stream<Arguments> {
            val surfaces = listOf(
                "/storage/folder",
                "/storage/folder/foreign-parent",
                "/storage/recent",
                "/search?q=prefix",
                "/search/suggestions?prefix=x",
                "/search/by-metadata?key=kind",
            )
            val values = listOf(
                "limit=-1",
                "limit=0",
                "limit=1001",
                "limit=SensitiveQueryMarker91",
                "limit=2147483648",
                "limit=%20",
                "limit=1&limit=1",
                "offset=-1",
            )
            return surfaces.flatMap { s -> values.map { q -> Arguments.of(s, q) } }.stream()
        }

        @JvmStatic fun invalidBodies(): Stream<Arguments> {
            val members = listOf(
                "\"limit\":-1", "\"limit\":0", "\"limit\":1001",
                "\"limit\":\"SensitiveQueryMarker91\"", "\"limit\":null", "\"limit\":2147483648",
                "\"limit\":1.5", "\"limit\":{}", "\"offset\":-1", "\"offset\":\"wrong\"",
                "\"offset\":null", "\"offset\":2147483648",
            )
            return listOf("/search", "/search/advanced").flatMap { path ->
                members.map { Arguments.of(path, it) }
            }.stream()
        }

        @JvmStatic fun invalidJson(): Stream<Arguments> =
            listOf("/search", "/search/advanced").flatMap { path ->
                listOf("{", "[]", "null", "{\"query\":{},\"limit\":1}").map {
                    Arguments.of(path, it)
                }
            }.stream()
    }
}
