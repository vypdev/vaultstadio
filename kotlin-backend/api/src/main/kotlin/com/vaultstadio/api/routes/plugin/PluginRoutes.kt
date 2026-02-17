/**
 * VaultStadio Plugin Routes
 *
 * Includes management endpoints and dynamic routing for plugin-registered endpoints.
 */

package com.vaultstadio.api.routes.plugin

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.PluginConfigResponse
import com.vaultstadio.api.dto.PluginInfoResponse
import com.vaultstadio.api.plugins.PluginManager
import com.vaultstadio.core.domain.model.UserRole
import com.vaultstadio.plugins.context.EndpointRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.get as koinGet

fun Route.pluginRoutes() {
    route("/plugins") {
        // List installed plugins
        get {
            val pluginManager: PluginManager = call.application.koinGet()
            val plugins = pluginManager.listPlugins()
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = plugins.map { plugin ->
                        PluginInfoResponse(
                            id = plugin.metadata.id,
                            name = plugin.metadata.name,
                            version = plugin.metadata.version,
                            description = plugin.metadata.description,
                            author = plugin.metadata.author,
                            isEnabled = pluginManager.isPluginEnabled(plugin.metadata.id),
                            state = pluginManager.getPluginState(plugin.metadata.id).name,
                        )
                    },
                ),
            )
        }

        // Get plugin details
        get("/{pluginId}") {
            val pluginManager: PluginManager = call.application.koinGet()
            val pluginId = call.parameters["pluginId"]!!

            val plugin = pluginManager.getPlugin(pluginId)
            if (plugin == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("PLUGIN_NOT_FOUND", "Plugin not found: $pluginId"),
                    ),
                )
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = PluginInfoResponse(
                        id = plugin.metadata.id,
                        name = plugin.metadata.name,
                        version = plugin.metadata.version,
                        description = plugin.metadata.description,
                        author = plugin.metadata.author,
                        isEnabled = pluginManager.isPluginEnabled(plugin.metadata.id),
                        state = pluginManager.getPluginState(plugin.metadata.id).name,
                    ),
                ),
            )
        }

        // Get plugin configuration
        get("/{pluginId}/config") {
            val pluginManager: PluginManager = call.application.koinGet()
            val pluginId = call.parameters["pluginId"]!!

            val plugin = pluginManager.getPlugin(pluginId)
            if (plugin == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("PLUGIN_NOT_FOUND", "Plugin not found: $pluginId"),
                    ),
                )
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = PluginConfigResponse(
                        pluginId = pluginId,
                        config = plugin.getConfiguration(),
                    ),
                ),
            )
        }

        // Update plugin configuration (admin only)
        put("/{pluginId}/config") {
            val pluginManager: PluginManager = call.application.koinGet()
            val user = call.user!!
            if (user.role != UserRole.ADMIN) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("ACCESS_DENIED", "Admin access required"),
                    ),
                )
                return@put
            }

            val pluginId = call.parameters["pluginId"]!!
            val config = call.receive<Map<String, Any?>>()

            val plugin = pluginManager.getPlugin(pluginId)
            if (plugin == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("PLUGIN_NOT_FOUND", "Plugin not found: $pluginId"),
                    ),
                )
                return@put
            }

            val success = plugin.updateConfiguration(config)
            if (success) {
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse<Unit>(success = true),
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("CONFIG_UPDATE_FAILED", "Failed to update plugin configuration"),
                    ),
                )
            }
        }

        // Enable plugin (admin only)
        post("/{pluginId}/enable") {
            val pluginManager: PluginManager = call.application.koinGet()
            val user = call.user!!
            if (user.role != UserRole.ADMIN) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("ACCESS_DENIED", "Admin access required"),
                    ),
                )
                return@post
            }

            val pluginId = call.parameters["pluginId"]!!

            pluginManager.enablePlugin(pluginId).fold(
                { error -> throw error },
                {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<Unit>(success = true),
                    )
                },
            )
        }

        // Disable plugin (admin only)
        post("/{pluginId}/disable") {
            val pluginManager: PluginManager = call.application.koinGet()
            val user = call.user!!
            if (user.role != UserRole.ADMIN) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("ACCESS_DENIED", "Admin access required"),
                    ),
                )
                return@post
            }

            val pluginId = call.parameters["pluginId"]!!

            pluginManager.disablePlugin(pluginId).fold(
                { error -> throw error },
                {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<Unit>(success = true),
                    )
                },
            )
        }

        // List registered endpoints for a plugin
        get("/{pluginId}/endpoints") {
            val pluginManager: PluginManager = call.application.koinGet()
            val pluginId = call.parameters["pluginId"]!!

            val endpoints = pluginManager.getPluginEndpoints(pluginId)
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = endpoints.map { endpoint ->
                        val parts = endpoint.split(":", limit = 2)
                        mapOf("method" to parts[0], "path" to parts.getOrElse(1) { "" })
                    },
                ),
            )
        }

        // Dynamic routing for plugin-registered endpoints
        // Handles: GET, POST, PUT, PATCH, DELETE to /plugins/{pluginId}/api/{path...}
        route("/{pluginId}/api") {
            get("/{path...}") {
                handlePluginEndpoint(call, "GET")
            }
            post("/{path...}") {
                handlePluginEndpoint(call, "POST")
            }
            put("/{path...}") {
                handlePluginEndpoint(call, "PUT")
            }
            patch("/{path...}") {
                handlePluginEndpoint(call, "PATCH")
            }
            delete("/{path...}") {
                handlePluginEndpoint(call, "DELETE")
            }
        }
    }
}

