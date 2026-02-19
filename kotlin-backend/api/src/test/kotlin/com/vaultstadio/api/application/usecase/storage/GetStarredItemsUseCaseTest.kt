/**
 * GetStarredItemsUseCase unit tests.
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetStarredItemsUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = GetStarredItemsUseCaseImpl(storageService)

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
                isStarred = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        coEvery { storageService.getStarredItems("user-1") } returns Either.Right(items)

        val result = useCase("user-1")

        assertTrue(result.isRight())
        assertEquals(1, (result as Either.Right).value.size)
        assertEquals("item-1", result.value[0].id)
    }

    @Test
    fun invokeReturnsLeftWhenStorageServiceReturnsLeft() = runTest {
        coEvery { storageService.getStarredItems(any()) } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
