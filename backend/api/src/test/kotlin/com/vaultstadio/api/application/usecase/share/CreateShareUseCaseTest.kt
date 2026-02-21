/**
 * CreateShareUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.application.usecase.share.CreateShareUseCaseImpl
import com.vaultstadio.core.domain.service.CreateShareInput
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.share.model.ShareLink
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateShareUseCaseTest {

    private val shareService: ShareService = mockk()
    private val useCase = CreateShareUseCaseImpl(shareService)

    @Test
    fun `invoke delegates to shareService and returns Right ShareLink`() = runTest {
        val input = CreateShareInput(
            itemId = "item-1",
            userId = "user-1",
            expirationDays = 7,
            password = null,
            maxDownloads = null,
        )
        val share = ShareLink(
            id = "share-1",
            itemId = input.itemId,
            token = "abc123",
            createdBy = input.userId,
            expiresAt = Clock.System.now(),
            password = null,
            maxDownloads = null,
            downloadCount = 0,
            isActive = true,
            createdAt = Clock.System.now(),
        )
        coEvery { shareService.createShare(input) } returns Either.Right(share)

        val result = useCase(input)

        assertTrue(result.isRight())
        assertTrue((result as Either.Right<ShareLink>).value.id == share.id)
    }

    @Test
    fun `invoke returns Left when shareService returns Left`() = runTest {
        val input = CreateShareInput("item-1", "user-1")
        coEvery { shareService.createShare(input) } returns
            Either.Left(AuthorizationException("Only the owner can share this item"))

        val result = useCase(input)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is AuthorizationException)
    }
}
