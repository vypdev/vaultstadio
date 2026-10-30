package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.domain.common.exception.StorageException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

internal suspend fun ApplicationCall.respondCollaborationError(error: StorageException) {
    val message = if (error.httpStatus >= 500) "Collaboration operation failed" else error.message
    respond(
        HttpStatusCode.fromValue(error.httpStatus),
        ApiResponse<Unit>(success = false, error = ApiError(error.errorCode, message)),
    )
}
