/**
 * SearchUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.application.usecase.storage.SearchUseCaseImpl
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = SearchUseCaseImpl(storageService)

    @Test
    fun `invoke delegates to storageService and returns Right PagedResult`() = runTest {
        val item = StorageItem(
            id = "item-1",
            name = "doc.pdf",
            path = "/doc.pdf",
            type = ItemType.FILE,
            ownerId = "user-1",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val paged = PagedResult(items = listOf(item), total = 1, offset = 0, limit = 50)
        coEvery { storageService.search("doc", "user-1", 50, 0) } returns Either.Right(paged)

        val result = useCase("doc", "user-1", 50, 0)

        assertTrue(result.isRight())
        assertTrue((result as Either.Right<PagedResult<StorageItem>>).value.items.size == 1)
    }

    @Test
    fun `invoke returns Left when storageService returns Left`() = runTest {
        coEvery { storageService.search(any(), any(), any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("q", "user-1")

        assertTrue(result.isLeft())
    }
}
