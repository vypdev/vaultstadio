/**
 * VaultStadio ListFolderUseCase Tests
 *
 * Unit tests for the storage application layer: use case delegates to StorageService.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.application.usecase.storage.ListFolderUseCaseImpl
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.SortField
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListFolderUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = ListFolderUseCaseImpl(storageService)

    @Test
    fun `invoke delegates to storageService and returns Right PagedResult`() = runTest {
        val userId = "user-1"
        val folderId: String? = null
        val query = StorageItemQuery(
            parentId = folderId,
            ownerId = userId,
            isTrashed = false,
            sortField = SortField.NAME,
            sortOrder = SortOrder.ASC,
            limit = 100,
            offset = 0,
        )
        val item = StorageItem(
            id = "item-1",
            name = "doc",
            path = "/doc",
            type = ItemType.FOLDER,
            ownerId = userId,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val pagedResult = PagedResult(
            items = listOf(item),
            total = 1,
            offset = 0,
            limit = 100,
        )

        coEvery {
            storageService.listFolder(folderId, userId, query)
        } returns Either.Right(pagedResult)

        val result = useCase(folderId, userId, query)

        assertTrue(result.isRight())
        val right = result as Either.Right<PagedResult<StorageItem>>
        assertEquals(pagedResult.items.size, right.value.items.size)
        assertEquals(pagedResult.total, right.value.total)
    }

    @Test
    fun `invoke returns Left when storageService returns Left`() = runTest {
        val userId = "user-1"
        val folderId = "missing-folder"
        val query = StorageItemQuery(
            parentId = folderId,
            ownerId = userId,
            isTrashed = false,
        )

        coEvery {
            storageService.listFolder(folderId, userId, query)
        } returns Either.Left(ItemNotFoundException(itemId = folderId))

        val result = useCase(folderId, userId, query)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ItemNotFoundException)
    }
}
