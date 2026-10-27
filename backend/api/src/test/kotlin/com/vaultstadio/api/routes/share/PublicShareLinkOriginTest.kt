package com.vaultstadio.api.routes.share

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareLinkOriginTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "S90-generated-origin {0}")
    @ValueSource(strings = ["127.0.0.1:18086", "api.test:8443", "[::1]:18086", "bad/path", "user:secret@api.test"])
    fun originIsValidatedBeforeMutationAndPublicPortIsRetained(authority: String) = testApplication {
        application { f.install(this) }
        val before = f.shares.findByItemId(f.item.id).getOrNull()!!
        val response = client.post("/api/v1/shares") {
            bearerAuth("owner")
            header("X-Forwarded-Proto", "https")
            header("X-Forwarded-Host", authority)
            contentType(ContentType.Application.Json)
            setBody("{\"itemId\":\"${f.item.id}\"}")
        }
        val valid = authority in listOf("127.0.0.1:18086", "api.test:8443", "[::1]:18086")
        assertEquals(if (valid) HttpStatusCode.Created else HttpStatusCode.BadRequest, response.status)
        if (valid) {
            val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
            val uri = URI.create(data.getValue("url").jsonPrimitive.content)
            assertEquals("https", uri.scheme)
            assertEquals(authority, uri.rawAuthority)
            assertEquals(HttpStatusCode.OK, client.get(uri.rawPath).status)
        } else {
            assertEquals(before, f.shares.findByItemId(f.item.id).getOrNull()!!)
        }
    }
}
