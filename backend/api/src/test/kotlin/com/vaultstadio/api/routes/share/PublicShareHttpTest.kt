package com.vaultstadio.api.routes.share

import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareHttpTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @Test
    fun `S90 repeated anonymous metadata is typed minimal and has zero download effects`() = testApplication {
        application { f.install(this) }
        startApplication()
        f.update(f.share.copy(maxDownloads = 1))
        repeat(3) {
            val response = client.get("/api/v1/share/${f.share.token}")
            assertEquals(HttpStatusCode.OK, response.status)
            val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
            assertEquals(setOf("name", "size", "mimeType", "hasPassword", "expiresAt"), data.keys)
            assertEquals(f.item.name, data.getValue("name").jsonPrimitive.content)
            assertEquals(f.item.size.toString(), data.getValue("size").jsonPrimitive.content)
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", response.headers["Referrer-Policy"])
            assertFalse(response.bodyAsText().contains(f.item.storageKey!!))
        }
        assertEquals(0, f.count())
    }

    @ParameterizedTest(name = "S90-HTTP-password {0}")
    @ValueSource(
        strings = [
            "info-missing",
            "info-wrong",
            "info-correct",
            "download-missing",
            "download-wrong",
            "download-correct",
            "form-missing",
            "form-wrong",
            "form-correct",
        ],
    )
    fun passwordJourney(case: String) = testApplication {
        application { f.install(this) }
        startApplication()
        f.update(f.share.copy(password = f.hasher.hash("correct")))
        val password = when {
            case.endsWith("correct") -> "correct"
            case.endsWith("wrong") -> "wrong"
            else -> null
        }
        val form = case.startsWith("form")
        val info = case.startsWith("info")
        val response = passwordRequest(client, form, info, password)
        val allowed = password == "correct"
        assertEquals(if (allowed) HttpStatusCode.OK else HttpStatusCode.Forbidden, response.status)
        assertEquals(if (allowed && !info) 1 else 0, f.count())
        if (allowed && !info) assertContentEquals("shared bytes ☃\n".toByteArray(), response.bodyAsBytes())
        if (!allowed) {
            val body = response.bodyAsText()
            assertFalse(body.contains(f.share.password!!))
            assertFalse(body.contains(f.item.storageKey!!))
        }
    }

    @ParameterizedTest(name = "S90-HTTP-denial {0}")
    @ValueSource(
        strings = [
            "invalid",
            "case",
            "revoked",
            "expired",
            "equal",
            "limit",
            "trash",
            "owner",
            "folder",
            "key",
            "missing-item",
        ],
    )
    fun deniedLinksHaveNoCounterEffects(case: String) = testApplication {
        application { f.install(this) }
        startApplication()
        var token = f.share.token
        when (case) {
            "invalid" -> token = "missing"
            "case" -> token = token.lowercase()
            "revoked" -> f.update(f.share.copy(isActive = false))
            "expired" -> f.update(f.share.copy(expiresAt = Instant.fromEpochSeconds(f.now.epochSeconds - 1)))
            "equal" -> f.update(f.share.copy(expiresAt = f.now))
            "limit" -> f.update(f.share.copy(maxDownloads = 1)).also {
                client.get("/api/v1/share/$token/download").bodyAsBytes()
            }
            "trash" -> f.storage.repository.update(f.item.copy(isTrashed = true)).fold({ throw it }, {})
            "owner" -> transaction {
                StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) {
                    it[ownerId] =
                        f.storage.bob.id
                }
            }
            "folder" -> f.storage.repository.update(f.item.copy(type = ItemType.FOLDER)).fold({ throw it }, {})
            "key" -> f.storage.repository.update(f.item.copy(storageKey = null)).fold({ throw it }, {})
            "missing-item" -> f.storage.repository.delete(f.item.id).fold({ throw it }, {})
        }
        val before = f.count()
        val expected = if (case in
            listOf("revoked", "expired", "equal", "limit")
        ) {
            HttpStatusCode.Gone
        } else {
            HttpStatusCode.NotFound
        }
        for (suffix in listOf("", "/download")) {
            val response = client.get("/api/v1/share/$token$suffix")
            assertEquals(expected, response.status)
            assertEquals(before, f.count())
        }
    }

    @ParameterizedTest(name = "S90-HTTP-query-secret {0}")
    @ValueSource(strings = ["", "/download", "/view"])
    fun passwordQueryIsRejectedWithoutAccess(suffix: String) = testApplication {
        application { f.install(this) }
        startApplication()
        val response = client.get("/api/v1/share/${f.share.token}$suffix?password=DoNotEcho")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(response.bodyAsText().contains("DoNotEcho"))
        assertEquals(0, f.count())
    }

    @ParameterizedTest(name = "S90-HTTP-page {0}")
    @ValueSource(strings = ["view", "js", "css"])
    fun publicPageAndAssetsNeedNoLogin(case: String) = testApplication {
        application { f.install(this) }
        val path = when (case) {
            "view" -> "/api/v1/share/${f.share.token}/view"
            "js" -> "/api/v1/share-assets/public-share.mjs"
            else -> "/api/v1/share-assets/public-share.css"
        }
        val response = client.get(path)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        if (case == "view") {
            assertTrue(response.bodyAsText().contains("for=\"password\""))
            assertTrue(response.bodyAsText().contains("role=\"status\""))
            assertTrue(response.headers["Content-Security-Policy"]!!.contains("frame-ancestors 'none'"))
        }
    }

    @ParameterizedTest(name = "S90-HTTP-form-boundary {0}")
    @ValueSource(strings = ["duplicate", "oversize", "json"])
    fun malformedFormsAreBoundedAndCannotDownload(case: String) = testApplication {
        application { f.install(this) }
        val response = client.post("/api/v1/share/${f.share.token}/view/download") {
            contentType(if (case == "json") ContentType.Application.Json else ContentType.Application.FormUrlEncoded)
            setBody(
                when (case) {
                    "duplicate" -> "password=a&password=b"
                    "oversize" -> "password=" + "a".repeat(3000)
                    else -> "{}"
                },
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(0, f.count())
    }

    @Test
    fun `S90 public capability does not authenticate owner management`() = testApplication {
        application { f.install(this) }
        val response = client.get("/api/v1/shares") { bearerAuth(f.share.token) }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(0, f.count())
    }
    private suspend fun passwordRequest(
        client: HttpClient,
        form: Boolean,
        info: Boolean,
        password: String?,
    ): HttpResponse {
        return if (form) {
            client.post("/api/v1/share/${f.share.token}/view/download") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(password?.let { "password=$it" }.orEmpty())
            }
        } else {
            client.get("/api/v1/share/${f.share.token}" + if (info) "" else "/download") {
                password?.let {
                    header("X-Share-Password", Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray()))
                }
            }
        }
    }
}
