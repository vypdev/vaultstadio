package com.vaultstadio.app.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Api116AuthorityTest {
    @Test fun bearerUsesCurrentSessionAndDisappearsOnLogout() = runTest {
        var token: String? = "first"
        val seen = mutableListOf<String?>()
        val engine = MockEngine { request ->
            seen += request.headers[HttpHeaders.Authorization]
            respond("{}")
        }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/gateway/api"),
            TokenProvider {
                token
            },
            engine,
        )
        try {
            client.get("/api/v1/auth/me")
            token = "second"
            client.get("/api/v1/auth/me")
            token = null
            client.get("/api/v1/auth/me")
            assertEquals(listOf("Bearer first", "Bearer second", null), seen)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun schemeAndPortArePartOfTheBearerBoundary() = runTest {
        val engine = MockEngine { request ->
            assertNull(request.headers[HttpHeaders.Authorization])
            respond("{}")
        }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid:8443/gateway/api"),
            TokenProvider {
                "private"
            },
            engine,
        )
        try {
            client.get("http://audit.invalid:8443/api/v1/auth/me")
            client.get("https://audit.invalid:9443/api/v1/auth/me")
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun foreignRedirectDoesNotForwardTheAutomaticBearer() = runTest {
        var calls = 0
        val engine = MockEngine { request ->
            if (calls++ == 0) {
                assertEquals("Bearer private", request.headers[HttpHeaders.Authorization])
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://foreign.invalid/end"))
            } else {
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("ok")
            }
        }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/gateway/api"),
            TokenProvider {
                "private"
            },
            engine,
        )
        try {
            client.get("/api/v1/auth/me")
            assertEquals(2, calls)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun sameAuthorityRedirectRechecksCurrentCredentials() = runTest {
        var token = "before"
        var calls = 0
        val engine = MockEngine { request ->
            if (calls++ == 0) {
                assertEquals("Bearer before", request.headers[HttpHeaders.Authorization])
                token = "after"
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/gateway/api/v1/auth/me"))
            } else {
                assertEquals("Bearer after", request.headers[HttpHeaders.Authorization])
                respond("ok")
            }
        }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/gateway/api"),
            TokenProvider {
                token
            },
            engine,
        )
        try {
            client.get("/api/v1/auth/me")
            assertEquals(2, calls)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun explicitCallerAuthorizationRemainsExplicit() = runTest {
        val engine =
            MockEngine { request ->
                assertEquals("Basic caller", request.headers[HttpHeaders.Authorization])
                respond("ok")
            }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/gateway/api"),
            TokenProvider {
                "private"
            },
            engine,
        )
        try {
            client.get("/api/v1/auth/me") { header(HttpHeaders.Authorization, "Basic caller") }
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun encodedIdsCannotBecomeAdditionalPathSegments() {
        assertEquals(
            "https://audit.invalid/gateway/api/v1/storage/download/a%2Fb%3F%23%20%C3%A9",
            apiV1Location("https://audit.invalid/gateway/api", listOf("storage", "download", "a/b?# é")),
        )
    }

    @Test fun websocketLocationPreservesPrefixAndEscapesEachIdentity() {
        assertEquals(
            "wss://audit.invalid/gateway/api/v1/collaboration/a%2Fb/ws?sessionId=s%26x&userId=u%3F",
            apiWebSocketLocation(
                "https://audit.invalid/gateway/api",
                listOf("collaboration", "a/b", "ws"),
                mapOf(
                    "sessionId" to "s&x",
                    "userId" to "u?",
                ),
            ),
        )
    }
}
