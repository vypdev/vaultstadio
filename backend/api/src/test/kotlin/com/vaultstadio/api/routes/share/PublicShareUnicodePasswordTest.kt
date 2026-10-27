package com.vaultstadio.api.routes.share

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URLEncoder
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareUnicodePasswordTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "S90-actual-UTF8-password {0}")
    @ValueSource(strings = ["snowman", "emoji", "spaces", "latin-max"])
    fun utf8PasswordWorksWithRealBCryptHeaderAndNativeForm(case: String) = testApplication {
        application { f.install(this) }
        startApplication()
        val password = when (case) {
            "snowman" -> "snow ☃"
            "emoji" -> "lock 🔐"
            "spaces" -> " +% password "
            else -> "é".repeat(36)
        }
        f.update(f.share.copy(password = f.hasher.hash(password)))
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(password.toByteArray())
        val info = client.get("/api/v1/share/${f.share.token}") { header("X-Share-Password", encoded) }
        assertEquals(HttpStatusCode.OK, info.status)
        assertEquals(0, f.count())
        val download = client.get("/api/v1/share/${f.share.token}/download") { header("X-Share-Password", encoded) }
        assertEquals(HttpStatusCode.OK, download.status)
        assertContentEquals("shared bytes ☃\n".toByteArray(), download.bodyAsBytes())
        val form = client.post("/api/v1/share/${f.share.token}/view/download") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("password=" + URLEncoder.encode(password, Charsets.UTF_8))
        }
        assertEquals(HttpStatusCode.OK, form.status)
        assertContentEquals("shared bytes ☃\n".toByteArray(), form.bodyAsBytes())
        assertEquals(2, f.count())
    }
}
