/**
 * GetShareUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetShareUseCaseTest {

    private val shareService: ShareService = mockk()
    private val useCase = GetShareUseCaseImpl(shareService)

    @Test
    fun invokeReturnsShareWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val share = ShareLink(
            id = "share-1",
            itemId = "item-1",
            token = "tok",
            createdBy = "user-1",
            expiresAt = now,
            createdAt = now,
        )
        coEvery { shareService.getShare("share-1") } returns Either.Right(share)

        val result = useCase("share-1")

        assertTrue(result.isRight())
        assertEquals("share-1", (result as Either.Right<*>).value.id)
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { shareService.getShare("share-1") } returns Either.Left(ItemNotFoundException(itemId = "share-1"))

        val result = useCase("share-1")

        assertTrue(result.isLeft())
    }
}
