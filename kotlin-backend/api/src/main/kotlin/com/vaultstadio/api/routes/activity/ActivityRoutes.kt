/**
 * VaultStadio Activity Routes
 */

package com.vaultstadio.api.routes.activity

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.core.domain.repository.ActivityRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.get as koinGet

fun Route.activityRoutes() {
    route("/activity") {
        // Get recent activities
        get {
            val activityRepository: ActivityRepository = call.application.koinGet()
            val user = call.user!!
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

            activityRepository.getRecentByUser(user.id, limit).fold(
                { error -> throw error },
                { activities ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = activities.map { it.toResponse() },
                        ),
                    )
                },
            )
        }

        // Get activities for an item
        get("/item/{itemId}") {
            val activityRepository: ActivityRepository = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

            activityRepository.getRecentByItem(itemId, limit).fold(
                { error -> throw error },
                { activities ->
                    // Filter to only show user's activities or public activities
                    val filtered = activities.filter { it.userId == user.id || it.userId == null }
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = filtered.map { it.toResponse() },
                        ),
                    )
                },
            )
        }
    }
}
