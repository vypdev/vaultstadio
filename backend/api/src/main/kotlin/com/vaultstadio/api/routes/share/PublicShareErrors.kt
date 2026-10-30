package com.vaultstadio.api.routes.share

import com.vaultstadio.domain.share.access.PublicShareFailure
import io.ktor.http.HttpStatusCode

/** HTTP status and public wording belong to this presentation adapter, not the domain model. */
internal val PublicShareFailure.httpStatus: HttpStatusCode
    get() = when (this) {
        PublicShareFailure.NOT_FOUND -> HttpStatusCode.NotFound
        PublicShareFailure.REVOKED, PublicShareFailure.EXPIRED, PublicShareFailure.LIMIT_REACHED -> HttpStatusCode.Gone
        PublicShareFailure.PASSWORD_REQUIRED, PublicShareFailure.PASSWORD_INVALID -> HttpStatusCode.Forbidden
        PublicShareFailure.CHANGED -> HttpStatusCode.Conflict
        PublicShareFailure.INVALID_REQUEST -> HttpStatusCode.BadRequest
        PublicShareFailure.UNAVAILABLE -> HttpStatusCode.InternalServerError
    }

internal val PublicShareFailure.description: String
    get() = when (this) {
        PublicShareFailure.NOT_FOUND -> "Shared file is unavailable"
        PublicShareFailure.REVOKED -> "This link has been revoked"
        PublicShareFailure.EXPIRED -> "This link has expired"
        PublicShareFailure.LIMIT_REACHED -> "This link has no downloads remaining"
        PublicShareFailure.PASSWORD_REQUIRED -> "Enter the link password"
        PublicShareFailure.PASSWORD_INVALID -> "The link password is incorrect"
        PublicShareFailure.CHANGED -> "The shared file changed; load the link again"
        PublicShareFailure.INVALID_REQUEST -> "Invalid share request"
        PublicShareFailure.UNAVAILABLE -> "The shared file could not be read; try again later"
    }
