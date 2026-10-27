package com.vaultstadio.api.routes.share

import com.vaultstadio.domain.share.model.ShareLink
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareCreationTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "S90-create-actual-BCrypt-input {0}")
    @ValueSource(
        strings = [
            "default", "no-expiry", "max-days", "zero-days", "negative-days", "over-days",
            "zero-limit", "negative-limit", "empty-password", "72-utf8", "74-utf8", "foreign",
        ],
    )
    fun ownerCreationIsBoundedAndGeneratedLinkIsUsable(case: String) = testApplication {
        application { f.install(this) }
        val body = creationBody(case)
        val before = f.shares.findByItemId(f.item.id).getOrNull()!!
        val response = client.post("/api/v1/shares") {
            bearerAuth(if (case == "foreign") "unrelated" else "owner")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val allowed = case in listOf("default", "no-expiry", "max-days", "72-utf8")
        val expected = when {
            allowed -> HttpStatusCode.Created
            case == "foreign" -> HttpStatusCode.Forbidden
            else -> HttpStatusCode.BadRequest
        }
        assertEquals(expected, response.status)
        val after = f.shares.findByItemId(f.item.id).getOrNull()!!
        if (allowed) {
            assertEquals(before.size + 1, after.size)
            assertCreated(case, response.bodyAsText(), after)
        } else {
            assertEquals(before, after)
        }
    }
    private fun assertCreated(case: String, responseBody: String, after: List<ShareLink>) {
        val data = Json.parseToJsonElement(responseBody).jsonObject.getValue("data").jsonObject
        val token = data.getValue("token").jsonPrimitive.content
        assertTrue(data.getValue("url").jsonPrimitive.content.endsWith("/api/v1/share/$token/view"))
        val created = after.single { it.token == token }
        assertEquals(0, created.downloadCount)
        if (case == "no-expiry") assertEquals(null, created.expiresAt)
        if (case == "72-utf8") {
            assertTrue(f.hasher.verify("é".repeat(36), created.password!!))
            assertFalse(responseBody.contains(created.password!!))
        }
    }

    private fun creationBody(case: String): JsonObject {
        return buildJsonObject {
            put("itemId", f.item.id)
            put("unknownField", "ignored")
            when (case) {
                "no-expiry" -> put("expirationDays", JsonNull)
                "max-days" -> put("expirationDays", 365)
                "zero-days" -> put("expirationDays", 0)
                "negative-days" -> put("expirationDays", -1)
                "over-days" -> put("expirationDays", 366)
                "zero-limit" -> put("maxDownloads", 0)
                "negative-limit" -> put("maxDownloads", -1)
                "empty-password" -> put("password", "")
                "72-utf8" -> put("password", "é".repeat(36))
                "74-utf8" -> put("password", "é".repeat(37))
            }
        }
    }
}
