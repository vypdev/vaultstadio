/**
 * GetOrCreateFolderUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.application.usecase.storage.GetOrCreateFolderUseCaseImpl
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.core.domain.service.GetOrCreateFolderResult
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.ValidationException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetOrCreateFolderUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = GetOrCreateFolderUseCaseImpl(storageService)

    @Test
    fun `invoke delegates to storageService and returns Right with created true`() = runTest {
        val folder = StorageItem(
            id = "folder-1",
            name = "New",
            path = "/New",
            type = ItemType.FOLDER,
            ownerId = "user-1",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val resultType = GetOrCreateFolderResult(folder, created = true)
        coEvery { storageService.getOrCreateFolder("New", null, "user-1") } returns Either.Right(resultType)

        val result = useCase("New", null, "user-1")

        assertTrue(result.isRight())
        val right = result as Either.Right<GetOrCreateFolderResult>
        assertTrue(right.value.created)
        assertTrue(right.value.folder.name == "New")
    }

    @Test
    fun `invoke returns Left when storageService returns Left`() = runTest {
        coEvery { storageService.getOrCreateFolder(any(), any(), any()) } returns
            Either.Left(ValidationException(message = "Folder name cannot be empty"))

        val result = useCase("", null, "user-1")

        assertTrue(result.isLeft())
        assertFalse(result.isRight())
    }
}
