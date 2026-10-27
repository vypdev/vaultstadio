/**
 * VaultStadio Share Routes
 */

package com.vaultstadio.api.routes.share

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.share.GetSharesByItemUseCase
import com.vaultstadio.application.usecase.share.GetSharesByUserUseCase
import com.vaultstadio.application.usecase.share.GetSharesSharedWithUserUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get as koinGet

internal fun Route.ownerShareListRoutes() {
    // List user's shares (created by user)
    get {
        val getSharesByUserUseCase: GetSharesByUserUseCase = call.application.koinGet()
        val user = call.user!!
        val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: true

        getSharesByUserUseCase(user.id, activeOnly).fold(
            { error -> throw error },
            { shares ->
                val baseUrl = getBaseUrl(call)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = shares.map { it.toResponse(baseUrl) },
                    ),
                )
            },
        )
    }
    // List shares shared with the current user
    get("/shared-with-me") {
        val getSharesSharedWithUserUseCase: GetSharesSharedWithUserUseCase = call.application.koinGet()
        val user = call.user!!
        val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: true

        getSharesSharedWithUserUseCase(user.id, activeOnly).fold(
            { error -> throw error },
            { shares ->
                val baseUrl = getBaseUrl(call)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = shares.map { it.toResponse(baseUrl) },
                    ),
                )
            },
        )
    }
}

internal fun Route.ownerItemShareRoute() {
    // Get shares for an item
    get("/item/{itemId}") {
        val getSharesByItemUseCase: GetSharesByItemUseCase = call.application.koinGet()
        val user = call.user!!
        val itemId = call.parameters["itemId"]!!

        getSharesByItemUseCase(itemId, user.id).fold(
            { error -> throw error },
            { shares ->
                val baseUrl = getBaseUrl(call)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = shares.map { it.toResponse(baseUrl) },
                    ),
                )
            },
        )
    }
}
