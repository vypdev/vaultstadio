/**
 * VaultStadio Route Extensions
 *
 * Provides standardized response handling for routes using Arrow Either.
 */

package com.vaultstadio.api.routes

import arrow.core.Either
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.domain.common.exception.StorageException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * Responds with the result of an Either, throwing the left value if present.
 *
 * This is the standard pattern for handling service results in routes.
 * Exceptions are caught by the error handling middleware.
 *
 * @param either The Either result from a service operation
 * @param status The HTTP status code for successful responses (default: OK)
 */
suspend inline fun <reified T : Any> ApplicationCall.respondEither(
    either: Either<StorageException, T>,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    either.fold(
        ifLeft = { throw it },
        ifRight = { respond(status, it) },
    )
}

/**
 * Responds with an ApiResponse wrapper for the Either result.
 *
 * @param either The Either result from a service operation
 * @param status The HTTP status code for successful responses (default: OK)
 */
suspend inline fun <reified T : Any> ApplicationCall.respondApiEither(
    either: Either<StorageException, T>,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    either.fold(
        ifLeft = { throw it },
        ifRight = {
            respond(
                status,
                ApiResponse(
                    success = true,
                    data = it,
                ),
            )
        },
    )
}

/**
 * Responds with Unit (no content) for Either<StorageException, Unit> results.
 *
 * @param either The Either result from a service operation
 * @param status The HTTP status code for successful responses (default: NoContent)
 */
suspend fun ApplicationCall.respondEitherUnit(
    either: Either<StorageException, Unit>,
    status: HttpStatusCode = HttpStatusCode.NoContent,
) {
    either.fold(
        ifLeft = { throw it },
        ifRight = { respond(status) },
    )
}
