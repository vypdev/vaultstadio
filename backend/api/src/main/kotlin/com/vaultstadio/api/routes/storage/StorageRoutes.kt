/**
 * VaultStadio Storage Routes
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.CopyRequest
import com.vaultstadio.api.dto.CreateFolderRequest
import com.vaultstadio.api.dto.MoveRequest
import com.vaultstadio.api.dto.PaginatedResponse
import com.vaultstadio.api.dto.RenameRequest
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.storage.CopyItemUseCase
import com.vaultstadio.application.usecase.storage.CreateFolderUseCase
import com.vaultstadio.application.usecase.storage.DeleteItemUseCase
import com.vaultstadio.application.usecase.storage.DownloadFileUseCase
import com.vaultstadio.application.usecase.storage.GetBreadcrumbsUseCase
import com.vaultstadio.application.usecase.storage.GetItemUseCase
import com.vaultstadio.application.usecase.storage.GetRecentItemsUseCase
import com.vaultstadio.application.usecase.storage.GetStarredItemsUseCase
import com.vaultstadio.application.usecase.storage.GetTrashItemsUseCase
import com.vaultstadio.application.usecase.storage.ListFolderUseCase
import com.vaultstadio.application.usecase.storage.MoveItemUseCase
import com.vaultstadio.application.usecase.storage.RenameItemUseCase
import com.vaultstadio.application.usecase.storage.RestoreItemUseCase
import com.vaultstadio.application.usecase.storage.ToggleStarUseCase
import com.vaultstadio.application.usecase.storage.TrashItemUseCase
import com.vaultstadio.application.usecase.storage.UploadFileUseCase
import com.vaultstadio.core.domain.service.CopyItemInput
import com.vaultstadio.core.domain.service.CreateFolderInput
import com.vaultstadio.core.domain.service.MoveItemInput
import com.vaultstadio.core.domain.service.UploadFileInput
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.repository.SortField
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.io.ByteArrayInputStream
import org.koin.ktor.ext.get as koinGet

fun Route.storageRoutes() {
    route("/storage") {
        // List folder contents
        get("/folder/{folderId?}") {
            val listFolderUseCase: ListFolderUseCase = call.application.koinGet()
            val user = call.user!!
            val folderId = call.parameters["folderId"]
            val sortBy = call.request.queryParameters["sortBy"] ?: "name"
            val sortOrder = call.request.queryParameters["sortOrder"] ?: "asc"
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val query = StorageItemQuery(
                parentId = folderId,
                ownerId = user.id,
                isTrashed = false,
                sortField = SortField.valueOf(sortBy.uppercase()),
                sortOrder = SortOrder.valueOf(sortOrder.uppercase()),
                limit = limit,
                offset = offset,
            )

            listFolderUseCase(folderId, user.id, query).fold(
                { error -> throw error },
                { result ->
                    val response = PaginatedResponse(
                        items = result.items.map { it.toResponse() },
                        total = result.total,
                        page = result.currentPage,
                        pageSize = result.limit,
                        totalPages = result.totalPages,
                        hasMore = result.hasMore,
                    )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = response),
                    )
                },
            )
        }

        // Get item details
        get("/item/{itemId}") {
            val getItemUseCase: GetItemUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            getItemUseCase(itemId, user.id).fold(
                { error -> throw error },
                { item ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = item.toResponse()),
                    )
                },
            )
        }

        // Create folder
        post("/folder") {
            val createFolderUseCase: CreateFolderUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<CreateFolderRequest>()

            val input = CreateFolderInput(
                name = request.name,
                parentId = request.parentId,
                ownerId = user.id,
            )

            createFolderUseCase(input).fold(
                { error -> throw error },
                { folder ->
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(success = true, data = folder.toResponse()),
                    )
                },
            )
        }

        // Upload file (parentId from query or from multipart form field)
        post("/upload") {
            val uploadFileUseCase: UploadFileUseCase = call.application.koinGet()
            val user = call.user!!
            var parentId = call.request.queryParameters["parentId"]

            val multipart = call.receiveMultipart(formFieldLimit = 512L * 1024 * 1024) // 512 MB max per part
            var fileName: String? = null
            var fileBytes: ByteArray? = null
            var mimeType: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "parentId") {
                            parentId = part.value.takeIf { it.isNotBlank() }
                        }
                    }
                    is PartData.FileItem -> {
                        fileName = part.originalFileName
                        mimeType = part.contentType?.toString()
                        fileBytes = part.streamProvider().readBytes()
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (fileName == null || fileBytes == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_REQUEST", "No file provided"),
                    ),
                )
                return@post
            }

            val input = UploadFileInput(
                name = fileName!!,
                parentId = parentId,
                ownerId = user.id,
                mimeType = mimeType,
                size = fileBytes!!.size.toLong(),
                inputStream = ByteArrayInputStream(fileBytes),
            )

            uploadFileUseCase(input).fold(
                { error -> throw error },
                { file ->
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(success = true, data = file.toResponse()),
                    )
                },
            )
        }

        // Download file
        get("/download/{itemId}") {
            val downloadFileUseCase: DownloadFileUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            downloadFileUseCase(itemId, user.id).fold(
                { error -> throw error },
                { (item, stream) ->
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
                            item.name,
                        ).toString(),
                    )
                    call.respondOutputStream(
                        contentType = item.mimeType?.let { ContentType.parse(it) }
                            ?: ContentType.Application.OctetStream,
                        contentLength = item.size,
                    ) {
                        stream.copyTo(this)
                    }
                },
            )
        }

        // Rename item
        patch("/item/{itemId}/rename") {
            val renameItemUseCase: RenameItemUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!
            val request = call.receive<RenameRequest>()

            renameItemUseCase(itemId, request.name, user.id).fold(
                { error -> throw error },
                { item ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = item.toResponse()),
                    )
                },
            )
        }

        // Move item
        post("/item/{itemId}/move") {
            val moveItemUseCase: MoveItemUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!
            val request = call.receive<MoveRequest>()

            val input = MoveItemInput(
                itemId = itemId,
                newParentId = request.destinationId,
                newName = request.newName,
                userId = user.id,
            )

            moveItemUseCase(input).fold(
                { error -> throw error },
                { item ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = item.toResponse()),
                    )
                },
            )
        }

        // Copy item
        post("/item/{itemId}/copy") {
            val copyItemUseCase: CopyItemUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!
            val request = call.receive<CopyRequest>()

            val input = CopyItemInput(
                itemId = itemId,
                destinationParentId = request.destinationId,
                newName = request.newName,
                userId = user.id,
            )

            copyItemUseCase(input).fold(
                { error -> throw error },
                { item ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = item.toResponse()),
                    )
                },
            )
        }

        // Toggle star
        post("/item/{itemId}/star") {
            val toggleStarUseCase: ToggleStarUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            toggleStarUseCase(itemId, user.id).fold(
                { error -> throw error },
                { item ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = item.toResponse()),
                    )
                },
            )
        }

        // Trash item
        delete("/item/{itemId}") {
            val deleteItemUseCase: DeleteItemUseCase = call.application.koinGet()
            val trashItemUseCase: TrashItemUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!
            val permanent = call.request.queryParameters["permanent"]?.toBoolean() ?: false

            if (permanent) {
                deleteItemUseCase(itemId, user.id).fold(
                    { error -> throw error },
                    {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse<Unit>(success = true),
                        )
                    },
                )
            } else {
                trashItemUseCase(itemId, user.id).fold(
                    { error -> throw error },
                    { item ->
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(success = true, data = item.toResponse()),
                        )
                    },
                )
            }
        }

        // Restore from trash
        post("/item/{itemId}/restore") {
            val restoreItemUseCase: RestoreItemUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            restoreItemUseCase(itemId, user.id).fold(
                { error -> throw error },
                { item ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = item.toResponse()),
                    )
                },
            )
        }

        // Get trash contents
        get("/trash") {
            val getTrashItemsUseCase: GetTrashItemsUseCase = call.application.koinGet()
            val user = call.user!!

            getTrashItemsUseCase(user.id).fold(
                { error -> throw error },
                { items ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = items.map { it.toResponse() },
                        ),
                    )
                },
            )
        }

        // Get starred items
        get("/starred") {
            val getStarredItemsUseCase: GetStarredItemsUseCase = call.application.koinGet()
            val user = call.user!!

            getStarredItemsUseCase(user.id).fold(
                { error -> throw error },
                { items ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = items.map { it.toResponse() },
                        ),
                    )
                },
            )
        }

        // Get recent items
        get("/recent") {
            val getRecentItemsUseCase: GetRecentItemsUseCase = call.application.koinGet()
            val user = call.user!!
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

            getRecentItemsUseCase(user.id, limit).fold(
                { error -> throw error },
                { items ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = items.map { it.toResponse() },
                        ),
                    )
                },
            )
        }

        // Get breadcrumbs
        get("/item/{itemId}/breadcrumbs") {
            val getBreadcrumbsUseCase: GetBreadcrumbsUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            getBreadcrumbsUseCase(itemId, user.id).fold(
                { error -> throw error },
                { items ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = items.map { it.toResponse() },
                        ),
                    )
                },
            )
        }
    }
}
