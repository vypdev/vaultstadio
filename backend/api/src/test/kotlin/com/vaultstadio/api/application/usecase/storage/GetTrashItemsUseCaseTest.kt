/**
 * GetTrashItemsUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.application.usecase.storage.GetTrashItemsUseCaseImpl
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

class GetTrashItemsUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = GetTrashItemsUseCaseImpl(storageService)

    @Test
    fun invokeDelegatesToStorageServiceAndReturnsRightList() = runTest {
        val now = Clock.System.now()
        val items = listOf(
            StorageItem(
                id = "item-1",
                name = "f",
                path = "/f",
                type = ItemType.FILE,
                ownerId = "user-1",
                createdAt = now,
                updatedAt = now,
            ),
        )
        coEvery { storageService.getTrashItems("user-1") } returns Either.Right(items)

        val result = useCase("user-1")

        assertTrue(result.isRight())
        val right = result as Either.Right<List<StorageItem>>
        assertEquals(1, right.value.size)
        assertEquals("item-1", right.value[0].id)
    }

    @Test
    fun invokeReturnsLeftWhenStorageServiceReturnsLeft() = runTest {
        coEvery { storageService.getTrashItems(any()) } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ItemNotFoundException)
    }
}
