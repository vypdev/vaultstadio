/**
 * UploadChunkUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.api.service.UploadSession
import com.vaultstadio.api.service.UploadSessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class UploadChunkUseCaseTest {

    private val uploadSessionManager: UploadSessionManager = mockk()
    private val useCase = UploadChunkUseCaseImpl(uploadSessionManager)

    @Test
    fun `invoke returns NotFound when session does not exist`() {
        every { uploadSessionManager.getSessionForUser("upload-1", "user-1") } returns null

        val result = useCase("upload-1", "user-1", 0, byteArrayOf(1, 2, 3))

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ChunkedUploadError.NotFound)
    }

    @Test
    fun invokeReturnsInvalidChunkIndexWhenChunkIndexOutOfRange() {
        val tempDir = java.nio.file.Files.createTempDirectory("vs-upload").toString()
        try {
            val session = UploadSession(
                id = "upload-1",
                userId = "user-1",
                fileName = "f.bin",
                totalSize = 100,
                mimeType = null,
                parentId = null,
                chunkSize = 50,
                totalChunks = 2,
                createdAt = Clock.System.now(),
                tempDir = tempDir,
            )
            every { uploadSessionManager.getSessionForUser("upload-1", "user-1") } returns session

            val result = useCase("upload-1", "user-1", 5, byteArrayOf(1, 2))

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ChunkedUploadError.InvalidChunkIndex)
        } finally {
            java.io.File(tempDir).deleteRecursively()
        }
    }

    @Test
    fun invokeWritesChunkAndReturnsRightSession() {
        val tempDir = java.nio.file.Files.createTempDirectory("vs-upload").toString()
        val session = UploadSession(
            id = "upload-2",
            userId = "user-1",
            fileName = "f.bin",
            totalSize = 100,
            mimeType = null,
            parentId = null,
            chunkSize = 50,
            totalChunks = 2,
            createdAt = Clock.System.now(),
            tempDir = tempDir,
        )
        every { uploadSessionManager.getSessionForUser("upload-2", "user-1") } returns session
        every { uploadSessionManager.markChunkReceived("upload-2", 0) } returns true
        every { uploadSessionManager.getSessionForUser("upload-2", "user-1") } returns session

        val result = useCase("upload-2", "user-1", 0, byteArrayOf(1, 2, 3))

        assertTrue(result.isRight())
        verify(exactly = 1) { uploadSessionManager.markChunkReceived("upload-2", 0) }
        File(tempDir).deleteRecursively()
    }
}
