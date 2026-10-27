package com.vaultstadio.api.routes.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals

internal abstract class Transfer78HttpBase : Auth119HttpBase() {
    protected suspend fun HttpClient.owner78(): String {
        register()
        return login().text("token")
    }
    protected suspend fun HttpClient.upload78(
        token: String,
        bytes: ByteArray = byteArrayOf(0, 1, 127, -1),
        name: String = "original78.bin",
        parent: String? = null,
        mime: String = "application/octet-stream",
    ): String {
        val result = submitFormWithBinaryData(
            "/api/v1/storage/upload",
            formData {
                append(
                    "file",
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                        append(HttpHeaders.ContentType, mime)
                    },
                )
            },
        ) {
            bearerAuth(token)
            if (parent != null) parameter("parentId", parent)
        }
        assertEquals(HttpStatusCode.Created, result.status)
        return envelope(result).getValue("data").jsonObject.text("id")
    }
    protected suspend fun HttpClient.folder78(token: String, name: String, parent: String? = null): String {
        val result = post("/api/v1/storage/folder") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                JsonObject(
                    mapOf(
                        "name" to JsonPrimitive(name),
                        "parentId" to (parent?.let(::JsonPrimitive) ?: JsonNull),
                    ),
                ).toString(),
            )
        }
        assertEquals(HttpStatusCode.Created, result.status)
        return envelope(result).getValue("data").jsonObject.text("id")
    }
    protected suspend fun HttpClient.zip78(
        token: String?,
        ids: List<String>,
    ) = post("/api/v1/storage/batch/download-zip") {
        if (token != null) bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody("{\"itemIds\":" + Json.encodeToString(ids) + "}")
    }
    protected fun unzip78(
        bytes: ByteArray,
    ): Map<String, ByteArray> = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        val entries = linkedMapOf<String, ByteArray>()
        while (true) {
            val entry = zip.nextEntry ?: break
            check(entry.name !in entries)
            entries[entry.name] = zip.readBytes()
            zip.closeEntry()
        }
        entries
    }
}
