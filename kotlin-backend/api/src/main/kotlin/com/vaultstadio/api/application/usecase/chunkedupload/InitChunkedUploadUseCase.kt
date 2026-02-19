/**
 * Init Chunked Upload Use Case
 *
 * Application use case for initializing a chunked upload session.
 */

package com.vaultstadio.api.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.api.service.UploadSession
import com.vaultstadio.api.service.UploadSessionManager
import kotlinx.datetime.Clock
import java.io.File
import java.util.UUID

/**
 * Result of initializing a chunked upload.
 */
data class InitChunkedUploadResult(
    val uploadId: String,
    val chunkSize: Long,
    val totalChunks: Int,
)

/**
 * Use case for initializing a chunked upload.
 */
interface InitChunkedUploadUseCase {

    operator fun invoke(
        fileName: String,
        totalSize: Long,
        userId: String,
        mimeType: String? = null,
        parentId: String? = null,
        chunkSize: Long = 10 * 1024 * 1024,
    ): Either<ChunkedUploadError, InitChunkedUploadResult>
}

/**
 * Default implementation using [UploadSessionManager].
 */
class InitChunkedUploadUseCaseImpl(
    private val uploadSessionManager: UploadSessionManager,
) : InitChunkedUploadUseCase {

    override fun invoke(
        fileName: String,
        totalSize: Long,
        userId: String,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): Either<ChunkedUploadError, InitChunkedUploadResult> {
        if (totalSize <= 0) {
            return Either.Left(ChunkedUploadError.InvalidRequest("Total size must be positive"))
        }
        if (fileName.isBlank()) {
            return Either.Left(ChunkedUploadError.InvalidRequest("File name is required"))
        }
        val clampedChunkSize = chunkSize.coerceIn(1 * 1024 * 1024, 100 * 1024 * 1024)
        val totalChunks = ((totalSize + clampedChunkSize - 1) / clampedChunkSize).toInt()
        val uploadId = UUID.randomUUID().toString()
        val tempDir = "${System.getProperty("java.io.tmpdir")}/vaultstadio-uploads/$uploadId"
        File(tempDir).mkdirs()
        val session = UploadSession(
            id = uploadId,
            userId = userId,
            fileName = fileName,
            totalSize = totalSize,
            mimeType = mimeType,
            parentId = parentId,
            chunkSize = clampedChunkSize,
            totalChunks = totalChunks,
            createdAt = Clock.System.now(),
            tempDir = tempDir,
        )
        uploadSessionManager.createSession(session)
        return Either.Right(
            InitChunkedUploadResult(
                uploadId = uploadId,
                chunkSize = clampedChunkSize,
                totalChunks = totalChunks,
            ),
        )
    }
}
