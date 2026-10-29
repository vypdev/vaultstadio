package com.vaultstadio.api

import com.vaultstadio.api.config.HttpServerConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Api116CorsTest {
    @Test fun `configured HTTPS origin is allowed without becoming HTTP`() = testApplication {
        application {
            configureCors(config("https://viewer.invalid:8443"))
            routing { get("/probe") { call.respondText("ok") } }
        }
        val response = client.get("/probe") { header(HttpHeaders.Origin, "https://viewer.invalid:8443") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("https://viewer.invalid:8443", response.headers[HttpHeaders.AccessControlAllowOrigin])
        assertEquals("true", response.headers[HttpHeaders.AccessControlAllowCredentials])
    }

    @ParameterizedTest(name = "API116 CORS denies {0}")
    @ValueSource(
        strings = ["http://viewer.invalid:8443", "https://viewer.invalid:9443", "https://foreign.invalid:8443", "null"],
    )
    fun unconfiguredOriginCannotReachTheHandler(origin: String) = testApplication {
        var effects = 0
        application {
            configureCors(config("https://viewer.invalid:8443"))
            routing {
                get("/probe") {
                    effects++
                    call.respondText("ok")
                }
            }
        }
        assertEquals(HttpStatusCode.Forbidden, client.get("/probe") { header(HttpHeaders.Origin, origin) }.status)
        assertEquals(0, effects)
    }

    @Test fun `uncached private image header is admitted only for the configured origin`() = testApplication {
        application {
            configureCors(config("http://viewer.invalid:18116"))
            routing { get("/probe") { call.respondText("ok") } }
        }
        val response = client.options("/probe") {
            header(HttpHeaders.Origin, "http://viewer.invalid:18116")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
            header(HttpHeaders.AccessControlRequestHeaders, "authorization,cache-control")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("http://viewer.invalid:18116", response.headers[HttpHeaders.AccessControlAllowOrigin])
        assertTrue(response.headers[HttpHeaders.AccessControlAllowHeaders]!!.lowercase().contains("cache-control"))
    }

    @Test fun `existing HTTP and absent origin requests retain their behavior`() = testApplication {
        application {
            configureCors(config("http://viewer.invalid:18116"))
            routing { get("/probe") { call.respondText("ok") } }
        }
        assertEquals(HttpStatusCode.OK, client.get("/probe").status)
        assertEquals(
            HttpStatusCode.OK,
            client.get("/probe") {
                header(HttpHeaders.Origin, "http://viewer.invalid:18116")
            }.status,
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            client.get("/probe") {
                header(HttpHeaders.Origin, "https://viewer.invalid:18116")
            }.status,
        )
    }

    @Test fun `uppercase HTTPS configuration retains its scheme and bare host keeps legacy HTTP`() = testApplication {
        application {
            configureCors(
                HttpServerConfig("0.0.0.0", 8080, false, listOf("HTTPS://viewer.invalid:8443", "legacy.invalid:8081")),
            )
            routing { get("/probe") { call.respondText("ok") } }
        }
        for (origin in listOf("https://viewer.invalid:8443", "http://legacy.invalid:8081")) {
            assertEquals(HttpStatusCode.OK, client.get("/probe") { header(HttpHeaders.Origin, origin) }.status)
        }
        for (origin in listOf("http://viewer.invalid:8443", "https://legacy.invalid:8081")) {
            assertEquals(HttpStatusCode.Forbidden, client.get("/probe") { header(HttpHeaders.Origin, origin) }.status)
        }
    }

    private fun config(origin: String) = HttpServerConfig("0.0.0.0", 8080, false, listOf(origin))
}
