/**
 * Cancel Chunked Upload Use Case
 *
 * Application use case for cancelling/aborting a chunked upload session.
 */

package com.vaultstadio.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.core.domain.service.UploadSessionManager

/**
 * Use case for cancelling a chunked upload.
 */
interface CancelChunkedUploadUseCase {

    operator fun invoke(uploadId: String, userId: String): Either<ChunkedUploadError, Unit>
}

/**
 * Default implementation using [UploadSessionManager].
 */
class CancelChunkedUploadUseCaseImpl(
    private val uploadSessionManager: UploadSessionManager,
) : CancelChunkedUploadUseCase {

    override fun invoke(uploadId: String, userId: String): Either<ChunkedUploadError, Unit> {
        val session = uploadSessionManager.getSessionForUser(uploadId, userId)
            ?: return Either.Left(ChunkedUploadError.NotFound)
        uploadSessionManager.removeSession(uploadId)
        return Either.Right(Unit)
    }
}
