/**
 * UploadFileUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.application.usecase.storage.UploadFileUseCaseImpl
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UploadFileInput
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.common.exception.ValidationException
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
        val right = result as Either.Right<StorageItem>
        assertEquals("item-1", right.value.id)
        assertEquals("f.txt", right.value.name)
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
        assertTrue((result as Either.Left<ValidationException>).value is ValidationException)
    }
}
