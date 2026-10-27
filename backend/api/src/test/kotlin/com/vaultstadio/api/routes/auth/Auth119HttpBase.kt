package com.vaultstadio.api.routes.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal const val AUTH119_PASSWORD = "Audit119!Pass2026"
internal const val AUTH119_REGISTER = """{"email":"owner119@audit.invalid","username":"owner119","password":"Audit119!Pass2026"}"""

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class Auth119HttpBase {
    protected val fixture = Auth119Fixture()
    @BeforeAll fun startAuth119Database() = fixture.start()
    @BeforeEach fun resetAuth119Database() = fixture.reset()
    @AfterAll fun closeAuth119Database() = fixture.close()
    protected fun app(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { fixture.install(this) }
        block()
    }
    protected suspend fun HttpClient.send(path: String, body: String, token: String? = null, userAgent: String? = null): HttpResponse = post("/api/v1/auth/$path") {
        contentType(ContentType.Application.Json); setBody(body)
        if (token != null) bearerAuth(token)
        if (userAgent != null) header(HttpHeaders.UserAgent, userAgent)
    }
    protected suspend fun HttpClient.register(body: String = AUTH119_REGISTER): JsonObject {
        val response = send("register", body)
        assertEquals(HttpStatusCode.Created, response.status)
        return envelope(response).getValue("data").jsonObject
    }
    protected suspend fun HttpClient.login(email: String = "owner119@audit.invalid", password: String = AUTH119_PASSWORD): JsonObject {
        val response = send("login", """{"email":"$email","password":"$password"}""")
        assertEquals(HttpStatusCode.OK, response.status)
        return envelope(response).getValue("data").jsonObject
    }
    protected suspend fun HttpClient.me(token: String? = null) = get("/api/v1/auth/me") { if (token != null) bearerAuth(token) }
    protected suspend fun envelope(response: HttpResponse) = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    protected fun JsonObject.text(name: String) = getValue(name).jsonPrimitive.content
    protected suspend fun assertSafeFailure(response: HttpResponse, status: HttpStatusCode) {
        assertEquals(status, response.status, response.bodyAsText())
        val body = response.bodyAsText()
        assertFalse(body.contains(AUTH119_PASSWORD))
        assertFalse(body.contains("passwordHash"))
        assertFalse(body.contains("org.postgresql"))
        assertEquals("false", envelope(response).text("success"))
    }
}
