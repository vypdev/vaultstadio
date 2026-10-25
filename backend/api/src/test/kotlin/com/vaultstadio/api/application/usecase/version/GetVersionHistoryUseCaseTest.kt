/**
 * GetVersionHistoryUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.application.usecase.version.GetVersionHistoryUseCaseImpl
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.service.FileVersionService
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

class GetVersionHistoryUseCaseTest {

    private val fileVersionService: FileVersionService = mockk()
    private val useCase = GetVersionHistoryUseCaseImpl(fileVersionService)

    @Test
    fun invokeReturnsHistoryWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val item = StorageItem(
            id = "item-1",
            name = "f.txt",
            path = "/f.txt",
            type = ItemType.FILE,
            ownerId = "user-1",
            createdAt = now,
            updatedAt = now,
        )
        val version = FileVersion(
            itemId = "item-1",
            versionNumber = 1,
            size = 100L,
            checksum = "abc",
            storageKey = "key-1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
        )
        val history = FileVersionHistory(
            item = item,
            versions = listOf(version),
            totalVersions = 1,
            totalSize = 100L,
        )
        coEvery { fileVersionService.getHistory("item-1") } returns Either.Right(history)

        val result = useCase("item-1")

        assertTrue(result.isRight())
        val resultHistory = (result as Either.Right<FileVersionHistory>).value
        assertEquals("item-1", resultHistory.item.id)
        assertEquals(1, resultHistory.versions.size)
        assertEquals(1, resultHistory.totalVersions)
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { fileVersionService.getHistory("item-1") } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<ItemNotFoundException>).value is ItemNotFoundException)
    }
}
