/**
 * GetBreadcrumbsUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.application.usecase.storage.GetBreadcrumbsUseCaseImpl
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

class GetBreadcrumbsUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = GetBreadcrumbsUseCaseImpl(storageService)

    @Test
    fun invokeDelegatesToStorageServiceAndReturnsRightList() = runTest {
        val now = Clock.System.now()
        val breadcrumbs = listOf(
            StorageItem(
                id = "root",
                name = "",
                path = "/",
                type = ItemType.FOLDER,
                ownerId = "user-1",
                createdAt = now,
                updatedAt = now,
            ),
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
        coEvery { storageService.getBreadcrumbs("item-1", "user-1") } returns Either.Right(breadcrumbs)

        val result = useCase("item-1", "user-1")

        assertTrue(result.isRight())
        val right = result as Either.Right<List<StorageItem>>
        assertEquals(2, right.value.size)
        assertEquals("root", right.value[0].id)
    }

    @Test
    fun invokeReturnsLeftWhenStorageServiceReturnsLeft() = runTest {
        coEvery { storageService.getBreadcrumbs(any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ItemNotFoundException)
    }
}
