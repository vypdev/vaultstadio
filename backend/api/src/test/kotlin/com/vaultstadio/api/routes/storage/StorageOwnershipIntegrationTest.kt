package com.vaultstadio.api.routes.storage

import arrow.core.Either
import com.vaultstadio.domain.storage.model.Visibility
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageOwnershipIntegrationTest {
    private val f = StorageIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @BeforeEach fun reset() = f.reset()

    @TestFactory
    fun `foreign folder writes leave rows and bytes unchanged`() = Visibility.entries.flatMap { visibility ->
        listOf("folder", "upload", "upload-folder").map { endpoint ->
            DynamicTest.dynamicTest("$endpoint denies foreign $visibility parent") {
                // Dynamic children do not receive BeforeEach individually.
                f.reset()
                val parent = runBlocking { f.seed(visibility = visibility) }
                testApplication {
                    application { f.install(this) }
                    val response = client.post("/api/v1/storage/$endpoint?parentId=${parent.id}") {
                        bearerAuth("bob-test-session")
                        if (endpoint == "folder") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"name":"child","parentId":"${parent.id}"}""")
                        } else {
                            setBody(
                                MultiPartFormDataContent(
                                    formData {
                                        append("parentId", parent.id)
                                        append(
                                            if (endpoint ==
                                                "upload-folder"
                                            ) {
                                                "nested/probe.txt"
                                            } else {
                                                "file"
                                            },
                                            byteArrayOf(1, 2, 3),
                                            Headers.build {
                                                append(HttpHeaders.ContentType, "text/plain")
                                                append(HttpHeaders.ContentDisposition, "filename=probe.txt")
                                            },
                                        )
                                    },
                                ),
                            )
                        }
                    }
                    val body = response.bodyAsText()
                    if (endpoint == "upload-folder") {
                        assertEquals(HttpStatusCode.OK, response.status)
                        val data = Json.parseToJsonElement(body).jsonObject.getValue("data").jsonObject
                        assertEquals("0", data.getValue("uploadedFiles").jsonPrimitive.content)
                        assertEquals("0", data.getValue("createdFolders").jsonPrimitive.content)
                        assertFalse(data.getValue("errors").toString() == "[]")
                    } else {
                        assertEquals(HttpStatusCode.Forbidden, response.status, body)
                        assertEquals(
                            "ACCESS_DENIED",
                            Json.parseToJsonElement(
                                body,
                            ).jsonObject.getValue("error").jsonObject.getValue("code").jsonPrimitive.content,
                        )
                    }
                    assertEquals(1, f.rowCount())
                    assertEquals(0, f.byteFileCount())
                    assertEquals(parent, assertIs<Either.Right<*>>(f.repository.findById(parent.id)).value)
                    coVerify(exactly = 0) { f.events.publish(any()) }
                }
            }
        }
    }

    @Test
    fun `owner can create a folder upload and download exact bytes`() = testApplication {
        val parent = f.seed()
        application { f.install(this) }
        val create = client.post("/api/v1/storage/folder") {
            bearerAuth("alice-test-session")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"child","parentId":"${parent.id}"}""")
        }
        assertEquals(HttpStatusCode.Created, create.status, create.bodyAsText())
        val childId = Json.parseToJsonElement(
            create.bodyAsText(),
        ).jsonObject.getValue("data").jsonObject.getValue("id").jsonPrimitive.content
        val payload = "Owned byte-for-byte round trip"
        val upload = client.post("/api/v1/storage/upload?parentId=$childId") {
            bearerAuth("alice-test-session")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            payload.toByteArray(),
                            Headers.build {
                                append(HttpHeaders.ContentType, "text/plain")
                                append(HttpHeaders.ContentDisposition, "filename=probe.txt")
                            },
                        )
                    },
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, upload.status, upload.bodyAsText())
        val fileId = Json.parseToJsonElement(
            upload.bodyAsText(),
        ).jsonObject.getValue("data").jsonObject.getValue("id").jsonPrimitive.content
        val download = client.get("/api/v1/storage/download/$fileId") { bearerAuth("alice-test-session") }
        assertEquals(HttpStatusCode.OK, download.status)
        assertEquals(payload, download.bodyAsText())
        assertEquals(3, f.rowCount())
        assertEquals(1, f.byteFileCount())
        val forbiddenDownload = client.get("/api/v1/storage/download/$fileId") { bearerAuth("bob-test-session") }
        assertEquals(HttpStatusCode.Forbidden, forbiddenDownload.status)
    }

    @Test
    fun `missing authentication never reaches storage effects`() = testApplication {
        application { f.install(this) }
        val response = client.post("/api/v1/storage/folder") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"child"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(0, f.rowCount())
        assertEquals(0, f.byteFileCount())
    }
}
