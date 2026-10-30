package com.vaultstadio.api.routes.share

import com.vaultstadio.api.config.HttpServerConfig
import com.vaultstadio.api.configureCors
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareBrowserOriginTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "S90-browser-form-origin {0}")
    @ValueSource(strings = ["http://127.0.0.1:18086", "null", "https://foreign.invalid"])
    fun productionCorsAllowsOnlyConfiguredBrowserFormOrigin(origin: String) = testApplication {
        application {
            configureCors(HttpServerConfig("0.0.0.0", 8080, false, listOf("http://127.0.0.1:18086")))
            f.install(this)
        }
        val response = client.post("/api/v1/share/${f.share.token}/view/download") {
            header(HttpHeaders.Origin, origin)
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("password=")
        }
        if (origin == "http://127.0.0.1:18086") {
            assertEquals(HttpStatusCode.OK, response.status)
            assertContentEquals("shared bytes ☃\n".toByteArray(), response.bodyAsBytes())
            assertEquals(1, f.count())
        } else {
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(0, f.count())
        }
    }
}
