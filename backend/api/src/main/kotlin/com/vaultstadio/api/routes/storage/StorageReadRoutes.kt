package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.storage.GetBreadcrumbsUseCase
import com.vaultstadio.application.usecase.storage.GetItemUseCase
import com.vaultstadio.application.usecase.storage.GetStarredItemsUseCase
import com.vaultstadio.application.usecase.storage.GetTrashItemsUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get as koinGet

internal fun Route.readsStorageRoutes() {
    itemDetailsRoute()
    trashContentsRoute()
    starredItemsRoute()
    breadcrumbsRoute()
}

private fun Route.itemDetailsRoute() {
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
}

private fun Route.trashContentsRoute() {
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
}

private fun Route.starredItemsRoute() {
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
}

private fun Route.breadcrumbsRoute() {
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
