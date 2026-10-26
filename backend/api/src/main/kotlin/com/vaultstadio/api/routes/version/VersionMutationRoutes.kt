package com.vaultstadio.api.routes.version

import com.vaultstadio.api.config.user
import com.vaultstadio.application.usecase.version.ApplyRetentionPolicyUseCase
import com.vaultstadio.application.usecase.version.DeleteVersionUseCase
import com.vaultstadio.application.usecase.version.RestoreVersionUseCase
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.service.RestoreVersionInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import org.koin.ktor.ext.get as koinGet

internal fun Route.versionMutationRoutes() {
    post("/item/{itemId}/restore") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<RestoreVersionRequest>()
        val operation: RestoreVersionUseCase = call.application.koinGet()
        operation(RestoreVersionInput(item, request.versionNumber, request.comment), actor).fold(
            { call.respondVersionError(it) },
            { call.respond(VersionRestoreResponse("Version restored", it.toResponse())) },
        )
    }

    delete("/{versionId}") {
        val actor = call.user?.id ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        val id = call.parameters["versionId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        val operation: DeleteVersionUseCase = call.application.koinGet()
        operation(id, actor).fold({ call.respondVersionError(it) }, { call.respond(HttpStatusCode.NoContent) })
    }

    post("/item/{itemId}/cleanup") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<CleanupRequest>()
        val operation: ApplyRetentionPolicyUseCase = call.application.koinGet()
        val policy = VersionRetentionPolicy(request.maxVersions, request.maxAgeDays, request.minVersionsToKeep)
        operation(item, policy, actor).fold(
            { call.respondVersionError(it) },
            { call.respond(VersionCleanupResponse("Retention policy applied", it)) },
        )
    }
}
