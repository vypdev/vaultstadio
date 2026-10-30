package com.vaultstadio.app.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class Api116ExistingRegressionTest {
    @Test fun customApiRootSurvivesBinaryLocationConstruction() = location(
        "https://audit.invalid/gateway/api/",
        "https://audit.invalid/gateway/api/v1/storage/download/id",
    )

    @Test fun versionedCustomRootIsNotDuplicatedOrDiscarded() = location(
        "https://audit.invalid/gateway/v1/",
        "https://audit.invalid/gateway/v1/storage/download/id",
    )

    @Test fun legacyOriginRetainsItsDefaultApiPrefix() = location(
        "https://audit.invalid",
        "https://audit.invalid/api/v1/storage/download/id",
    )

    @Test fun ipv6AuthorityPortAndCustomRootStayTogether() = location(
        "https://[::1]:8443/gateway/api",
        "https://[::1]:8443/gateway/api/v1/storage/download/id",
    )

    @Test fun ordinaryApiRequestUsesConfiguredPrefixAndPreservesQuery() = runTest {
        withClient(
            MockEngine { request ->
                assertEquals("/gateway/api/v1/auth/me", request.url.encodedPath)
                assertEquals("é +&", request.url.parameters["filter"])
                assertEquals("Bearer ephemeral116", request.headers[HttpHeaders.Authorization])
                respond("{}")
            },
        ) { it.get("/api/v1/auth/me?filter=%C3%A9%20%2B%26") }
    }

    @Test fun genericTransportPathKeepsItsExistingContract() = runTest {
        withClient(
            MockEngine { request ->
                assertEquals("/record", request.url.encodedPath)
                respond("{}")
            },
        ) { it.get("/record") }
    }

    @Test fun explicitForeignAuthorityDoesNotInheritApplicationBearer() = runTest {
        withClient(
            MockEngine { request ->
                assertEquals("foreign.invalid", request.url.host)
                assertEquals("/api/v1/auth/me", request.url.encodedPath)
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("{}")
            },
        ) { it.get("https://foreign.invalid/api/v1/auth/me") }
    }

    @Test fun prefixedPublicCapabilityIsNotExposedByInfoLogging() = runTest {
        val messages = mutableListOf<String>()
        val logger = object : Logger {
            override fun log(message: String) {
                messages += message
            }
        }
        val engine = MockEngine { respond("{}") }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/gateway/api"),
            TokenProvider { null },
            engine,
            logger,
        )
        try {
            client.get("https://audit.invalid/gateway/api/v1/share/opaque116")
            assertFalse(messages.joinToString().contains("opaque116"))
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun legacyLocationSanitizationDoesNotLeakBaseSecrets() {
        val raw = URLBuilder("https://audit.invalid/gateway/api").apply {
            user = "fixture-user"
            password = "fixture-password"
            parameters.append("secret", "fixture-query")
            fragment = "fixture-fragment"
        }.buildString()
        location(raw, "https://audit.invalid/gateway/api/v1/storage/download/id")
    }

    private fun location(base: String, expected: String) = assertEquals(
        expected,
        apiV1Location(base, listOf("storage", "download", "id")),
    )

    private suspend fun withClient(engine: MockEngine, block: suspend (io.ktor.client.HttpClient) -> Unit) {
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/gateway/api"),
            TokenProvider { "ephemeral116" },
            engine,
        )
        try {
            block(client)
        } finally {
            client.close()
            engine.close()
        }
    }
}
