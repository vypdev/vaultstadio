package com.vaultstadio.api.routes.version

import com.vaultstadio.api.config.user
import com.vaultstadio.application.usecase.version.CompareVersionsUseCase
import com.vaultstadio.application.usecase.version.GetVersionHistoryUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get as koinGet

internal fun Route.versionHistoryRoutes() {
    get("/item/{itemId}") {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val operation: GetVersionHistoryUseCase = call.application.koinGet()
        operation(item, actor).fold(
            { call.respondVersionError(it) },
            { history ->
                call.respond(
                    VersionHistoryResponse(
                        itemId = history.item.id,
                        itemName = history.item.name,
                        versions = history.versions.map { it.toResponse() },
                        totalVersions = history.totalVersions,
                        totalSize = history.totalSize,
                    ),
                )
            },
        )
    }

    get("/item/{itemId}/diff") {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val from = call.request.queryParameters["from"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'from' version")
        val to = call.request.queryParameters["to"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'to' version")
        val operation: CompareVersionsUseCase = call.application.koinGet()
        operation(item, from, to, actor).fold(
            { call.respondVersionError(it) },
            { diff ->
                call.respond(
                    VersionDiffResponse(
                        fromVersion = diff.fromVersion,
                        toVersion = diff.toVersion,
                        sizeChange = diff.sizeChange,
                        additions = diff.additions,
                        deletions = diff.deletions,
                        isBinary = diff.isBinary,
                        patches = diff.patches.map { patch ->
                            DiffPatchResponse(
                                patch.operation.name,
                                patch.startLine,
                                patch.endLine,
                                patch.oldContent,
                                patch.newContent,
                            )
                        },
                    ),
                )
            },
        )
    }
}
