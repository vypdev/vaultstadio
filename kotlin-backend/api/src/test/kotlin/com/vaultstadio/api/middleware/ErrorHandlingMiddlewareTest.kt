/**
 * Tests that the error handling middleware (configureErrorHandling) is invoked
 * when routes throw. Uses testApplication with a minimal app that installs
 * StatusPages and a route that throws.
 */

package com.vaultstadio.api.middleware

import com.vaultstadio.api.config.AuthenticationException
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.core.exception.ItemNotFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ErrorHandlingMiddlewareTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `StorageException returns correct status and ApiResponse body`() = testApplication {
        application {
            install(ServerContentNegotiation) {
                json(json)
            }
            configureErrorHandling()
            routing {
                get("/fail-item") {
                    throw ItemNotFoundException("item-123")
                }
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val response = client.get("/fail-item")
        assertEquals(HttpStatusCode.NotFound, response.status)
        val body: ApiResponse<Unit> = response.body()
        assertFalse(body.success)
        assertNotNull(body.error)
        assertEquals("ITEM_NOT_FOUND", body.error?.code)
    }

    @Test
    fun `AuthenticationException returns 401`() = testApplication {
        application {
            install(ServerContentNegotiation) {
                json(json)
            }
            configureErrorHandling()
            routing {
                get("/fail-auth") {
                    throw AuthenticationException()
                }
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val response = client.get("/fail-auth")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body: ApiResponse<Unit> = response.body()
        assertFalse(body.success)
        assertNotNull(body.error)
        assertEquals("AUTHENTICATION_REQUIRED", body.error?.code)
    }

    @Test
    fun `IllegalArgumentException returns 400 validation error`() = testApplication {
        application {
            install(ServerContentNegotiation) {
                json(json)
            }
            configureErrorHandling()
            routing {
                post("/fail-validation") {
                    throw IllegalArgumentException("Invalid input")
                }
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val response = client.post("/fail-validation")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body: ApiResponse<Unit> = response.body()
        assertFalse(body.success)
        assertNotNull(body.error)
        assertEquals("VALIDATION_ERROR", body.error?.code)
    }
}
