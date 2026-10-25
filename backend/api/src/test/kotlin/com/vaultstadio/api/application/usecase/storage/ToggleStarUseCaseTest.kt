/**
 * ToggleStarUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.application.usecase.storage.ToggleStarUseCaseImpl
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToggleStarUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = ToggleStarUseCaseImpl(storageService)

    @Test
    fun invokeDelegatesToStorageServiceAndReturnsRightStorageItem() = runTest {
        val now = Clock.System.now()
        val item = StorageItem(
            id = "item-1",
            name = "f",
            path = "/f",
            type = ItemType.FILE,
            ownerId = "user-1",
            isStarred = true,
            createdAt = now,
            updatedAt = now,
        )
        coEvery { storageService.toggleStar("item-1", "user-1") } returns Either.Right(item)

        val result = useCase("item-1", "user-1")

        assertTrue(result.isRight())
        assertTrue((result as Either.Right<StorageItem>).value.isStarred)
    }

    @Test
    fun invokeReturnsLeftWhenStorageServiceReturnsLeft() = runTest {
        coEvery { storageService.toggleStar(any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ItemNotFoundException)
    }
}
