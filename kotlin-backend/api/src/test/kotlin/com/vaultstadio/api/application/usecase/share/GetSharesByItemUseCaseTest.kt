/**
 * GetSharesByItemUseCase unit tests.
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

class GetSharesByItemUseCaseTest {

    private val shareService: ShareService = mockk()
    private val useCase = GetSharesByItemUseCaseImpl(shareService)

    @Test
    fun invokeReturnsListWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val shares = listOf(
            ShareLink(
                id = "s1",
                itemId = "item-1",
                token = "t1",
                createdBy = "user-1",
                createdAt = now,
            ),
        )
        coEvery { shareService.getSharesByItem("item-1", "user-1") } returns Either.Right(shares)

        val result = useCase("item-1", "user-1")

        assertTrue(result.isRight())
        assertEquals(1, (result as Either.Right).value.size)
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { shareService.getSharesByItem("item-1", "user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", "user-1")

        assertTrue(result.isLeft())
    }
}
