/**
 * VaultStadio Batch Operations Routes
 *
 * Endpoints for performing operations on multiple items at once.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.application.usecase.storage.CopyItemUseCase
import com.vaultstadio.api.application.usecase.storage.DeleteItemUseCase
import com.vaultstadio.api.application.usecase.storage.DownloadFileUseCase
import com.vaultstadio.api.application.usecase.storage.GetTrashItemsUseCase
import com.vaultstadio.api.application.usecase.storage.MoveItemUseCase
import com.vaultstadio.api.application.usecase.storage.SetStarUseCase
import com.vaultstadio.api.application.usecase.storage.TrashItemUseCase
import com.vaultstadio.core.domain.service.CopyItemInput
import com.vaultstadio.core.domain.service.MoveItemInput
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.koin.ktor.ext.get as koinGet

/**
 * Request for batch delete operation.
 */
@Serializable
data class BatchDeleteRequest(
    val itemIds: List<String>,
    val permanent: Boolean = false,
)

/**
 * Request for batch move operation.
 */
@Serializable
data class BatchMoveRequest(
    val itemIds: List<String>,
    val destinationId: String?,
)

/**
 * Request for batch copy operation.
 */
@Serializable
data class BatchCopyRequest(
    val itemIds: List<String>,
    val destinationId: String?,
)

/**
 * Request for batch star/unstar operation.
 */
@Serializable
data class BatchStarRequest(
    val itemIds: List<String>,
    val starred: Boolean,
)

/**
 * Request for downloading multiple items as ZIP.
 */
@Serializable
data class DownloadZipRequest(
    val itemIds: List<String>,
)

/**
 * Result of a batch operation.
 */
@Serializable
data class BatchResult(
    val successful: Int,
    val failed: Int,
    val errors: List<BatchError> = emptyList(),
)

/**
 * Error detail for batch operations.
 */
@Serializable
data class BatchError(
    val itemId: String,
    val error: String,
)

