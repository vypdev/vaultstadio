/**
 * VaultStadio Folder Upload Routes
 *
 * Endpoints for uploading entire folders with their structure preserved.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UploadFileInput
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.core.use
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet

/**
 * Result of a folder upload.
 */
@Serializable
data class FolderUploadResult(
    val uploadedFiles: Int,
    val createdFolders: Int,
    val errors: List<FolderUploadError>,
)

/**
 * Error from folder upload.
 */
@Serializable
data class FolderUploadError(
    val path: String,
    val error: String,
)

fun Route.folderUploadRoutes() {
    route("/storage") {
        // Upload a folder with structure
        post("/upload-folder") {
            val storageService: StorageService = call.application.koinGet()
            val user = call.user!!

            val multipart = call.receiveMultipart(formFieldLimit = 512L * 1024 * 1024) // 512 MB max per part
            var parentId: String? = null
            val files = mutableListOf<FileWithPath>()

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "parentId" -> parentId = part.value.takeIf { it.isNotBlank() }
                        }
                    }
                    is PartData.FileItem -> {
                        val relativePath = part.name ?: part.originalFileName ?: "unknown"
                        val fileName = part.originalFileName ?: "unknown"
                        val bytes = part.streamProvider().use { it.readBytes() }
                        val contentType = part.contentType?.toString() ?: "application/octet-stream"

                        files.add(
                            FileWithPath(
                                relativePath = relativePath,
                                fileName = fileName,
                                bytes = bytes,
                                contentType = contentType,
                            ),
                        )
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (files.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("NO_FILES", "No files provided"),
                    ),
                )
                return@post
            }

            // Process the folder structure
            var uploadedFiles = 0
            var createdFolders = 0
            val errors = mutableListOf<FolderUploadError>()
            val createdFolderIds = mutableMapOf<String, String>()

            // Sort files by path depth to create parent folders first
            val sortedFiles = files.sortedBy { it.relativePath.count { c -> c == '/' || c == '\\' } }

            sortedFiles.forEach { file ->
                try {
                    // Parse the path to determine parent folders
                    val pathParts = file.relativePath.split("/", "\\").filter { it.isNotBlank() }

                    // Create necessary parent folders
                    var currentParentId = parentId
                    if (pathParts.size > 1) {
                        // We have folder(s) in the path
                        val folderPath = pathParts.dropLast(1)

                        var folderPathKey = ""
                        folderPath.forEach { folderName ->
                            folderPathKey = if (folderPathKey.isEmpty()) folderName else "$folderPathKey/$folderName"

                            if (!createdFolderIds.containsKey(folderPathKey)) {
                                // Try to get or create this folder
                                val existingFolder = storageService.getOrCreateFolder(
                                    name = folderName,
                                    parentId = currentParentId,
                                    ownerId = user.id,
                                )

                                existingFolder.fold(
                                    { error ->
                                        errors.add(
                                            FolderUploadError(
                                                folderPathKey,
                                                "Failed to get/create folder: ${error.message}",
                                            ),
                                        )
                                    },
                                    { result ->
                                        createdFolderIds[folderPathKey] = result.folder.id
                                        if (result.created) {
                                            createdFolders++
                                        }
                                        currentParentId = result.folder.id
                                    },
                                )
                            } else {
                                currentParentId = createdFolderIds[folderPathKey]
                            }
                        }
                    }

                    // Upload the file
                    val uploadInput = UploadFileInput(
                        name = file.fileName,
                        mimeType = file.contentType,
                        size = file.bytes.size.toLong(),
                        parentId = currentParentId,
                        ownerId = user.id,
                        inputStream = file.bytes.inputStream(),
                    )

                    storageService.uploadFile(uploadInput).fold(
                        { error ->
                            errors.add(FolderUploadError(file.relativePath, error.message ?: "Upload failed"))
                        },
                        { uploadedFiles++ },
                    )
                } catch (e: Exception) {
                    errors.add(FolderUploadError(file.relativePath, e.message ?: "Unknown error"))
                }
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = FolderUploadResult(uploadedFiles, createdFolders, errors),
                ),
            )
        }
    }
}

private data class FileWithPath(
    val relativePath: String,
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String,
)
