package com.vaultstadio.core.share.access

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.share.access.AccessShareInput
import com.vaultstadio.domain.share.access.PublicShareFailure
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Eleven original ShareService public-access cases, migrated without counting them as new coverage. */
class LegacyPublicShareAccessTest {
    @ParameterizedTest(name = "Legacy share access {0}")
    @ValueSource(
        strings = [
            "success",
            "missing-share",
            "lookup-failure",
            "item-read-failure",
            "inactive",
            "expired",
            "limit",
            "password-missing",
            "password-wrong",
            "password-correct",
            "missing-item",
        ],
    )
    fun originalAccessScenarios(case: String) = runTest {
        val f = ShareAccessFixture()
        val expected = when (case) {
            "missing-share", "missing-item" -> PublicShareFailure.NOT_FOUND
            "lookup-failure", "item-read-failure" -> PublicShareFailure.UNAVAILABLE
            "inactive" -> PublicShareFailure.REVOKED
            "expired" -> PublicShareFailure.EXPIRED
            "limit" -> PublicShareFailure.LIMIT_REACHED
            "password-missing" -> PublicShareFailure.PASSWORD_REQUIRED
            "password-wrong" -> PublicShareFailure.PASSWORD_INVALID
            else -> null
        }
        when (case) {
            "missing-share" -> coEvery { f.repository.find(any()) } returns null.right()
            "lookup-failure" -> coEvery { f.repository.find(any()) } returns PublicShareFailure.UNAVAILABLE.left()
            "missing-item", "item-read-failure" -> coEvery { f.repository.inspect(any()) } returns expected!!.left()
            "inactive" -> f.share = f.share.copy(isActive = false)
            "expired" -> f.share = f.share.copy(expiresAt = Instant.fromEpochSeconds(f.now.epochSeconds - 1))
            "limit" -> f.share = f.share.copy(maxDownloads = 1, downloadCount = 1)
            "password-missing", "password-wrong", "password-correct" -> f.share = f.share.copy(password = "hash")
        }
        val password = when (case) {
            "password-correct" -> "correct"
            "password-wrong" -> "wrong"
            else -> null
        }
        val result = f.service.information(AccessShareInput("ToKen", password))
        if (expected == null) {
            assertTrue(result.isRight())
            assertEquals("file", (result as Either.Right).value.name)
        } else {
            assertEquals(expected, (result as Either.Left).value)
        }
        // Corrected legacy expectation: information never increments/admit-downloads.
        coVerify(exactly = 0) {
            f.repository.admit(any())
            f.reader.prepare(any())
        }
    }
}
