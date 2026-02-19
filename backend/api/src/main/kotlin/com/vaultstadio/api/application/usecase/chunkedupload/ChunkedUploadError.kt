/**
 * Errors for chunked upload use cases.
 */

package com.vaultstadio.api.application.usecase.chunkedupload

/**
 * Sealed class representing chunked upload failures (session not found, invalid request, etc.).
 */
sealed class ChunkedUploadError {

    data object NotFound : ChunkedUploadError()

    data class Incomplete(val missingChunks: List<Int>) : ChunkedUploadError()

    data class InvalidRequest(val message: String) : ChunkedUploadError()

    data object InvalidChunkIndex : ChunkedUploadError()
}
