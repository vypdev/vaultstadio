/**
 * Get Chunked Upload Status Use Case
 *
 * Application use case for retrieving the status of a chunked upload session.
 */

package com.vaultstadio.api.application.usecase.chunkedupload

import arrow.core.Either
import com.vaultstadio.api.service.UploadSession
import com.vaultstadio.api.service.UploadSessionManager

/**
 * Use case for getting chunked upload status.
 */
interface GetChunkedUploadStatusUseCase {

    operator fun invoke(uploadId: String, userId: String): Either<ChunkedUploadError, UploadSession>
}

/**
 * Default implementation using [UploadSessionManager].
 */
class GetChunkedUploadStatusUseCaseImpl(
    private val uploadSessionManager: UploadSessionManager,
) : GetChunkedUploadStatusUseCase {

    override fun invoke(uploadId: String, userId: String): Either<ChunkedUploadError, UploadSession> {
        val session = uploadSessionManager.getSessionForUser(uploadId, userId)
            ?: return Either.Left(ChunkedUploadError.NotFound)
        return Either.Right(session)
    }
}
