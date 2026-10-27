package com.vaultstadio.api.routes.auth

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

internal class Transfer78ArchiveHttpTest : Transfer78HttpBase() {
    @Test fun `TRANS78-ARCH01 ZIP preserves every selected file byte without source changes`() = app {
        val owner = client.owner78()
        val first = client.upload78(owner)
        val utf8 = "Archivo UTF-8: ñ 日本語\n".encodeToByteArray()
        val second = client.upload78(owner, utf8, "Archivo ñ.txt", mime = "text/plain")
        val before = fixture.contentHashes()
        val response = client.zip78(owner, listOf(first, second))
        assertEquals(HttpStatusCode.OK, response.status)
        val entries = unzip78(response.body())
        assertEquals(setOf("original78.bin", "Archivo ñ.txt"), entries.keys)
        assertContentEquals(byteArrayOf(0, 1, 127, -1), entries["original78.bin"])
        assertContentEquals(utf8, entries["Archivo ñ.txt"])
        assertEquals(before, fixture.contentHashes())
        assertEquals(2, fixture.storageCount())
    }

    @Test fun `TRANS78-ARCH02 nested and empty folders retain their relative ZIP paths`() = app {
        val owner = client.owner78()
        val root = client.folder78(owner, "Carpeta ñ")
        val nested = client.folder78(owner, "nested", root)
        client.folder78(owner, "empty", root)
        client.upload78(owner, byteArrayOf(), "empty.txt", nested)
        val response = client.zip78(owner, listOf(root))
        assertEquals(HttpStatusCode.OK, response.status)
        val entries = unzip78(response.body())
        assertEquals(
            setOf("Carpeta ñ/", "Carpeta ñ/nested/", "Carpeta ñ/nested/empty.txt", "Carpeta ñ/empty/"),
            entries.keys,
        )
        assertContentEquals(byteArrayOf(), entries["Carpeta ñ/nested/empty.txt"])
    }

    @Test fun `TRANS78-ARCH05 missing selected item fails without attachment bytes`() = app {
        val owner = client.owner78()
        val id = client.upload78(owner)
        val before = fixture.contentHashes()
        val response = client.zip78(owner, listOf(id, "missing78"))
        assertSafeFailure(response, HttpStatusCode.NotFound)
        assertNull(response.headers[HttpHeaders.ContentDisposition])
        assertEquals(before, fixture.contentHashes())
    }

    @Test fun `TRANS78-ARCH06 empty selection fails before archive creation`() = app {
        val response = client.zip78(client.owner78(), emptyList())
        assertSafeFailure(response, HttpStatusCode.BadRequest)
        assertNull(response.headers[HttpHeaders.ContentDisposition])
    }

    @Test fun `TRANS78-ARCH06 duplicate selected IDs are rejected before content`() = app {
        val owner = client.owner78()
        val id = client.upload78(owner)
        val before = fixture.contentHashes()
        assertSafeFailure(client.zip78(owner, listOf(id, id)), HttpStatusCode.BadRequest)
        assertEquals(before, fixture.contentHashes())
    }

    @Test fun `TRANS78-ARCH06 parent plus descendant cannot create overlapping entries`() = app {
        val owner = client.owner78()
        val folder = client.folder78(owner, "root78")
        val file = client.upload78(owner, parent = folder)
        assertSafeFailure(client.zip78(owner, listOf(folder, file)), HttpStatusCode.BadRequest)
    }

    @Test fun `TRANS78-ARCH07 conflicting names from different folders cannot be silently skipped`() = app {
        val owner = client.owner78()
        val folder = client.folder78(owner, "root78")
        val first = client.upload78(owner)
        val second = client.upload78(owner, parent = folder)
        assertSafeFailure(client.zip78(owner, listOf(first, second)), HttpStatusCode.BadRequest)
    }

    @Test fun `TRANS78-ARCH07 untrusted path segment cannot become an extraction path`() = app {
        val owner = client.owner78()
        val id = client.upload78(owner)
        fixture.items.update(fixture.item(id).copy(name = "../escape.bin")).fold({ throw it }, {})
        val before = fixture.contentHashes()
        assertSafeFailure(client.zip78(owner, listOf(id)), HttpStatusCode.BadRequest)
        assertEquals(before, fixture.contentHashes())
    }

    @Test fun `TRANS78-ARCH05 actual database failure cannot become an empty ZIP`() = app {
        val owner = client.owner78()
        fixture.storageUnavailable {
            val response = client.zip78(owner, listOf("lookup78"))
            assertSafeFailure(response, HttpStatusCode.InternalServerError)
            assertNull(response.headers[HttpHeaders.ContentDisposition])
        }
        assertEquals(0, fixture.storageCount())
    }

    @Test fun `TRANS78-HTTP03 archive without credentials is unauthorized`() = app {
        assertEquals(HttpStatusCode.Unauthorized, client.zip78(null, listOf("private78")).status)
        assertEquals(0, fixture.storageCount())
    }

    @Test fun `TRANS78-HTTP malformed archive JSON has safe400 and no attachment`() = app {
        val owner = client.owner78()
        val response = client.post("/api/v1/storage/batch/download-zip") {
            bearerAuth(owner)
            contentType(ContentType.Application.Json)
            setBody("{invalid")
        }
        assertSafeFailure(response, HttpStatusCode.BadRequest)
        assertNull(response.headers[HttpHeaders.ContentDisposition])
        assertFalse(response.headers[HttpHeaders.ContentType].orEmpty().contains("zip"))
    }
}
