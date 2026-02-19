/**
 * MoveItemUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.MoveItemInput
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveItemUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = MoveItemUseCaseImpl(storageService)

    @Test
    fun invokeDelegatesToStorageServiceAndReturnsRightStorageItem() = runTest {
        val now = Clock.System.now()
        val input = MoveItemInput(
            itemId = "item-1",
            newParentId = "parent-2",
            newName = null,
            userId = "user-1",
        )
        val item = StorageItem(
            id = "item-1",
            name = "f",
            path = "/parent-2/f",
            type = ItemType.FILE,
            ownerId = "user-1",
            createdAt = now,
            updatedAt = now,
        )
        coEvery { storageService.moveItem(input) } returns Either.Right(item)

        val result = useCase(input)

        assertTrue(result.isRight())
        assertEquals("item-1", (result as Either.Right).value.id)
        assertEquals("/parent-2/f", result.value.path)
    }

    @Test
    fun invokeReturnsLeftWhenStorageServiceReturnsLeft() = runTest {
        val input = MoveItemInput(
            itemId = "item-1",
            newParentId = "parent-2",
            newName = null,
            userId = "user-1",
        )
        coEvery { storageService.moveItem(input) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase(input)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
