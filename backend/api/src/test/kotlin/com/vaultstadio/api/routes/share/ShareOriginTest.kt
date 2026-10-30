package com.vaultstadio.api.routes.share

import com.vaultstadio.domain.common.exception.ValidationException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShareOriginTest {
    @ParameterizedTest(name = "S90-origin-valid {0}")
    @ValueSource(strings = ["api.test", "api.test:443", "api.test:8443", "127.0.0.1:18086", "[::1]", "[::1]:18086"])
    fun authorityIncludingPublicPortIsPreserved(authority: String) {
        assertEquals("https://$authority", validatedShareOrigin("HTTPS", authority))
    }

    @ParameterizedTest(name = "S90-origin-invalid {0}")
    @ValueSource(
        strings = [
            "", "api.test:", "api.test:0", "api.test:65536", "user:password@api.test",
            "api.test/path", "api.test?password=secret", "api.test#fragment", "a b", "::1", "api.test,other.test",
        ],
    )
    fun invalidAuthorityCannotEscapeIntoNavigation(authority: String) {
        val failure = assertFailsWith<ValidationException> { validatedShareOrigin("http", authority) }
        assertEquals("Invalid public share origin", failure.message)
    }

    @ParameterizedTest(name = "S90-origin-scheme-invalid {0}")
    @ValueSource(strings = ["javascript", "file", "https,http", ""])
    fun nonHttpSchemesAreRejected(scheme: String) {
        assertFailsWith<ValidationException> { validatedShareOrigin(scheme, "api.test") }
    }
}
