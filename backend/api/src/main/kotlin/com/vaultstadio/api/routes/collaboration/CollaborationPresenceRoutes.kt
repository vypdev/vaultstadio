package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.api.config.user
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.service.CollaborationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.collaborationPresenceRoutes(service: CollaborationService) {
    route("/presence") {
        post {
            val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<UpdatePresenceRequest>()
            val status = PresenceStatus.entries.find { it.name == request.status.uppercase() }
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid presence status")
            service.updatePresence(actor, status, activeDocument = request.activeDocument).fold(
                { call.respondCollaborationError(it) },
                { call.respond(HttpStatusCode.OK) },
            )
        }
        get {
            val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val users = call.request.queryParameters["userIds"]?.split(",") ?: emptyList()
            service.getPresence(users, actor).fold(
                { call.respondCollaborationError(it) },
                { rows ->
                    call.respond(
                        rows.values.map {
                            UserPresenceResponse(
                                it.userId,
                                null,
                                it.status.name,
                                it.lastSeen.toString(),
                                it.activeDocument,
                            )
                        },
                    )
                },
            )
        }
        post("/offline") {
            val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
            service.setOffline(actor).fold(
                { call.respondCollaborationError(it) },
                { call.respond(HttpStatusCode.OK) },
            )
        }
    }
}
