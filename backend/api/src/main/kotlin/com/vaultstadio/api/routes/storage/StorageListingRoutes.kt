package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.PaginatedResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.storage.GetRecentItemsUseCase
import com.vaultstadio.application.usecase.storage.ListFolderUseCase
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get as koinGet

internal fun Route.storageListingRoutes() {
    // List folder contents
    get("/folder/{folderId?}") {
        val listFolderUseCase: ListFolderUseCase = call.application.koinGet()
        val user = call.user!!
        val folderId = call.parameters["folderId"]
        val selection = call.storageSelection(SelectionSurface.FOLDER)

        val query = StorageItemQuery(
            parentId = folderId,
            ownerId = user.id,
            isTrashed = false,
            sortField = selection.sortField,
            sortOrder = selection.sortOrder,
            limit = selection.page.limit,
            offset = selection.page.offset,
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

    // Get recent items
    get("/recent") {
        val getRecentItemsUseCase: GetRecentItemsUseCase = call.application.koinGet()
        val user = call.user!!
        val selection = call.storageSelection(SelectionSurface.RECENT)

        getRecentItemsUseCase(user.id, selection.page.limit).fold(
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
