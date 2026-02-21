/**
 * AccessShareUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.core.domain.service.AccessShareInput
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessShareUseCaseTest {

    private val shareService: ShareService = mockk()
    private val useCase = AccessShareUseCaseImpl(shareService)

    @Test
    fun invokeReturnsPairWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val share = ShareLink(
            id = "s1",
            itemId = "item-1",
            token = "t1",
            createdBy = "user-1",
            createdAt = now,
        )
        val item = StorageItem(
            id = "item-1",
            name = "f",
            path = "/f",
            type = ItemType.FILE,
            ownerId = "user-1",
            createdAt = now,
            updatedAt = now,
        )
        val input = AccessShareInput(token = "t1")
        coEvery { shareService.accessShare(input) } returns Either.Right(Pair(share, item))

        val result = useCase(input)

        assertTrue(result.isRight())
        val pair = (result as Either.Right<*>).value
        assertTrue(pair.first.id == "s1" && pair.second.id == "item-1")
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        val input = AccessShareInput(token = "bad")
        coEvery { shareService.accessShare(input) } returns Either.Left(ItemNotFoundException(itemId = "share"))

        val result = useCase(input)

        assertTrue(result.isLeft())
    }
}
