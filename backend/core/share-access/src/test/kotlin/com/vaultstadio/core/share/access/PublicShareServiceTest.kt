package com.vaultstadio.core.share.access

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.share.access.AccessShareInput
import com.vaultstadio.domain.share.access.PublicShareFailure
import com.vaultstadio.domain.share.access.PublicShareSnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PublicShareServiceTest {
    @ParameterizedTest(name = "S90-invalid-token {0}")
    @ValueSource(
        strings = [
            "", "has space", "a/b", "a?password=x", "á", "a" +
                "________________________________________________________________",
        ],
    )
    fun rejectsMalformedTokensBeforeLookup(token: String) = runTest {
        val f = ShareAccessFixture()
        assertEquals(Either.Left(PublicShareFailure.NOT_FOUND), f.service.information(AccessShareInput(token)))
        coVerify(exactly = 0) {
            f.repository.find(any())
            f.repository.inspect(any())
            f.reader.prepare(any())
        }
    }

    @Test
    fun `S90 metadata preserves token case and does not expose private fields`() = runTest {
        val f = ShareAccessFixture()
        val info = f.service.information(AccessShareInput("ToKen")).getOrNull()!!
        assertEquals("file", info.name)
        assertEquals(5L, info.size)
        coVerify(exactly = 1) {
            f.repository.find("ToKen")
            f.repository.inspect(f.share)
        }
        coVerify(exactly = 0) {
            f.repository.admit(any())
            f.reader.prepare(any())
        }
    }

    @ParameterizedTest(name = "S90-invalid-password-byte-count {0}")
    @ValueSource(ints = [0, 73, 1024])
    fun invalidPasswordIsNotSentToHasher(bytes: Int) = runTest {
        val f = ShareAccessFixture()
        f.share = f.share.copy(password = "hash")
        val outcome = f.service.information(AccessShareInput("ToKen", "a".repeat(bytes)))
        assertEquals(Either.Left(PublicShareFailure.PASSWORD_INVALID), outcome)
        verify(exactly = 0) { f.passwords.matches(any(), any()) }
        coVerify(exactly = 0) { f.repository.inspect(any()) }
    }

    @Test
    fun `S90 download orders authorization preparation admission and transfers ownership`() = runTest {
        val f = ShareAccessFixture()
        val order = mutableListOf<String>()
        coEvery { f.repository.find(any()) } answers {
            order += "find"
            f.share.right()
        }
        coEvery { f.repository.inspect(any()) } answers {
            order += "inspect"
            PublicShareSnapshot(f.share, f.item).right()
        }
        coEvery { f.reader.prepare(f.item) } answers {
            order += "prepare"
            f.prepared.right()
        }
        coEvery { f.repository.admit(any()) } answers {
            order += "admit"
            Unit.right()
        }
        val result = f.service.download(AccessShareInput("ToKen")).getOrNull()!!
        assertEquals(listOf("find", "inspect", "prepare", "admit"), order)
        assertSame(f.prepared, result.content)
        assertFalse(f.closed)
        result.content.close()
        assertTrue(f.closed)
    }

    @ParameterizedTest(name = "S90-admission-failure-closes {0}")
    @EnumSource(PublicShareFailure::class)
    fun failedAdmissionClosesWithoutReturningBytes(failure: PublicShareFailure) = runTest {
        val f = ShareAccessFixture()
        coEvery { f.repository.admit(any()) } returns failure.left()
        assertEquals(Either.Left(failure), f.service.download(AccessShareInput("ToKen")))
        assertTrue(f.closed)
    }

    @Test
    fun `S90 provider preparation failure cannot admit or consume allowance`() = runTest {
        val f = ShareAccessFixture()
        coEvery { f.reader.prepare(any()) } returns PublicShareFailure.UNAVAILABLE.left()
        assertEquals(Either.Left(PublicShareFailure.UNAVAILABLE), f.service.download(AccessShareInput("ToKen")))
        coVerify(exactly = 0) { f.repository.admit(any()) }
    }

    @Test
    fun `S90 password denial cannot open content`() = runTest {
        val f = ShareAccessFixture()
        f.share = f.share.copy(password = "hash")
        assertEquals(Either.Left(PublicShareFailure.PASSWORD_REQUIRED), f.service.download(AccessShareInput("ToKen")))
        coVerify(exactly = 0) {
            f.repository.inspect(any())
            f.reader.prepare(any())
            f.repository.admit(any())
        }
    }

    @Test
    fun `S90 cancellation in admission closes prepared stream and propagates`() = runTest {
        val f = ShareAccessFixture()
        coEvery { f.repository.admit(any()) } throws CancellationException("test")
        assertFailsWith<CancellationException> { f.service.download(AccessShareInput("ToKen")) }
        assertTrue(f.closed)
    }

    @Test
    fun `S90 cancellation after preparation prevents admission and closes stream`() = runTest {
        val f = ShareAccessFixture()
        coEvery { f.reader.prepare(any()) } coAnswers {
            currentCoroutineContext().cancel()
            f.prepared.right()
        }
        assertFailsWith<CancellationException> {
            withContext(Job()) { f.service.download(AccessShareInput("ToKen")) }
        }
        assertTrue(f.closed)
        coVerify(exactly = 0) { f.repository.admit(any()) }
    }

    @Test
    fun `S90 inspect denial cannot open content`() = runTest {
        val f = ShareAccessFixture()
        coEvery { f.repository.inspect(any()) } returns PublicShareFailure.CHANGED.left()
        assertEquals(Either.Left(PublicShareFailure.CHANGED), f.service.download(AccessShareInput("ToKen")))
        coVerify(exactly = 0) {
            f.reader.prepare(any())
            f.repository.admit(any())
        }
    }
}
