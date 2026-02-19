/**
 * UploadFileUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UploadFileInput
import com.vaultstadio.core.exception.ValidationException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class UploadFileUseCaseTest {

    private val storageService: StorageService = mockk()
    private val useCase = UploadFileUseCaseImpl(storageService)

    @Test
    fun invokeDelegatesToStorageServiceAndReturnsRightStorageItem() = runTest {
        val now = Clock.System.now()
        val input = UploadFileInput(
            name = "f.txt",
            parentId = null,
            ownerId = "user-1",
            mimeType = "text/plain",
            size = 10L,
            inputStream = ByteArrayInputStream(ByteArray(10)),
        )
        val item = StorageItem(
            id = "item-1",
            name = input.name,
            path = "/${input.name}",
            type = ItemType.FILE,
            ownerId = input.ownerId,
            createdAt = now,
            updatedAt = now,
        )
        coEvery { storageService.uploadFile(input) } returns Either.Right(item)

        val result = useCase(input)

        assertTrue(result.isRight())
        assertEquals("item-1", (result as Either.Right).value.id)
        assertEquals("f.txt", result.value.name)
    }

    @Test
    fun invokeReturnsLeftWhenStorageServiceReturnsLeft() = runTest {
        val input = UploadFileInput(
            name = "f.txt",
            parentId = null,
            ownerId = "user-1",
            mimeType = "text/plain",
            size = 0L,
            inputStream = ByteArrayInputStream(ByteArray(0)),
        )
        coEvery { storageService.uploadFile(input) } returns
            Either.Left(ValidationException("Upload failed"))

        val result = useCase(input)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ValidationException)
    }
}
