package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.application.usecase.share.AccessShareUseCaseImpl
import com.vaultstadio.core.share.access.PublicShareService
import com.vaultstadio.domain.share.access.AccessShareInput
import com.vaultstadio.domain.share.access.PublicShareFailure
import com.vaultstadio.domain.share.access.PublicShareInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Same two legacy wrapper cases, corrected from private Pair to public semantic metadata. */
class AccessShareUseCaseTest {
    private val service: PublicShareService = mockk()
    private val useCase = AccessShareUseCaseImpl(service)

    @Test
    fun invokeReturnsInformationWhenServiceSucceeds() = runTest {
        val input = AccessShareInput("t1")
        val info = PublicShareInfo("file", 1, null, false, null)
        coEvery { service.information(input) } returns Either.Right(info)
        assertEquals(Either.Right(info), useCase(input))
        coVerify(exactly = 1) { service.information(input) }
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        val input = AccessShareInput("bad")
        coEvery { service.information(input) } returns Either.Left(PublicShareFailure.NOT_FOUND)
        assertEquals(Either.Left(PublicShareFailure.NOT_FOUND), useCase(input))
    }
}
