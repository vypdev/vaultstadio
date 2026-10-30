/**
 * VaultStadio Share Routes
 */

package com.vaultstadio.api.routes.share

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.CreateShareRequest
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.share.CreateShareUseCase
import com.vaultstadio.application.usecase.share.DeleteShareUseCase
import com.vaultstadio.core.domain.service.CreateShareInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.get as koinGet

internal fun Route.ownerShareCreateRoute() {
    // Create share
    post {
        val createShareUseCase: CreateShareUseCase = call.application.koinGet()
        val user = call.user!!
        val request = call.receive<CreateShareRequest>()

        val input = CreateShareInput(
            itemId = request.itemId,
            userId = user.id,
            expirationDays = request.expirationDays,
            password = request.password,
            maxDownloads = request.maxDownloads,
        )

        val baseUrl = getBaseUrl(call)
        createShareUseCase(input).fold(
            { error -> throw error },
            { share ->
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(success = true, data = share.toResponse(baseUrl)),
                )
            },
        )
    }
}

internal fun Route.ownerShareDeleteRoute() {
    // Delete share
    delete("/{shareId}") {
        val deleteShareUseCase: DeleteShareUseCase = call.application.koinGet()
        val user = call.user!!
        val shareId = call.parameters["shareId"]!!

        deleteShareUseCase(shareId, user.id).fold(
            { error -> throw error },
            {
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse<Unit>(success = true),
                )
            },
        )
    }
}