fun Route.batchRoutes() {
    route("/storage/batch") {
        // Batch delete items
        post("/delete") {
            val deleteItemUseCase: DeleteItemUseCase = call.application.koinGet()
            val trashItemUseCase: TrashItemUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<BatchDeleteRequest>()

            if (request.itemIds.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("EMPTY_LIST", "No items provided"),
                    ),
                )
                return@post
            }

            var successful = 0
            var failed = 0
            val errors = mutableListOf<BatchError>()

            request.itemIds.forEach { itemId ->
                try {
                    val result = if (request.permanent) {
                        deleteItemUseCase(itemId, user.id)
                    } else {
                        trashItemUseCase(itemId, user.id).map { }
                    }

                    result.fold(
                        { error ->
                            failed++
                            errors.add(BatchError(itemId, error.message ?: "Unknown error"))
                        },
                        { successful++ },
                    )
                } catch (e: Exception) {
                    failed++
                    errors.add(BatchError(itemId, e.message ?: "Unknown error"))
                }
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = BatchResult(successful, failed, errors),
                ),
            )
        }

        // Batch move items
        post("/move") {
            val moveItemUseCase: MoveItemUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<BatchMoveRequest>()

            if (request.itemIds.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("EMPTY_LIST", "No items provided"),
                    ),
                )
                return@post
            }

            var successful = 0
            var failed = 0
            val errors = mutableListOf<BatchError>()

            request.itemIds.forEach { itemId ->
                try {
                    val input = MoveItemInput(
                        itemId = itemId,
                        newParentId = request.destinationId,
                        newName = null,
                        userId = user.id,
                    )

                    moveItemUseCase(input).fold(
                        { error ->
                            failed++
                            errors.add(BatchError(itemId, error.message ?: "Unknown error"))
                        },
                        { successful++ },
                    )
                } catch (e: Exception) {
                    failed++
                    errors.add(BatchError(itemId, e.message ?: "Unknown error"))
                }
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = BatchResult(successful, failed, errors),
                ),
            )
        }

        // Batch copy items
        post("/copy") {
            val copyItemUseCase: CopyItemUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<BatchCopyRequest>()

            if (request.itemIds.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("EMPTY_LIST", "No items provided"),
                    ),
                )
                return@post
            }

            var successful = 0
            var failed = 0
            val errors = mutableListOf<BatchError>()

            request.itemIds.forEach { itemId ->
                try {
                    val input = CopyItemInput(
                        itemId = itemId,
                        destinationParentId = request.destinationId,
                        newName = null,
                        userId = user.id,
                    )

                    copyItemUseCase(input).fold(
                        { error ->
                            failed++
                            errors.add(BatchError(itemId, error.message ?: "Unknown error"))
                        },
                        { successful++ },
                    )
                } catch (e: Exception) {
                    failed++
                    errors.add(BatchError(itemId, e.message ?: "Unknown error"))
                }
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = BatchResult(successful, failed, errors),
                ),
            )
        }

        // Batch star/unstar items
        post("/star") {
            val setStarUseCase: SetStarUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<BatchStarRequest>()

            if (request.itemIds.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("EMPTY_LIST", "No items provided"),
                    ),
                )
                return@post
            }

            var successful = 0
            var failed = 0
            val errors = mutableListOf<BatchError>()

            request.itemIds.forEach { itemId ->
                try {
                    setStarUseCase(itemId, user.id, request.starred).fold(
                        { error ->
                            failed++
                            errors.add(BatchError(itemId, error.message ?: "Unknown error"))
                        },
                        { successful++ },
                    )
                } catch (e: Exception) {
                    failed++
                    errors.add(BatchError(itemId, e.message ?: "Unknown error"))
                }
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = BatchResult(successful, failed, errors),
                ),
            )
        }

        // Download multiple items as ZIP
        post("/download-zip") {
            val downloadFileUseCase: DownloadFileUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<DownloadZipRequest>()

            if (request.itemIds.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("EMPTY_LIST", "No items provided"),
                    ),
                )
                return@post
            }

            // Set response headers for ZIP download
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    "download-${System.currentTimeMillis()}.zip",
                ).toString(),
            )

            call.respondOutputStream(
                contentType = ContentType.Application.Zip,
            ) {
                ZipOutputStream(this).use { zipOut ->
                    request.itemIds.forEach { itemId ->
                        try {
                            downloadFileUseCase(itemId, user.id).fold(
                                { /* Skip failed items */ },
                                { (item, stream) ->
                                    // Add file to ZIP
                                    val entry = ZipEntry(item.name)
                                    entry.size = item.size
                                    zipOut.putNextEntry(entry)
                                    stream.copyTo(zipOut)
                                    zipOut.closeEntry()
                                },
                            )
                        } catch (e: Exception) {
                            // Skip failed items
                        }
                    }
                }
            }
        }

        // Empty trash
        post("/empty-trash") {
            val getTrashItemsUseCase: GetTrashItemsUseCase = call.application.koinGet()
            val deleteItemUseCase: DeleteItemUseCase = call.application.koinGet()
            val user = call.user!!

            getTrashItemsUseCase(user.id).fold(
                { error -> throw error },
                { trashedItems ->
                    var successful = 0
                    var failed = 0
                    val errors = mutableListOf<BatchError>()

                    trashedItems.forEach { item ->
                        try {
                            deleteItemUseCase(item.id, user.id).fold(
                                { error ->
                                    failed++
                                    errors.add(BatchError(item.id, error.message ?: "Unknown error"))
                                },
                                { successful++ },
                            )
                        } catch (e: Exception) {
                            failed++
                            errors.add(BatchError(item.id, e.message ?: "Unknown error"))
                        }
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = BatchResult(successful, failed, errors),
                        ),
                    )
                },
            )
        }
    }
}
