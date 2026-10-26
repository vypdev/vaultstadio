package com.vaultstadio.api.routes.version

import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.domain.common.exception.StorageException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/** Preserve semantic/provider status without exposing provider diagnostics or storage keys. */
internal suspend fun ApplicationCall.respondVersionError(error: StorageException) {
    val message = if (error.httpStatus >= 500) "Version operation failed" else error.message
    respond(
        HttpStatusCode.fromValue(error.httpStatus),
        ApiResponse<Unit>(success = false, error = ApiError(error.errorCode, message)),
    )
}
