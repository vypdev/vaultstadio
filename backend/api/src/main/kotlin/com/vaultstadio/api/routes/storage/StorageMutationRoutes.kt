package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.CopyRequest
import com.vaultstadio.api.dto.CreateFolderRequest
import com.vaultstadio.api.dto.MoveRequest
import com.vaultstadio.api.dto.RenameRequest
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.storage.CopyItemUseCase
import com.vaultstadio.application.usecase.storage.CreateFolderUseCase
import com.vaultstadio.application.usecase.storage.MoveItemUseCase
import com.vaultstadio.application.usecase.storage.RenameItemUseCase
import com.vaultstadio.application.usecase.storage.ToggleStarUseCase
import com.vaultstadio.core.domain.service.CopyItemInput
import com.vaultstadio.core.domain.service.CreateFolderInput
import com.vaultstadio.core.domain.service.MoveItemInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import org.koin.ktor.ext.get as koinGet

internal fun Route.mutationsStorageRoutes() {
    createFolderRoute()
    renameItemRoute()
    moveItemRoute()
    copyItemRoute()
    toggleStarRoute()
}

private fun Route.createFolderRoute() {
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
}

private fun Route.renameItemRoute() {
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
}

private fun Route.moveItemRoute() {
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
}

private fun Route.copyItemRoute() {
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
}

private fun Route.toggleStarRoute() {
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
}
