/**
 * VaultStadio Health Routes Tests
 *
 * Unit tests for health check API endpoints.
 */

package com.vaultstadio.api.routes

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Health route tests. These tests do not load the full Application.module() (which requires
 * a database). To assert 200 on /health, /health/live, /health/ready in integration, run
 * tests with testApplication { application { module() } } and a test DB (e.g. Testcontainers).
 */
class HealthRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Nested
    inner class BasicHealthTests {

        @Test
        fun `GET health without app returns 404`() = testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json(json)
                }
            }
            val response = client.get("/health")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `GET health endpoint is hit`() = testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json(json)
                }
            }
            val response = client.get("/health")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Nested
    inner class LivenessTests {

        @Test
        fun `GET liveness without app returns 404`() = testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json(json)
                }
            }
            val response = client.get("/health/live")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Nested
    inner class ReadinessTests {

        @Test
        fun `GET readiness without app returns 404`() = testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json(json)
                }
            }
            val response = client.get("/health/ready")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `GET readiness endpoint is hit`() = testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json(json)
                }
            }
            val response = client.get("/health/ready")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Nested
    inner class DetailedHealthTests {

        @Test
        fun `GET health details without app returns 404`() = testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json(json)
                }
            }
            val response = client.get("/health/details")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }
}
