/**
 * InitChunkedUploadUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.application.usecase.chunkedupload.ChunkedUploadError
import com.vaultstadio.application.usecase.chunkedupload.InitChunkedUploadResult
import com.vaultstadio.application.usecase.chunkedupload.InitChunkedUploadUseCaseImpl
import com.vaultstadio.core.domain.service.UploadSession
import com.vaultstadio.core.domain.service.UploadSessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InitChunkedUploadUseCaseTest {

    private val uploadSessionManager: UploadSessionManager = mockk()
    private val useCase = InitChunkedUploadUseCaseImpl(uploadSessionManager)

    @Test
    fun invokeReturnsInvalidRequestWhenTotalSizeNonPositive() {
        val result = useCase("file.txt", 0, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ChunkedUploadError.InvalidRequest)
    }

    @Test
    fun `invoke returns InvalidRequest when fileName is blank`() {
        val result = useCase("  ", 1000, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ChunkedUploadError.InvalidRequest)
    }

    @Test
    fun `invoke creates session and returns Right with uploadId and chunk counts`() {
        every { uploadSessionManager.createSession(any()) } answers { firstArg() }

        val result = useCase("large.bin", 50 * 1024 * 1024, "user-1", null, null, 10 * 1024 * 1024)

        assertTrue(result.isRight())
        val data = (result as Either.Right<InitChunkedUploadResult>).value
        assertEquals(5, data.totalChunks)
        assertEquals(10 * 1024 * 1024, data.chunkSize)
        verify(exactly = 1) {
            uploadSessionManager.createSession(
                match<UploadSession> {
                    it.fileName == "large.bin" &&
                        it.userId == "user-1"
                },
            )
        }
    }
}
