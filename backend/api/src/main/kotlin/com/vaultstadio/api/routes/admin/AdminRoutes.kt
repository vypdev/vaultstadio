/**
 * VaultStadio Admin Routes
 */

package com.vaultstadio.api.routes.admin

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.PaginatedResponse
import com.vaultstadio.api.dto.toAdminResponse
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.application.usecase.admin.DeleteUserUseCase
import com.vaultstadio.application.usecase.admin.GetAdminStatisticsUseCase
import com.vaultstadio.application.usecase.admin.ListUsersUseCase
import com.vaultstadio.application.usecase.admin.UpdateQuotaUseCase
import com.vaultstadio.domain.auth.repository.UserQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import org.koin.ktor.ext.get as koinGet

fun Route.adminRoutes() {
    route("/admin") {
        // List all users
        get("/users") {
            val listUsersUseCase: ListUsersUseCase = call.application.koinGet()
            val user = call.user
            if (user?.role != UserRole.ADMIN) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("ACCESS_DENIED", "Admin access required"),
                    ),
                )
                return@get
            }

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val query = UserQuery(
                limit = limit,
                offset = offset,
            )

            listUsersUseCase(user.id, query).fold(
                { error -> throw error },
                { result ->
                    val response = PaginatedResponse(
                        items = result.items.map { it.toAdminResponse() },
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

        // Get system statistics
        get("/statistics") {
            val getAdminStatisticsUseCase: GetAdminStatisticsUseCase = call.application.koinGet()
            val user = call.user
            if (user?.role != UserRole.ADMIN) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("ACCESS_DENIED", "Admin access required"),
                    ),
                )
                return@get
            }

            getAdminStatisticsUseCase().fold(
                { error -> throw error },
                { stats ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = stats),
                    )
                },
            )
        }

        // Update user quota
        patch("/users/{userId}/quota") {
            val updateQuotaUseCase: UpdateQuotaUseCase = call.application.koinGet()
            val adminUser = call.user
            if (adminUser?.role != UserRole.ADMIN) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("ACCESS_DENIED", "Admin access required"),
                    ),
                )
                return@patch
            }

            val userId = call.parameters["userId"]!!
            val quotaBytes = call.request.queryParameters["quotaBytes"]?.toLongOrNull()

            updateQuotaUseCase(userId, quotaBytes, adminUser.id).fold(
                { error -> throw error },
                { updatedUser ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = updatedUser.toAdminResponse()),
                    )
                },
            )
        }

        // Delete user
        delete("/users/{userId}") {
            val deleteUserUseCase: DeleteUserUseCase = call.application.koinGet()
            val adminUser = call.user
            if (adminUser?.role != UserRole.ADMIN) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("ACCESS_DENIED", "Admin access required"),
                    ),
                )
                return@delete
            }

            val userId = call.parameters["userId"]!!

            if (userId == adminUser.id) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_OPERATION", "Cannot delete your own account"),
                    ),
                )
                return@delete
            }

            deleteUserUseCase(userId, adminUser.id).fold(
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
}
