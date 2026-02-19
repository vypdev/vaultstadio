/**
 * GetSharesSharedWithUserUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetSharesSharedWithUserUseCaseTest {

    private val shareService: ShareService = mockk()
    private val useCase = GetSharesSharedWithUserUseCaseImpl(shareService)

    @Test
    fun invokeReturnsListWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val shares = listOf(
            ShareLink(
                id = "s1",
                itemId = "item-1",
                token = "t1",
                createdBy = "other",
                createdAt = now,
            ),
        )
        coEvery { shareService.getSharesSharedWithUser("user-1", true) } returns Either.Right(shares)

        val result = useCase("user-1", activeOnly = true)

        assertTrue(result.isRight())
        assertEquals(1, (result as Either.Right).value.size)
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { shareService.getSharesSharedWithUser("user-1", false) } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("user-1", activeOnly = false)

        assertTrue(result.isLeft())
    }
}
