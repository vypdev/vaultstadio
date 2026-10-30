package com.vaultstadio.api.routes.auth

import com.vaultstadio.domain.auth.model.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** STO119-17/20: authenticated production file routes, SQL and actual provider bytes. */
internal class Auth119FileJourneyTest : Auth119HttpBase() {
    private val bytes = byteArrayOf(0, 1, 127, -1)
    private suspend fun HttpClient.owner(): String {
        register()
        return login().text("token")
    }
    private suspend fun HttpClient.upload(token: String): String {
        val response = submitFormWithBinaryData(
            "/api/v1/storage/upload",
            formData {
                append(
                    "file",
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"owned119.bin\"")
                        append(HttpHeaders.ContentType, "application/octet-stream")
                    },
                )
            },
        ) { bearerAuth(token) }
        assertEquals(HttpStatusCode.Created, response.status)
        return envelope(response).getValue("data").jsonObject.text("id")
    }

    @Test fun `STO119-17 owner lifecycle returns and preserves actual bytes through rename trash and restore`() = app {
        val token = client.owner()
        val id = client.upload(token)
        assertContentEquals(bytes, client.get("/api/v1/storage/download/$id") { bearerAuth(token) }.body<ByteArray>())
        val renamed = client.patch("/api/v1/storage/item/$id/rename") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"renamed119.bin"}""")
        }
        assertEquals(HttpStatusCode.OK, renamed.status)
        assertEquals("renamed119.bin", fixture.item(id).name)
        val hashes = fixture.contentHashes()
        assertEquals(HttpStatusCode.OK, client.delete("/api/v1/storage/item/$id") { bearerAuth(token) }.status)
        assertTrue(fixture.item(id).isTrashed)
        assertEquals(hashes, fixture.contentHashes())
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/storage/item/$id/restore") { bearerAuth(token) }.status)
        assertFalse(fixture.item(id).isTrashed)
        assertContentEquals(bytes, client.get("/api/v1/storage/download/$id") { bearerAuth(token) }.body<ByteArray>())
        assertEquals(
            HttpStatusCode.OK,
            client.delete("/api/v1/storage/item/$id?permanent=true") {
                bearerAuth(token)
            }.status,
        )
        assertEquals(0, fixture.storageCount())
        assertTrue(fixture.contentHashes().isEmpty())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `STO119-18 real nonowner credentials and nonowner administrator have zero storage effects`(
        admin: Boolean,
    ) = app {
        val owner = client.owner()
        val id = client.upload(owner)
        client.register(AUTH119_REGISTER.replace("owner119", "other119"))
        val other = fixture.user("other119@audit.invalid")
        if (admin) fixture.users.update(other.copy(role = UserRole.ADMIN)).fold({ throw it }, {})
        val foreign = client.login("other119@audit.invalid").text("token")
        val before = fixture.item(id)
        val hashes = fixture.contentHashes()
        val responses = listOf(
            client.get("/api/v1/storage/item/$id") { bearerAuth(foreign) },
            client.get("/api/v1/storage/download/$id") { bearerAuth(foreign) },
            client.patch("/api/v1/storage/item/$id/rename") {
                bearerAuth(foreign)
                contentType(ContentType.Application.Json)
                setBody("""{"name":"forbidden.bin"}""")
            },
            client.delete("/api/v1/storage/item/$id") { bearerAuth(foreign) },
            client.delete("/api/v1/storage/item/$id?permanent=true") { bearerAuth(foreign) },
        )
        for (response in responses) assertSafeFailure(response, HttpStatusCode.Forbidden)
        assertEquals(before, fixture.item(id))
        assertEquals(hashes, fixture.contentHashes())
        assertEquals(1, fixture.storageCount())
    }

    @Test fun `STO119-19 anonymous requests cannot create folders or bytes`() = app {
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.post("/api/v1/storage/folder") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"forbidden"}""")
            }.status,
        )
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/storage/folder").status)
        assertEquals(0, fixture.storageCount())
        assertTrue(fixture.contentHashes().isEmpty())
    }

    @Test fun `STO119-19 missing resource and malformed mutation retain rows and bytes`() = app {
        val token = client.owner()
        val id = client.upload(token)
        val before = fixture.item(id)
        val hashes = fixture.contentHashes()
        assertSafeFailure(
            client.get("/api/v1/storage/item/owned-missing119") {
                bearerAuth(token)
            },
            HttpStatusCode.NotFound,
        )
        assertSafeFailure(
            client.patch("/api/v1/storage/item/$id/rename") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("{}")
            },
            HttpStatusCode.BadRequest,
        )
        assertEquals(before, fixture.item(id))
        assertEquals(hashes, fixture.contentHashes())
    }

    @Test fun `STO119-20 actual SQL failure cannot become an empty listing or created folder`() = app {
        val token = client.owner()
        fixture.storageUnavailable {
            assertSafeFailure(
                client.get("/api/v1/storage/folder") {
                    bearerAuth(token)
                },
                HttpStatusCode.InternalServerError,
            )
            assertSafeFailure(
                client.post("/api/v1/storage/folder") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"not-created"}""")
                },
                HttpStatusCode.InternalServerError,
            )
        }
        assertEquals(0, fixture.storageCount())
        assertTrue(fixture.contentHashes().isEmpty())
    }
}
