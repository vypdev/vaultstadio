package com.vaultstadio.api.routes.version

import arrow.core.Either
import com.vaultstadio.domain.storage.model.Visibility
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionOwnershipHttpTest {
    private val f = VersionIntegrationFixture()
    private val surfaces =
        listOf("history", "detail", "download-metadata", "restore", "compare", "delete", "retention-cleanup")

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @TestFactory
    fun `all authenticated version surfaces enforce current ownership`() = surfaces.flatMap { surface ->
        Visibility.entries.flatMap { visibility ->
            f.actors.keys.map { actor ->
                DynamicTest.dynamicTest("VER74-HTTP-$surface-$visibility-$actor") {
                    f.reset(visibility)
                    val before = f.snapshot()
                    testApplication {
                        application { f.install(this) }
                        val sqlStart = f.storage.postgres.logs.length
                        val response = client.versionRequest(surface, actor)
                        val sql = f.storage.postgres.logs.drop(sqlStart)
                        verifyAuthenticated(surface, actor, response, sql, before)
                    }
                }
            }
        }
    }

    @TestFactory
    fun `anonymous requests stop before resource work`() = surfaces.map { surface ->
        DynamicTest.dynamicTest("VER74-HTTP-$surface-anonymous") {
            f.reset()
            val before = f.snapshot()
            testApplication {
                application { f.install(this) }
                val response = client.versionRequest(surface, null)
                assertEquals(HttpStatusCode.Unauthorized, response.status, response.bodyAsText())
                assertEquals(before, f.snapshot())
            }
        }
    }

    private suspend fun HttpClient.versionRequest(surface: String, actor: String?): HttpResponse {
        val (method, path, body) = requestFor(surface)
        return request("/api/v1/versions$path") {
            this.method = method
            if (actor != null) bearerAuth(actor)
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    private suspend fun verifyAuthenticated(
        surface: String,
        actor: String,
        response: HttpResponse,
        sql: String,
        before: List<Any>,
    ) {
        val text = response.bodyAsText()
        if (actor == "owner") {
            val expected = if (surface == "delete") HttpStatusCode.NoContent else HttpStatusCode.OK
            assertEquals(expected, response.status, text)
            verifyOwner(surface, text)
        } else {
            assertRejected(surface, response.status, text, sql, before)
        }
    }

    private fun assertRejected(surface: String, status: HttpStatusCode, text: String, sql: String, before: List<Any>) {
        assertEquals(HttpStatusCode.Forbidden, status, text)
        assertEquals(before, f.snapshot())
        assertFalse(text.contains(f.item.name))
        assertFalse(text.contains(f.old.storageKey))
        assertFalse(text.contains(f.latest.storageKey))
        assertTrue(text.contains("ACCESS_DENIED"))
        val versionReads = sql.lineSequence().filter {
            it.contains("SELECT", true) &&
                it.contains("file_versions", true)
        }.toList()
        if (surface == "delete") {
            assertTrue(versionReads.isNotEmpty(), "Expected the minimal SQL scope lookup")
            assertTrue(
                versionReads.all {
                    it.contains("SELECT file_versions.item_id FROM file_versions", true)
                },
                versionReads.joinToString("\n"),
            )
        } else {
            assertTrue(versionReads.isEmpty(), versionReads.joinToString("\n"))
        }
    }

    private fun requestFor(surface: String): Triple<HttpMethod, String, String?> = when (surface) {
        "history" -> Triple(HttpMethod.Get, "/item/${f.item.id}", null)
        "detail" -> Triple(HttpMethod.Get, "/item/${f.item.id}/version/1", null)
        "download-metadata" -> Triple(HttpMethod.Get, "/item/${f.item.id}/version/1/download", null)
        "restore" -> Triple(HttpMethod.Post, "/item/${f.item.id}/restore", """{"versionNumber":1}""")
        "compare" -> Triple(HttpMethod.Get, "/item/${f.item.id}/diff?from=1&to=2", null)
        "delete" -> Triple(HttpMethod.Delete, "/${f.old.id}", null)
        "retention-cleanup" -> Triple(
            HttpMethod.Post,
            "/item/${f.item.id}/cleanup",
            """{"maxVersions":1,"minVersionsToKeep":1}""",
        )
        else -> error("Unknown surface $surface")
    }

    private suspend fun verifyOwner(surface: String, text: String) {
        if (surface == "delete") {
            assertEquals(null, assertIs<Either.Right<*>>(f.repository.findById(f.old.id)).value)
            assertEquals(false, assertIs<Either.Right<*>>(f.storage.backend.exists(f.old.storageKey)).value)
            assertEquals(true, assertIs<Either.Right<*>>(f.storage.backend.exists(f.latest.storageKey)).value)
            return
        }
        val json = Json.parseToJsonElement(text).jsonObject
        when (surface) {
            "history" -> {
                assertEquals(f.item.name, json.getValue("itemName").jsonPrimitive.content)
                assertEquals(2, json.getValue("versions").jsonArray.size)
            }
            "detail" -> {
                assertEquals(f.old.checksum, json.getValue("checksum").jsonPrimitive.content)
            }
            "download-metadata" -> {
                assertEquals(f.old.storageKey, json.getValue("storageKey").jsonPrimitive.content)
                val stream = f.storage.backend.retrieve(f.old.storageKey).fold({ throw it }, { it })
                assertEquals("old\n", stream.bufferedReader().use { it.readText() })
            }
            "restore" -> {
                assertEquals("1", json.getValue("newVersion").jsonObject.getValue("restoredFrom").jsonPrimitive.content)
                val row = f.repository.findByItemAndVersion(f.item.id, 3).fold({ throw it }, { checkNotNull(it) })
                assertEquals(1, row.restoredFrom)
                assertEquals(f.storage.alice.id, row.createdBy)
            }
            "compare" -> {
                assertEquals("false", json.getValue("isBinary").jsonPrimitive.content)
                assertTrue(json.getValue("patches").jsonArray.isNotEmpty())
            }
            "retention-cleanup" -> {
                assertEquals(
                    listOf(f.old.id),
                    json.getValue("deletedVersions").jsonArray.map {
                        it.jsonPrimitive.content
                    },
                )
                assertEquals(null, assertIs<Either.Right<*>>(f.repository.findById(f.old.id)).value)
                assertEquals(false, assertIs<Either.Right<*>>(f.storage.backend.exists(f.old.storageKey)).value)
                assertEquals(true, assertIs<Either.Right<*>>(f.storage.backend.exists(f.latest.storageKey)).value)
            }
        }
    }
}
