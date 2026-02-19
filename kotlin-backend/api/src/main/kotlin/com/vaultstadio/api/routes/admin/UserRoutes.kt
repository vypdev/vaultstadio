/**
 * VaultStadio User Routes
 */

package com.vaultstadio.api.routes.admin

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.ChangePasswordRequest
import com.vaultstadio.api.dto.UpdateProfileRequest
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.api.application.usecase.user.ChangePasswordUseCase
import com.vaultstadio.api.application.usecase.user.GetQuotaUseCase
import com.vaultstadio.api.application.usecase.user.GetUserInfoUseCase
import com.vaultstadio.api.application.usecase.user.LogoutAllUseCase
import com.vaultstadio.api.application.usecase.user.UpdateUserUseCase
import com.vaultstadio.core.domain.service.UpdateUserInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.get as koinGet

fun Route.userRoutes() {
    route("/user") {
        // Get current user
        get("/me") {
            val user = call.user!!
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(success = true, data = user.toResponse()),
            )
        }

        // Get user quota
        get("/me/quota") {
            val getQuotaUseCase: GetQuotaUseCase = call.application.koinGet()
            val user = call.user!!

            getQuotaUseCase(user.id).fold(
                { error -> throw error },
                { quota ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = quota.toResponse()),
                    )
                },
            )
        }

        // Update profile
        patch("/me") {
            val updateUserUseCase: UpdateUserUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<UpdateProfileRequest>()

            val input = UpdateUserInput(
                userId = user.id,
                username = request.username,
                avatarUrl = request.avatarUrl,
            )

            updateUserUseCase(input).fold(
                { error -> throw error },
                { updated ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = updated.toResponse()),
                    )
                },
            )
        }

        // Change password
        post("/me/password") {
            val changePasswordUseCase: ChangePasswordUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<ChangePasswordRequest>()

            changePasswordUseCase(
                user.id,
                request.currentPassword,
                request.newPassword,
            ).fold(
                { error -> throw error },
                {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<Unit>(success = true),
                    )
                },
            )
        }

        // Logout from all sessions
        post("/me/logout-all") {
            val logoutAllUseCase: LogoutAllUseCase = call.application.koinGet()
            val user = call.user!!

            logoutAllUseCase(user.id).fold(
                { error -> throw error },
                {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<Unit>(success = true),
                    )
                },
            )
        }

        // Get public user info
        get("/{userId}") {
            val getUserInfoUseCase: GetUserInfoUseCase = call.application.koinGet()
            val userId = call.parameters["userId"]!!

            getUserInfoUseCase(userId).fold(
                { error -> throw error },
                { userInfo ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = userInfo.toResponse()),
                    )
                },
            )
        }
    }
}
