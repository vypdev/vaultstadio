package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.api.config.user
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.core.domain.service.CreateCommentInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.collaborationCommentRoutes(service: CollaborationService) {
    route("/documents/{itemId}/comments") {
        commentReadCreateRoutes(service)
        resolveRoute(service)
        replyRoute(service)
        deleteCommentRoute(service)
    }
}

private fun Route.commentReadCreateRoutes(service: CollaborationService) {
    get {
        val actor = call.user?.id ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val resolved = call.request.queryParameters["includeResolved"]?.toBoolean() ?: false
        service.getComments(item, actor, resolved).fold(
            { call.respondCollaborationError(it) },
            { call.respond(it.map { comment -> comment.toResponse() }) },
        )
    }
    post {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<CreateCommentRequest>()
        val input = CreateCommentInput(
            item,
            request.content,
            request.startLine,
            request.startColumn,
            request.endLine,
            request.endColumn,
            request.quotedText,
        )
        service.createComment(input, actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(HttpStatusCode.Created, mapOf("id" to it.id)) },
        )
    }
}

private fun Route.resolveRoute(service: CollaborationService) {
    post("/{commentId}/resolve") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val comment = call.parameters["commentId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        service.resolveComment(item, comment, actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(mapOf("message" to "Comment resolved")) },
        )
    }
}

private fun Route.replyRoute(service: CollaborationService) {
    post("/{commentId}/replies") {
        val actor = call.user?.id ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val comment = call.parameters["commentId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val request = call.receive<AddReplyRequest>()
        service.addReply(itemId = item, commentId = comment, content = request.content, userId = actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(HttpStatusCode.Created, mapOf("id" to checkNotNull(it.replies.lastOrNull()).id)) },
        )
    }
}

private fun Route.deleteCommentRoute(service: CollaborationService) {
    delete("/{commentId}") {
        val actor = call.user?.id ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        val item = call.parameters["itemId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        val comment = call.parameters["commentId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        service.deleteComment(item, comment, actor).fold(
            { call.respondCollaborationError(it) },
            { call.respond(HttpStatusCode.NoContent) },
        )
    }
}
