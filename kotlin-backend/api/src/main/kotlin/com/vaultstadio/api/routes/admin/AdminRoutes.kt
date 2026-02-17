/**
 * VaultStadio Admin Routes
 */

package com.vaultstadio.api.routes.admin

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.PaginatedResponse
import com.vaultstadio.api.dto.toAdminResponse
import com.vaultstadio.core.domain.model.UserRole
import com.vaultstadio.core.domain.repository.ActivityRepository
import com.vaultstadio.core.domain.repository.UserQuery
import com.vaultstadio.core.domain.service.UserService
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
            val userService: UserService = call.application.koinGet()
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

            userService.listUsers(user.id, query).fold(
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
            val activityRepository: ActivityRepository = call.application.koinGet()
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

            activityRepository.getStatistics().fold(
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
            val userService: UserService = call.application.koinGet()
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

            userService.updateQuota(userId, quotaBytes, adminUser.id).fold(
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
            val userService: UserService = call.application.koinGet()
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

            userService.deleteUser(userId, adminUser.id).fold(
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
