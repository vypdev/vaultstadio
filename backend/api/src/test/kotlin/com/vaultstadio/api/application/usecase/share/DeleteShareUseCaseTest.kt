/**
 * DeleteShareUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.application.usecase.share.DeleteShareUseCaseImpl
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeleteShareUseCaseTest {

    private val shareService: ShareService = mockk()
    private val useCase = DeleteShareUseCaseImpl(shareService)

    @Test
    fun invokeReturnsRightWhenServiceSucceeds() = runTest {
        coEvery { shareService.deleteShare("share-1", "user-1") } returns Either.Right(Unit)

        val result = useCase("share-1", "user-1")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { shareService.deleteShare("share-1", "user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "share-1"))

        val result = useCase("share-1", "user-1")

        assertTrue(result.isLeft())
    }
}
