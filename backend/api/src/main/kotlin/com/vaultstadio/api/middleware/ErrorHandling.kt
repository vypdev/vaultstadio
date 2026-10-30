/**
 * VaultStadio Error Handling Middleware
 */

package com.vaultstadio.api.middleware

import com.vaultstadio.api.config.AuthenticationException
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.domain.common.exception.StorageException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException

private val logger = KotlinLogging.logger {}

fun Application.configureErrorHandling() {
    install(StatusPages) {
        storageErrors()
        requestErrors()
        unhandledErrors()
    }
}

private fun StatusPagesConfig.storageErrors() {
    // Handle storage exceptions
    exception<StorageException> { call, cause ->
        logger.warn { "Storage exception: ${cause.message}" }

        val response = ApiResponse<Unit>(
            success = false,
            error = ApiError(
                code = cause.errorCode,
                message = cause.message ?: "An error occurred",
            ),
        )

        call.respond(
            HttpStatusCode.fromValue(cause.httpStatus),
            response,
        )
    }
}

private fun StatusPagesConfig.requestErrors() {
    exception<BadRequestException> { call, _ ->
        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(
                success = false,
                error = ApiError(code = "VALIDATION_ERROR", message = "Invalid request"),
            ),
        )
    }

    // Handle authentication exceptions
    exception<AuthenticationException> { call, _ ->
        call.respond(
            HttpStatusCode.Unauthorized,
            ApiResponse<Unit>(
                success = false,
                error = ApiError(
                    code = "AUTHENTICATION_REQUIRED",
                    message = "Authentication required",
                ),
            ),
        )
    }

    // Handle validation exceptions
    exception<IllegalArgumentException> { call, cause ->
        logger.warn { "Validation error: ${cause.message}" }

        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(
                success = false,
                error = ApiError(
                    code = "VALIDATION_ERROR",
                    message = cause.message ?: "Invalid request",
                ),
            ),
        )
    }
}

private fun StatusPagesConfig.unhandledErrors() {
    // Transport/request cancellation must remain control flow, including a closing WebSocket.
    exception<CancellationException> { _, cause -> throw cause }

    // Handle generic exceptions
    exception<Exception> { call, cause ->
        logger.error(cause) { "Unhandled exception" }

        call.respond(
            HttpStatusCode.InternalServerError,
            ApiResponse<Unit>(
                success = false,
                error = ApiError(
                    code = "INTERNAL_ERROR",
                    message = "An internal error occurred",
                ),
            ),
        )
    }

    // Handle 404
    status(HttpStatusCode.NotFound) { call, _ ->
        call.respond(
            HttpStatusCode.NotFound,
            ApiResponse<Unit>(
                success = false,
                error = ApiError(
                    code = "NOT_FOUND",
                    message = "Resource not found",
                ),
            ),
        )
    }
}
