package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.api.config.user
import com.vaultstadio.core.domain.service.CollaborationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.collaborationDocumentRoutes(service: CollaborationService) {
    route("/documents") {
        documentReadRoute(service)
        operationRoutes(service)
        saveRoute(service)
    }
}

private fun Route.documentReadRoute(service: CollaborationService) {
    get("/{itemId}") {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        service.getDocumentState(item, actor).fold(
            { call.respondCollaborationError(it) },
            { state ->
                if (state == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(
                        DocumentStateResponse(
                            state.itemId,
                            state.version,
                            state.content,
                            state.lastModified.toString(),
                        ),
                    )
                }
            },
        )
    }
}

private fun Route.operationRoutes(service: CollaborationService) {
    post("/{itemId}/operations") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<OperationRequest>()
        val operation = OperationData(request.type, request.position, request.text, request.length, request.baseVersion)
        service.applyOperation(item, operation.toOperation(actor), actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(OperationAcknowledgement(true, it.version)) },
        )
    }
    get("/{itemId}/operations") {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0
        service.getOperationsSince(item, since, actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(it.map { operation -> operation.response() }) },
        )
    }
}

private fun Route.saveRoute(service: CollaborationService) {
    post("/{itemId}/save") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        service.saveDocument(item, actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(mapOf("message" to "Collaboration snapshot saved")) },
        )
    }
}
