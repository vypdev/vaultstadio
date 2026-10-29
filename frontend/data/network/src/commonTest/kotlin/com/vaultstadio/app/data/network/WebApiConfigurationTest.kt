package com.vaultstadio.app.data.network

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WebApiConfigurationTest {
    @Test fun absentOverrideUsesOnlyTheServingOrigin() {
        assertEquals("https://web.invalid/api", webApiRootCandidate("https://web.invalid", "{}"))
        assertEquals("http://127.0.0.1:18116/api", webApiRootCandidate("http://127.0.0.1:18116", "{}"))
        assertEquals("https://[::1]:8443/api", webApiRootCandidate("https://[::1]:8443", "{}"))
    }

    @Test fun explicitRootsAndVersionedAliasesKeepTheirPath() {
        for ((input, expected) in listOf(
            "https://api.invalid" to "https://api.invalid/api",
            "https://api.invalid/" to "https://api.invalid/api",
            "https://api.invalid/gateway/api/" to "https://api.invalid/gateway/api",
            "https://api.invalid/gateway/v1/" to "https://api.invalid/gateway/v1",
            "https://api.invalid/tenant%20one/api" to "https://api.invalid/tenant%20one/api",
            "https://[2001:db8::1]:8443/api" to "https://[2001:db8::1]:8443/api",
        )) {
            assertEquals(expected, webApiRootCandidate("https://web.invalid", config(input)), input)
        }
    }

    @Test fun developmentHttpCanSelectAnotherExplicitHttpAuthority() {
        assertEquals(
            "http://localhost:8080/api",
            webApiRootCandidate("http://localhost:18116", config("http://localhost:8080")),
        )
    }

    @Test fun httpsPageCannotSelectPlaintextApi() {
        assertNull(webApiRootCandidate("https://web.invalid", config("http://localhost:8080/api")))
    }

    @Test fun invalidDocumentShapesAndUnknownKeysNeverBecomeDefaults() {
        for (body in listOf(
            "", "null", "[]", "true", "42", "{", "\"url\"", "{\"apiUrl\":\"https://api.invalid\"}",
            "{\"apiBaseUrl\":null}", "{\"apiBaseUrl\":false}", "{\"apiBaseUrl\":1}",
            "{\"apiBaseUrl\":{}}", "{\"apiBaseUrl\":[]}",
        )) {
            assertNull(webApiRootCandidate("https://web.invalid", body), body)
        }
    }

    @Test fun missingOrUnsafeAuthorityIsRejectedBeforeNativeParsing() {
        for (value in listOf(
            "", " ", "/api", "//api.invalid/api", "api.invalid", "ftp://api.invalid", "file:///api",
            "javascript:alert(1)", "https:///api", "https://", "https://user@api.invalid", "https://u:p@api.invalid",
        )) {
            invalid(value)
        }
    }

    @Test fun explicitInvalidPortsAndUnbracketedIpv6AreRejected() {
        for (value in listOf(
            "https://api.invalid:0/api", "https://api.invalid:65536/api", "https://api.invalid:-1/api",
            "https://api.invalid:port/api", "https://api.invalid:/api",
            "https://api.invalid:9999999999999/api",
            "https://::1/api", "https://[::1/api", "https://[::1]junk/api", "https://[::1]:/api",
        )) {
            invalid(value)
        }
    }

    @Test fun queryFragmentWhitespaceAndControlsAreNotConfiguration() {
        for (value in listOf(
            "https://api.invalid/api?", "https://api.invalid/api?token=fixture", "https://api.invalid/api#",
            "https://api.invalid/api#fixture", " https://api.invalid", "https://api.invalid ",
            "https://api.invalid/a b",
            "https://api.invalid/a\nb", "https://api.invalid/a\u0000b", "https://api.invalid/a\u007fb",
        )) {
            invalid(value)
        }
    }

    @Test fun navigationRepeatedSeparatorsAndAmbiguousEscapesAreRejected() {
        for (path in listOf(
            "//api", "/api//v1", "/./api", "/../api", "/%2e/api", "/%2e%2e/api",
            "/%252e%252e/api", "/a%2fb/api", "/a%5cb/api", "/a\\b/api", "/%00/api", "/%0a/api", "/%7f/api",
            "/%/api", "/%2/api", "/%zz/api", "/%FF/api",
        )) {
            invalid("https://api.invalid$path")
        }
    }

    @Test fun configurationSizeIsBoundedBeforeJsonParsing() {
        assertEquals("https://web.invalid/api", webApiRootCandidate("https://web.invalid", "{}" + " ".repeat(16_382)))
        assertNull(webApiRootCandidate("https://web.invalid", "{}" + " ".repeat(16_383)))
    }

    @Test fun urlLengthHasAnExactBoundary() {
        val prefix = "https://api.invalid/"
        val exact = prefix + "a".repeat(2048 - prefix.length)
        assertEquals(exact, webApiRootCandidate("https://web.invalid", config(exact)))
        invalid(exact + "a")
    }

    @Test fun invalidPageOriginCannotAuthorizeAConfiguration() {
        for (origin in listOf(
            "null",
            "file:///tmp/index.html",
            "https://web.invalid/path",
            "https://u:p@web.invalid",
            "https://web.invalid?q=fixture",
        )) {
            assertNull(webApiRootCandidate(origin, "{}"), origin)
        }
    }

    @Test fun deeplyNestedInvalidValuesCannotExhaustTheJsonDecoderStack() {
        assertNull(
            webApiRootCandidate(
                "https://web.invalid",
                "{\"apiBaseUrl\":" + "[".repeat(6000) + "]".repeat(6000) + "}",
            ),
        )
        assertNull(webApiRootCandidate("https://web.invalid", "{\"unknown\":{".repeat(900) + "}".repeat(900)))
        assertEquals(
            "https://api.invalid/%7B%7D/api",
            webApiRootCandidate("https://web.invalid", config("https://api.invalid/%7B%7D/api")),
        )
    }

    @Test fun invalidUnicodeScalarsAreNotSilentlyReplacedByUriEncoding() {
        for (char in listOf(0xD800.toChar(), 0xDFFF.toChar())) {
            assertNull(webApiRootCandidate("https://web.invalid", config("https://api.invalid/" + char + "/api")))
        }
        assertNotNull(webApiRootCandidate("https://web.invalid", config("https://api.invalid/😀/api")))
    }

    private fun invalid(value: String) = assertNull(webApiRootCandidate("https://web.invalid", config(value)), value)
    private fun config(value: String) = buildJsonObject { put("apiBaseUrl", JsonPrimitive(value)) }.toString()
}
