package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.storage.DeleteItemUseCase
import com.vaultstadio.application.usecase.storage.RestoreItemUseCase
import com.vaultstadio.application.usecase.storage.TrashItemUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.get as koinGet

internal fun Route.trashStorageRoutes() {
    trashItemRoute()
    restoreItemRoute()
}

private fun Route.trashItemRoute() {
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
}

private fun Route.restoreItemRoute() {
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
}
