package com.vaultstadio.api.routes.share

import arrow.core.Either
import com.vaultstadio.domain.share.access.PublicShareFailure
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Base64
import kotlin.test.assertEquals

class SharePasswordHeaderTest {
    @ParameterizedTest(name = "S90-header-encoding-invalid {0}")
    @ValueSource(strings = ["", "=", "YQ==", "YQ=", "YR", "Y+", "Y/", "a b", "☃", "_w", "wK8"])
    fun invalidOrNonCanonicalEncodingFailsSafely(value: String) {
        assertEquals(Either.Left(PublicShareFailure.INVALID_REQUEST), decodeSharePassword(value))
    }

    @ParameterizedTest(name = "S90-header-encoding-bounds {0}")
    @ValueSource(ints = [1, 72, 73, 1000])
    fun decodedByteBoundsAreEnforced(size: Int) {
        val password = "a".repeat(size)
        val value = Base64.getUrlEncoder().withoutPadding().encodeToString(password.toByteArray())
        assertEquals(
            if (size <= 72) Either.Right(password) else Either.Left(PublicShareFailure.INVALID_REQUEST),
            decodeSharePassword(value),
        )
    }

    @ParameterizedTest(name = "S90-header-encoding-valid {0}")
    @ValueSource(strings = ["ascii", "unicode", "spaces", "absent"])
    fun validUtf8AndAbsentHeaderAreDistinct(case: String) {
        val value = when (case) {
            "ascii" -> "Y29ycmVjdA"
            "unicode" -> "4piD"
            "spaces" -> "ICslIA"
            else -> null
        }
        val expected = when (case) {
            "ascii" -> "correct"
            "unicode" -> "☃"
            "spaces" -> " +% "
            else -> null
        }
        assertEquals(Either.Right(expected), decodeSharePassword(value))
    }
}
