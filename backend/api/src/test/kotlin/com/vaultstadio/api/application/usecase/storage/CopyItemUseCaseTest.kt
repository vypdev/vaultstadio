/**
 * CopyItemUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.application.usecase.storage.CopyItemUseCaseImpl
import com.vaultstadio.core.domain.service.CopyItemInput
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CopyItemUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = CopyItemUseCaseImpl(storageService)

    @Test
    fun invokeDelegatesToStorageServiceAndReturnsRightStorageItem() = runTest {
        val now = Clock.System.now()
        val input = CopyItemInput(
            itemId = "item-1",
            destinationParentId = "parent-2",
            newName = null,
            userId = "user-1",
        )
        val item = StorageItem(
            id = "item-copy",
            name = "f",
            path = "/parent-2/f",
            type = ItemType.FILE,
            ownerId = "user-1",
            createdAt = now,
            updatedAt = now,
        )
        coEvery { storageService.copyItem(input) } returns Either.Right(item)

        val result = useCase(input)

        assertTrue(result.isRight())
        assertEquals("item-copy", (result as Either.Right<StorageItem>).value.id)
    }

    @Test
    fun invokeReturnsLeftWhenStorageServiceReturnsLeft() = runTest {
        val input = CopyItemInput(
            itemId = "item-1",
            destinationParentId = "parent-2",
            newName = null,
            userId = "user-1",
        )
        coEvery { storageService.copyItem(input) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase(input)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ItemNotFoundException)
    }
}
