package com.vaultstadio.api.routes.storage

import arrow.core.Either
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.Visibility
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageItemAuthorizationIntegrationTest {
    private val f = StorageIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @TestFactory
    fun `foreign sources remain forbidden through production routes`() = Visibility.entries.flatMap { visibility ->
        listOf(
            "read",
            "download",
            "list",
            "breadcrumbs",
            "rename",
            "move",
            "copy",
            "star",
            "trash",
            "restore",
            "delete",
        ).map { operation ->
            DynamicTest.dynamicTest("HTTP $operation denies foreign $visibility source") {
                f.reset()
                val source = runBlocking { f.seed(visibility = visibility) }
                val (method, path, body) = sourceRequest(operation, source.id)
                testApplication {
                    application { f.install(this) }
                    val response = client.request("/api/v1/storage/$path") {
                        this.method = method
                        bearerAuth("bob-test-session")
                        if (body != null) {
                            contentType(ContentType.Application.Json)
                            setBody(body)
                        }
                    }
                    assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
                    assertEquals(source, assertIs<Either.Right<*>>(f.repository.findById(source.id)).value)
                    assertEquals(1, f.rowCount())
                    assertEquals(0, f.byteFileCount())
                    coVerify(exactly = 0) { f.events.publish(any()) }
                }
            }
        }
    }

    @TestFactory
    fun `foreign destinations cannot change owned sources`() = Visibility.entries.flatMap { visibility ->
        listOf("move", "copy", "restore").map { operation ->
            DynamicTest.dynamicTest("HTTP $operation denies foreign $visibility destination") {
                f.reset()
                val parent = runBlocking { f.seed(owner = f.bob, visibility = visibility) }
                val source = runBlocking {
                    f.seed(type = ItemType.FILE, parentId = if (operation == "restore") parent.id else null)
                }
                val path = "item/${source.id}/$operation"
                testApplication {
                    application { f.install(this) }
                    val response = client.request("/api/v1/storage/$path") {
                        method = HttpMethod.Post
                        bearerAuth("alice-test-session")
                        if (operation != "restore") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"destinationId":"${parent.id}"}""")
                        }
                    }
                    assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
                    assertEquals(source, assertIs<Either.Right<*>>(f.repository.findById(source.id)).value)
                    assertEquals(2, f.rowCount())
                    assertEquals(0, f.byteFileCount())
                    coVerify(exactly = 0) { f.events.publish(any()) }
                }
            }
        }
    }

    private fun sourceRequest(operation: String, id: String): Triple<HttpMethod, String, String?> = when (operation) {
        "read" -> Triple(HttpMethod.Get, "item/$id", null)
        "download" -> Triple(HttpMethod.Get, "download/$id", null)
        "list" -> Triple(HttpMethod.Get, "folder/$id", null)
        "breadcrumbs" -> Triple(HttpMethod.Get, "item/$id/breadcrumbs", null)
        "rename" -> Triple(HttpMethod.Patch, "item/$id/rename", """{"name":"renamed"}""")
        "move", "copy" -> Triple(HttpMethod.Post, "item/$id/$operation", """{"destinationId":null}""")
        "trash" -> Triple(HttpMethod.Delete, "item/$id", null)
        "delete" -> Triple(HttpMethod.Delete, "item/$id?permanent=true", null)
        else -> Triple(HttpMethod.Post, "item/$id/$operation", null)
    }
}
