package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.core.domain.service.CollaborationService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.collaborationRoutes(service: CollaborationService) {
    authenticate("auth-bearer") {
        route("/api/v1/collaboration") {
            collaborationSessionRoutes(service)
            collaborationDocumentRoutes(service)
            collaborationCommentRoutes(service)
            collaborationPresenceRoutes(service)
        }
    }
    collaborationWebSocket(service)
}
