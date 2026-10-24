/**
 * CompleteChunkedUploadUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.application.usecase.chunkedupload.CompleteChunkedUploadUseCaseImpl
import com.vaultstadio.application.usecase.storage.UploadFileUseCase
import com.vaultstadio.core.domain.service.UploadSession
import com.vaultstadio.core.domain.service.UploadSessionManager
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CompleteChunkedUploadUseCaseTest {

    private val uploadFileUseCase: UploadFileUseCase = mockk()
    private val uploadSessionManager: UploadSessionManager = mockk()
    private val useCase = CompleteChunkedUploadUseCaseImpl(uploadFileUseCase, uploadSessionManager)

    @Test
    fun `invoke returns Left when session not found`() = runTest {
        every { uploadSessionManager.getSessionForUser("upload-1", "user-1") } returns null

        val result = useCase("upload-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ItemNotFoundException)
    }

    @Test
    fun invokeReturnsRightStorageItemWhenSessionCompleteAndUploadSucceeds() = runTest {
        val tempDir = java.nio.file.Files.createTempDirectory("vs-complete").toString()
        try {
            File("$tempDir/chunk_0").writeBytes(byteArrayOf(1, 2, 3))
            File("$tempDir/chunk_1").writeBytes(byteArrayOf(4, 5))
            val session = UploadSession(
                id = "upload-1",
                userId = "user-1",
                fileName = "f.bin",
                totalSize = 5,
                mimeType = null,
                parentId = null,
                chunkSize = 3,
                totalChunks = 2,
                createdAt = Clock.System.now(),
                tempDir = tempDir,
            )
            session.receivedChunks.add(0)
            session.receivedChunks.add(1)
            val file = StorageItem(
                id = "file-1",
                name = "f.bin",
                path = "/f.bin",
                type = ItemType.FILE,
                ownerId = "user-1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            every { uploadSessionManager.getSessionForUser("upload-1", "user-1") } returns session
            coEvery { uploadFileUseCase(any()) } returns Either.Right(file)
            every { uploadSessionManager.removeSession("upload-1") } returns session

            val result = useCase("upload-1", "user-1")

            assertTrue(result.isRight())
            assertEquals("file-1", (result as Either.Right<StorageItem>).value.id)
            verify(exactly = 1) { uploadSessionManager.removeSession("upload-1") }
        } finally {
            File(tempDir).deleteRecursively()
        }
    }
}
