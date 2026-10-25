/**
 * Upload Chunk Use Case
 *
 * Application use case for uploading a single chunk of a chunked upload.
 */

package com.vaultstadio.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.core.domain.service.UploadSession
import com.vaultstadio.core.domain.service.UploadSessionManager
import java.io.File
import java.io.FileOutputStream

/**
 * Use case for uploading a chunk.
 */
interface UploadChunkUseCase {

    operator fun invoke(
        uploadId: String,
        userId: String,
        chunkIndex: Int,
        chunkBytes: ByteArray,
    ): Either<ChunkedUploadError, UploadSession>
}

/**
 * Default implementation using [UploadSessionManager].
 */
class UploadChunkUseCaseImpl(
    private val uploadSessionManager: UploadSessionManager,
) : UploadChunkUseCase {

    override fun invoke(
        uploadId: String,
        userId: String,
        chunkIndex: Int,
        chunkBytes: ByteArray,
    ): Either<ChunkedUploadError, UploadSession> {
        val session = uploadSessionManager.getSessionForUser(uploadId, userId)
            ?: return Either.Left(ChunkedUploadError.NotFound)
        if (session.userId != userId) {
            return Either.Left(ChunkedUploadError.NotFound)
        }
        if (chunkIndex < 0 || chunkIndex >= session.totalChunks) {
            return Either.Left(ChunkedUploadError.InvalidChunkIndex)
        }
        val chunkFile = File("${session.tempDir}/chunk_$chunkIndex")
        FileOutputStream(chunkFile).use { it.write(chunkBytes) }
        uploadSessionManager.markChunkReceived(uploadId, chunkIndex)
        val updated = uploadSessionManager.getSessionForUser(uploadId, userId)!!
        return Either.Right(updated)
    }
}
