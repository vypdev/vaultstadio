package com.vaultstadio.api.routes.version

import com.vaultstadio.api.config.user
import com.vaultstadio.application.usecase.version.GetVersionUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get as koinGet

internal fun Route.versionDetailRoutes() {
    get("/item/{itemId}/version/{versionNumber}") {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val number = call.parameters["versionNumber"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid version number")
        val operation: GetVersionUseCase = call.application.koinGet()
        operation(item, number, actor).fold(
            { call.respondVersionError(it) },
            { version -> call.respond(version.toResponse()) },
        )
    }

    get("/item/{itemId}/version/{versionNumber}/download") {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val number = call.parameters["versionNumber"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid version number")
        val operation: GetVersionUseCase = call.application.koinGet()
        operation(item, number, actor).fold(
            { call.respondVersionError(it) },
            { version ->
                call.respond(
                    VersionDownloadResponse(version.id, version.storageKey, version.size, version.checksum),
                )
            },
        )
    }
}
