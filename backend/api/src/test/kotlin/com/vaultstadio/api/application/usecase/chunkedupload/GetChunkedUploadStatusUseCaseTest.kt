/**
 * GetChunkedUploadStatusUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.api.service.UploadSession
import com.vaultstadio.api.service.UploadSessionManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetChunkedUploadStatusUseCaseTest {

    private val uploadSessionManager: UploadSessionManager = mockk()
    private val useCase = GetChunkedUploadStatusUseCaseImpl(uploadSessionManager)

    @Test
    fun `invoke returns NotFound when session does not exist`() {
        every { uploadSessionManager.getSessionForUser("upload-1", "user-1") } returns null

        val result = useCase("upload-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ChunkedUploadError.NotFound)
    }

    @Test
    fun `invoke returns Right session when session exists`() {
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
            tempDir = "/tmp/u1",
        )
        every { uploadSessionManager.getSessionForUser("upload-1", "user-1") } returns session

        val result = useCase("upload-1", "user-1")

        assertTrue(result.isRight())
        assertEquals("f.bin", (result as Either.Right<*>).value.fileName)
    }
}
