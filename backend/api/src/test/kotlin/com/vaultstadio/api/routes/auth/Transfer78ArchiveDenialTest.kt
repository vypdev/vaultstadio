package com.vaultstadio.api.routes.auth

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class Transfer78ArchiveDenialTest : Auth119HttpBase() {
    @Test fun `TRANS78-ARCH03 foreign selection cannot become a successful empty ZIP`() = app {
        client.register()
        val owner = client.login().text("token")
        val upload = client.submitFormWithBinaryData(
            "/api/v1/storage/upload",
            formData {
                append(
                    "file",
                    byteArrayOf(0, 1, -1),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"private78.bin\"")
                        append(HttpHeaders.ContentType, "application/octet-stream")
                    },
                )
            },
        ) { bearerAuth(owner) }
        assertEquals(HttpStatusCode.Created, upload.status)
        val id = envelope(upload).getValue("data").jsonObject.text("id")
        val hashes = fixture.contentHashes()
        client.register(AUTH119_REGISTER.replace("owner119", "foreign78"))
        val foreign = client.login("foreign78@audit.invalid").text("token")
        val result = client.post("/api/v1/storage/batch/download-zip") {
            bearerAuth(foreign)
            contentType(ContentType.Application.Json)
            setBody("""{"itemIds":["$id"]}""")
        }
        assertSafeFailure(result, HttpStatusCode.Forbidden)
        assertEquals(hashes, fixture.contentHashes())
        assertEquals(1, fixture.storageCount())
    }
}
