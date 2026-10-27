package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.api.config.user
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.core.domain.service.JoinSessionInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.collaborationSessionRoutes(service: CollaborationService) {
    route("/sessions") {
        joinRoute(service)
        leaveRoute(service)
        sessionReadRoutes(service)
        cursorRoute(service)
        selectionRoute(service)
    }
}

private fun Route.joinRoute(service: CollaborationService) {
    post("/join") {
        val actor = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val item = call.receive<JoinSessionRequest>().itemId.trim()
        if (item.isEmpty()) return@post call.respond(HttpStatusCode.BadRequest, "Missing item ID")
        service.getDocumentState(item, actor.id).fold(
            { call.respondCollaborationError(it) },
            { document ->
                service.joinSession(JoinSessionInput(item), actor.id, actor.username).fold(
                    { call.respondCollaborationError(it) },
                    { (session, participant) ->
                        call.respond(HttpStatusCode.Created, session.response(participant.id, document?.version ?: 0))
                    },
                )
            },
        )
    }
}

private fun Route.leaveRoute(service: CollaborationService) {
    post("/{sessionId}/leave") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val session = call.parameters["sessionId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val participant =
            call.request.headers["X-Participant-ID"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        service.leaveSession(session, participant, actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(mapOf("message" to "Left session")) },
        )
    }
}

private fun Route.sessionReadRoutes(service: CollaborationService) {
    get("/{sessionId}") {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val session = call.parameters["sessionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        service.getSession(session, actor).fold(
            { call.respondCollaborationError(it) },
            { bound ->
                service.getDocumentState(bound.itemId, actor).fold(
                    { call.respondCollaborationError(it) },
                    { call.respond(bound.response("", it?.version ?: 0)) },
                )
            },
        )
    }
    get("/{sessionId}/participants") {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val session = call.parameters["sessionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        service.getParticipants(session, actor).fold(
            {
                call.respondCollaborationError(it)
            },
            { call.respond(it.map { participant -> participant.toResponse() }) },
        )
    }
}

private fun Route.cursorRoute(service: CollaborationService) {
    post("/{sessionId}/cursor") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val session = call.parameters["sessionId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val participant =
            call.request.headers["X-Participant-ID"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<UpdateCursorRequest>()
        service.updateCursor(
            session,
            participant,
            CursorPosition(request.line, request.column, request.offset),
            actor,
        ).fold(
            { call.respondCollaborationError(it) },
            { call.respond(HttpStatusCode.OK) },
        )
    }
}

private fun Route.selectionRoute(service: CollaborationService) {
    post("/{sessionId}/selection") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val session = call.parameters["sessionId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val participant =
            call.request.headers["X-Participant-ID"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<UpdateSelectionRequest>()
        val selection = TextSelection(
            CursorPosition(request.start.line, request.start.column, request.start.offset),
            CursorPosition(request.end.line, request.end.column, request.end.offset),
        )
        service.updateSelection(session, participant, selection, actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(HttpStatusCode.OK) },
        )
    }
}

private fun CollaborationSession.response(participant: String, version: Long) = CollaborationSessionResponse(
    id,
    itemId,
    participant,
    participants.map { it.toResponse() },
    version,
    createdAt.toString(),
    expiresAt.toString(),
)
