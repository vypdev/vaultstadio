/**
 * VaultStadio Chunked Upload Routes
 *
 * Handles large file uploads by splitting them into chunks.
 * This allows uploading files up to 60GB+ without memory issues.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.api.service.UploadSession
import com.vaultstadio.api.service.UploadSessionManager
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UploadFileInput
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
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.SequenceInputStream
import java.util.Collections
import java.util.UUID
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

fun Route.chunkedUploadRoutes() {
    route("/storage/upload") {
        // Initialize a new chunked upload
        post("/init") {
            val storageService: StorageService = call.application.koinGet()
            val uploadSessionManager: UploadSessionManager = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<InitUploadRequest>()

            // Validate request
            if (request.totalSize <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_SIZE", "Total size must be positive"),
                    ),
                )
                return@post
            }

            if (request.fileName.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_FILENAME", "File name is required"),
                    ),
                )
                return@post
            }

            // Calculate number of chunks
            val chunkSize = request.chunkSize.coerceIn(1 * 1024 * 1024, 100 * 1024 * 1024) // 1MB - 100MB
            val totalChunks = ((request.totalSize + chunkSize - 1) / chunkSize).toInt()

            // Create upload session
            val uploadId = UUID.randomUUID().toString()
            val tempDir = System.getProperty("java.io.tmpdir") + "/vaultstadio-uploads/$uploadId"
            File(tempDir).mkdirs()

            val session = UploadSession(
                id = uploadId,
                userId = user.id,
                fileName = request.fileName,
                totalSize = request.totalSize,
                mimeType = request.mimeType,
                parentId = request.parentId,
                chunkSize = chunkSize,
                totalChunks = totalChunks,
                createdAt = Clock.System.now(),
                tempDir = tempDir,
            )

            uploadSessionManager.createSession(session)

            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    data = InitUploadResponse(
                        uploadId = uploadId,
                        chunkSize = chunkSize,
                        totalChunks = totalChunks,
                    ),
                ),
            )
        }

        // Upload a chunk
        post("/{uploadId}/chunk/{chunkIndex}") {
            val uploadSessionManager: UploadSessionManager = call.application.koinGet()
            val user = call.user!!
            val uploadId = call.parameters["uploadId"]!!
            val chunkIndex = call.parameters["chunkIndex"]?.toIntOrNull()

            if (chunkIndex == null || chunkIndex < 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_CHUNK_INDEX", "Valid chunk index is required"),
                    ),
                )
                return@post
            }

            val session = uploadSessionManager.getSessionForUser(uploadId, user.id)
            if (session == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("UPLOAD_NOT_FOUND", "Upload session not found"),
                    ),
                )
                return@post
            }

            if (session.userId != user.id) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("ACCESS_DENIED", "You don't own this upload"),
                    ),
                )
                return@post
            }

            if (chunkIndex >= session.totalChunks) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_CHUNK_INDEX", "Chunk index out of range"),
                    ),
                )
                return@post
            }

            // Receive chunk data
            val multipart = call.receiveMultipart(formFieldLimit = 512L * 1024 * 1024) // 512 MB max per part
            var chunkBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        chunkBytes = part.streamProvider().readBytes()
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (chunkBytes == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("NO_DATA", "Chunk data is required"),
                    ),
                )
                return@post
            }

            // Save chunk to temp file
            val chunkFile = File("${session.tempDir}/chunk_$chunkIndex")
            FileOutputStream(chunkFile).use { it.write(chunkBytes) }

            // Mark chunk as received
            session.receivedChunks.add(chunkIndex)

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = UploadStatusResponse(
                        uploadId = session.id,
                        fileName = session.fileName,
                        totalSize = session.totalSize,
                        uploadedBytes = session.uploadedBytes,
                        progress = session.progress,
                        receivedChunks = session.receivedChunks.toList().sorted(),
                        missingChunks = (0 until session.totalChunks)
                            .filter { it !in session.receivedChunks },
                        isComplete = session.isComplete,
                    ),
                ),
            )
        }

        // Get upload status
        get("/{uploadId}/status") {
            val uploadSessionManager: UploadSessionManager = call.application.koinGet()
            val user = call.user!!
            val uploadId = call.parameters["uploadId"]!!

            val session = uploadSessionManager.getSessionForUser(uploadId, user.id)
            if (session == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("UPLOAD_NOT_FOUND", "Upload session not found or access denied"),
                    ),
                )
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = UploadStatusResponse(
                        uploadId = session.id,
                        fileName = session.fileName,
                        totalSize = session.totalSize,
                        uploadedBytes = session.uploadedBytes,
                        progress = session.progress,
                        receivedChunks = session.receivedChunks.toList().sorted(),
                        missingChunks = (0 until session.totalChunks)
                            .filter { it !in session.receivedChunks },
                        isComplete = session.isComplete,
                    ),
                ),
            )
        }

        // Complete the upload (merge chunks and save)
        post("/{uploadId}/complete") {
            val storageService: StorageService = call.application.koinGet()
            val uploadSessionManager: UploadSessionManager = call.application.koinGet()
            val user = call.user!!
            val uploadId = call.parameters["uploadId"]!!

            val session = uploadSessionManager.getSessionForUser(uploadId, user.id)
            if (session == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("UPLOAD_NOT_FOUND", "Upload session not found or access denied"),
                    ),
                )
                return@post
            }

            if (!session.isComplete) {
                val missing = (0 until session.totalChunks).filter { it !in session.receivedChunks }
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError(
                            "INCOMPLETE_UPLOAD",
                            "Missing chunks: ${missing.take(
                                10,
                            ).joinToString(", ")}${if (missing.size > 10) "..." else ""}",
                        ),
                    ),
                )
                return@post
            }

            try {
                // Create a SequenceInputStream that reads chunks in order
                val chunkStreams = (0 until session.totalChunks).map { index ->
                    FileInputStream(File("${session.tempDir}/chunk_$index"))
                }
                val combinedStream = SequenceInputStream(Collections.enumeration(chunkStreams))

                // Upload to storage service
                val input = UploadFileInput(
                    name = session.fileName,
                    parentId = session.parentId,
                    ownerId = user.id,
                    mimeType = session.mimeType,
                    size = session.totalSize,
                    inputStream = combinedStream,
                )

                storageService.uploadFile(input).fold(
                    { error ->
                        // Clean up temp files on error
                        cleanupSession(session)
                        throw error
                    },
                    { file ->
                        // Clean up temp files on success (manager handles this)
                        uploadSessionManager.removeSession(uploadId)

                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(success = true, data = file.toResponse()),
                        )
                    },
                )
            } catch (e: Exception) {
                uploadSessionManager.removeSession(uploadId)
                throw e
            }
        }

        // Cancel/abort an upload
        delete("/{uploadId}") {
            val uploadSessionManager: UploadSessionManager = call.application.koinGet()
            val user = call.user!!
            val uploadId = call.parameters["uploadId"]!!

            val session = uploadSessionManager.getSessionForUser(uploadId, user.id)
            if (session == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("UPLOAD_NOT_FOUND", "Upload session not found or access denied"),
                    ),
                )
                return@delete
            }

            uploadSessionManager.removeSession(uploadId)

            call.respond(
                HttpStatusCode.OK,
                ApiResponse<Unit>(success = true),
            )
        }
    }
}

/**
 * Clean up temporary files for an upload session.
 * Note: This is now handled by UploadSessionManager.removeSession()
 */
private fun cleanupSession(session: UploadSession) {
    try {
        val tempDir = File(session.tempDir)
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    } catch (e: Exception) {
        // Log but don't throw
        println("Failed to clean up upload session ${session.id}: ${e.message}")
    }
}