/**
 * Handles requests to plugin-registered endpoints.
 */
private suspend fun handlePluginEndpoint(
    call: ApplicationCall,
    method: String,
) {
    val pluginManager: PluginManager = call.application.koinGet()
    val pluginId = call.parameters["pluginId"]
    val pathParts = call.parameters.getAll("path") ?: emptyList()
    val path = "/" + pathParts.joinToString("/")

    if (pluginId == null) {
        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(
                success = false,
                error = ApiError("INVALID_PLUGIN_ID", "Plugin ID is required"),
            ),
        )
        return
    }

    val plugin = pluginManager.getPlugin(pluginId)
    if (plugin == null) {
        call.respond(
            HttpStatusCode.NotFound,
            ApiResponse<Unit>(
                success = false,
                error = ApiError("PLUGIN_NOT_FOUND", "Plugin not found: $pluginId"),
            ),
        )
        return
    }

    if (!pluginManager.isPluginEnabled(pluginId)) {
        call.respond(
            HttpStatusCode.ServiceUnavailable,
            ApiResponse<Unit>(
                success = false,
                error = ApiError("PLUGIN_DISABLED", "Plugin is not enabled: $pluginId"),
            ),
        )
        return
    }

    // Build endpoint request
    val user = call.user
    val body = try {
        if (method in listOf("POST", "PUT", "PATCH")) {
            call.receiveText()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    val request = EndpointRequest(
        method = method,
        path = path,
        headers = call.request.headers.entries().associate { it.key to it.value },
        queryParams = call.request.queryParameters.entries().associate { it.key to it.value },
        body = body,
        userId = user?.id,
    )

    // Delegate to plugin
    val response = pluginManager.handlePluginEndpoint(pluginId, method, path, request)

    if (response == null) {
        call.respond(
            HttpStatusCode.NotFound,
            ApiResponse<Unit>(
                success = false,
                error = ApiError("ENDPOINT_NOT_FOUND", "Endpoint not found: $method $path"),
            ),
        )
        return
    }

    // Apply response headers
    response.headers.forEach { (key, value) ->
        call.response.headers.append(key, value)
    }

    // Send response
    val contentType = ContentType.parse(response.contentType)
    val statusCode = HttpStatusCode.fromValue(response.statusCode)

    val responseBody = response.body
    if (responseBody != null) {
        call.respondText(responseBody, contentType, statusCode)
    } else {
        call.respond(statusCode)
    }
}
