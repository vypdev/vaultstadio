/**
 * SetStarUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetStarUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = SetStarUseCaseImpl(storageService)

    @Test
    fun `invoke delegates to storageService and returns Right StorageItem`() = runTest {
        val item = StorageItem(
            id = "item-1",
            name = "f",
            path = "/f",
            type = ItemType.FILE,
            ownerId = "user-1",
            isStarred = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        coEvery { storageService.setStar("item-1", "user-1", true) } returns Either.Right(item)

        val result = useCase("item-1", "user-1", true)

        assertTrue(result.isRight())
        assertTrue((result as Either.Right).value.isStarred)
    }

    @Test
    fun `invoke returns Left when storageService returns Left`() = runTest {
        coEvery { storageService.setStar(any(), any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("x", "user-1", false)

        assertTrue(result.isLeft())
    }
}
