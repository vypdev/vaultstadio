/**
 * Complete Chunked Upload Use Case
 *
 * Application use case for completing a chunked upload (merge chunks and persist).
 */

package com.vaultstadio.api.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.api.application.usecase.storage.UploadFileUseCase
import com.vaultstadio.api.service.UploadSessionManager
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.UploadFileInput
import com.vaultstadio.core.exception.ItemNotFoundException
import com.vaultstadio.core.exception.StorageException
import com.vaultstadio.core.exception.ValidationException
import java.io.File
import java.io.FileInputStream
import java.io.SequenceInputStream
import java.util.Collections

/**
 * Use case for completing a chunked upload.
 * Returns [StorageItem] on success, or [ChunkedUploadError] / [StorageException] on failure.
 */
interface CompleteChunkedUploadUseCase {

    suspend operator fun invoke(uploadId: String, userId: String): Either<StorageException, StorageItem>
}

/**
 * Default implementation using [UploadFileUseCase] and [UploadSessionManager].
 */
class CompleteChunkedUploadUseCaseImpl(
    private val uploadFileUseCase: UploadFileUseCase,
    private val uploadSessionManager: UploadSessionManager,
) : CompleteChunkedUploadUseCase {

    override suspend fun invoke(uploadId: String, userId: String): Either<StorageException, StorageItem> {
        val session = uploadSessionManager.getSessionForUser(uploadId, userId)
            ?: return Either.Left(ItemNotFoundException(message = "Upload session not found"))
        if (!session.isComplete) {
            val missing = (0 until session.totalChunks).filter { it !in session.receivedChunks }
            return Either.Left(ValidationException(message = "Missing chunks: $missing"))
        }
        val chunkStreams = (0 until session.totalChunks).map { index ->
            FileInputStream(File("${session.tempDir}/chunk_$index"))
        }
        val combinedStream = SequenceInputStream(Collections.enumeration(chunkStreams))
        val input = UploadFileInput(
            name = session.fileName,
            parentId = session.parentId,
            ownerId = userId,
            mimeType = session.mimeType,
            size = session.totalSize,
            inputStream = combinedStream,
        )
        return uploadFileUseCase(input).fold(
            { error ->
                cleanupTempDir(session.tempDir)
                uploadSessionManager.removeSession(uploadId)
                Either.Left(error)
            },
            { file ->
                uploadSessionManager.removeSession(uploadId)
                Either.Right(file)
            },
        )
    }

    private fun cleanupTempDir(tempDir: String) {
        try {
            File(tempDir).takeIf { it.exists() }?.deleteRecursively()
        } catch (_: Exception) {
            // Log only if needed
        }
    }
}
