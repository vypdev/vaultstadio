/**
 * VaultStadio Chunked Upload Routes
 *
 * Handles large file uploads by splitting them into chunks.
 * This allows uploading files up to 60GB+ without memory issues.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.application.usecase.chunkedupload.ChunkedUploadError
import com.vaultstadio.api.application.usecase.chunkedupload.CancelChunkedUploadUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.CompleteChunkedUploadUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.GetChunkedUploadStatusUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.InitChunkedUploadUseCase
import com.vaultstadio.api.application.usecase.chunkedupload.UploadChunkUseCase
import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.api.service.UploadSession
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet

/**
 * Request to initialize a chunked upload.
 */
@Serializable
data class InitUploadRequest(
    val fileName: String,
    val totalSize: Long,
    val mimeType: String? = null,
    val parentId: String? = null,
    val chunkSize: Long = 10 * 1024 * 1024, // 10MB default
)

/**
 * Response for upload initialization.
 */
@Serializable
data class InitUploadResponse(
    val uploadId: String,
    val chunkSize: Long,
    val totalChunks: Int,
)

/**
 * Response for upload status.
 */
@Serializable
data class UploadStatusResponse(
    val uploadId: String,
    val fileName: String,
    val totalSize: Long,
    val uploadedBytes: Long,
    val progress: Float,
    val receivedChunks: List<Int>,
    val missingChunks: List<Int>,
    val isComplete: Boolean,
)

private fun UploadSession.toStatusResponse() = UploadStatusResponse(
    uploadId = id,
    fileName = fileName,
    totalSize = totalSize,
    uploadedBytes = uploadedBytes,
    progress = progress,
    receivedChunks = receivedChunks.toList().sorted(),
    missingChunks = (0 until totalChunks).filter { it !in receivedChunks },
    isComplete = isComplete,
)

private fun ChunkedUploadError.toApiError(): Pair<HttpStatusCode, ApiError> = when (this) {
    is ChunkedUploadError.NotFound -> HttpStatusCode.NotFound to ApiError("UPLOAD_NOT_FOUND", "Upload session not found")
    is ChunkedUploadError.Incomplete -> HttpStatusCode.BadRequest to ApiError("INCOMPLETE_UPLOAD", "Missing chunks: ${missingChunks.take(10).joinToString(", ")}${if (missingChunks.size > 10) "..." else ""}")
    is ChunkedUploadError.InvalidRequest -> HttpStatusCode.BadRequest to ApiError("INVALID_REQUEST", message)
    is ChunkedUploadError.InvalidChunkIndex -> HttpStatusCode.BadRequest to ApiError("INVALID_CHUNK_INDEX", "Valid chunk index is required")
}

fun Route.chunkedUploadRoutes() {
    route("/storage/upload") {
        post("/init") {
            val initChunkedUploadUseCase: InitChunkedUploadUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<InitUploadRequest>()

            initChunkedUploadUseCase(
                fileName = request.fileName,
                totalSize = request.totalSize,
                userId = user.id,
                mimeType = request.mimeType,
                parentId = request.parentId,
                chunkSize = request.chunkSize,
            ).fold(
                { error ->
                    val (status, apiError) = error.toApiError()
                    call.respond(status, ApiResponse<Unit>(success = false, error = apiError))
                },
                { result ->
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(
                            success = true,
                            data = InitUploadResponse(
                                uploadId = result.uploadId,
                                chunkSize = result.chunkSize,
                                totalChunks = result.totalChunks,
                            ),
                        ),
                    )
                },
            )
        }

        post("/{uploadId}/chunk/{chunkIndex}") {
            val uploadChunkUseCase: UploadChunkUseCase = call.application.koinGet()
            val user = call.user!!
            val uploadId = call.parameters["uploadId"]!!
            val chunkIndex = call.parameters["chunkIndex"]?.toIntOrNull()

            if (chunkIndex == null || chunkIndex < 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = ApiError("INVALID_CHUNK_INDEX", "Valid chunk index is required")),
                )
                return@post
            }

            val multipart = call.receiveMultipart(formFieldLimit = 512L * 1024 * 1024)
            var chunkBytes: ByteArray? = null
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> chunkBytes = part.streamProvider().readBytes()
                    else -> {}
                }
                part.dispose()
            }
            if (chunkBytes == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = ApiError("NO_DATA", "Chunk data is required")),
                )
                return@post
            }

            uploadChunkUseCase(uploadId, user.id, chunkIndex, chunkBytes).fold(
                { error ->
                    val (status, apiError) = error.toApiError()
                    call.respond(status, ApiResponse<Unit>(success = false, error = apiError))
                },
                { session ->
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = session.toStatusResponse()))
                },
            )
        }

        get("/{uploadId}/status") {
            val getChunkedUploadStatusUseCase: GetChunkedUploadStatusUseCase = call.application.koinGet()
            val user = call.user!!
            val uploadId = call.parameters["uploadId"]!!

            getChunkedUploadStatusUseCase(uploadId, user.id).fold(
                { error ->
                    val (status, apiError) = error.toApiError()
                    call.respond(status, ApiResponse<Unit>(success = false, error = apiError))
                },
                { session ->
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = session.toStatusResponse()))
                },
            )
        }

        post("/{uploadId}/complete") {
            val completeChunkedUploadUseCase: CompleteChunkedUploadUseCase = call.application.koinGet()
            val user = call.user!!
            val uploadId = call.parameters["uploadId"]!!

            completeChunkedUploadUseCase(uploadId, user.id).fold(
                { error -> throw error },
                { file ->
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = file.toResponse()))
                },
            )
        }

        delete("/{uploadId}") {
            val cancelChunkedUploadUseCase: CancelChunkedUploadUseCase = call.application.koinGet()
            val user = call.user!!
            val uploadId = call.parameters["uploadId"]!!

            cancelChunkedUploadUseCase(uploadId, user.id).fold(
                { error ->
                    val (status, apiError) = error.toApiError()
                    call.respond(status, ApiResponse<Unit>(success = false, error = apiError))
                },
                { call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true)) },
            )
        }
    }
}
