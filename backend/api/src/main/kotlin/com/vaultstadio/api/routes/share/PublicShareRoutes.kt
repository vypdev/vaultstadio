package com.vaultstadio.api.routes.share

import arrow.core.getOrElse
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.application.usecase.share.AccessShareUseCase
import com.vaultstadio.application.usecase.share.DownloadPublicShareUseCase
import com.vaultstadio.domain.share.access.AccessShareInput
import com.vaultstadio.domain.share.access.PublicShareFailure
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.get as koinGet

fun Route.publicShareRoutes() {
    publicSharePageRoutes()
    route("/share/{token}") {
        get {
            call.publicShareHeaders()
            if (call.rejectPasswordQuery()) return@get
            val input = call.headerShareInput().getOrElse {
                call.shareFailure(it)
                return@get
            }
            val access: AccessShareUseCase = call.application.koinGet()
            access(input).fold(
                { call.shareFailure(it) },
                { call.respond(ApiResponse(success = true, data = it.toPublicResponse())) },
            )
        }
        get("/download") {
            call.publicShareHeaders()
            if (call.rejectPasswordQuery()) return@get
            val input = call.headerShareInput().getOrElse {
                call.shareFailure(it)
                return@get
            }
            val download: DownloadPublicShareUseCase = call.application.koinGet()
            download(input).fold(
                { call.shareFailure(it) },
                { call.streamShare(it) },
            )
        }
    }
}

internal fun ApplicationCall.shareInput(password: String?) =
    AccessShareInput(parameters["token"].orEmpty(), password)

internal fun ApplicationCall.headerShareInput() =
    decodeSharePassword(request.headers["X-Share-Password"]).map { shareInput(it) }

internal suspend fun ApplicationCall.rejectPasswordQuery(): Boolean {
    val headerCount = request.headers.getAll("X-Share-Password").orEmpty().size
    val invalid = request.queryParameters.contains("password") || headerCount > 1
    if (invalid) shareFailure(PublicShareFailure.INVALID_REQUEST)
    return invalid
}

internal suspend fun ApplicationCall.shareFailure(failure: PublicShareFailure) {
    respond(
        failure.httpStatus,
        ApiResponse<Unit>(success = false, error = ApiError(failure.code, failure.description)),
    )
}
