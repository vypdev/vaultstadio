/**
 * VaultStadio CreateFolderUseCase Tests
 *
 * Unit tests for the storage application layer: use case delegates to StorageService.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.core.domain.service.CreateFolderInput
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.ValidationException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateFolderUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = CreateFolderUseCaseImpl(storageService)

    @Test
    fun `invoke delegates to storageService and returns Right StorageItem`() = runTest {
        val input = CreateFolderInput(
            name = "New Folder",
            parentId = null,
            ownerId = "user-1",
        )
        val folder = StorageItem(
            id = "folder-1",
            name = input.name,
            path = "/${input.name}",
            type = ItemType.FOLDER,
            ownerId = input.ownerId,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )

        coEvery { storageService.createFolder(input) } returns Either.Right(folder)

        val result = useCase(input)

        assertTrue(result.isRight())
        assertEquals(folder.id, (result as Either.Right<*>).value.id)
        assertEquals(folder.name, result.value.name)
    }

    @Test
    fun `invoke returns Left when storageService returns Left`() = runTest {
        val input = CreateFolderInput(
            name = "",
            parentId = null,
            ownerId = "user-1",
        )

        coEvery { storageService.createFolder(input) } returns
            Either.Left(ValidationException("Name is required"))

        val result = useCase(input)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ValidationException)
    }
}
