package com.vaultstadio.domain.share.access

/** Safe semantic outcomes: never carry token, password, SQL or provider diagnostics. */
enum class PublicShareFailure(val code: String) {
    NOT_FOUND("SHARE_NOT_FOUND"),
    REVOKED("SHARE_REVOKED"),
    EXPIRED("SHARE_EXPIRED"),
    LIMIT_REACHED("SHARE_LIMIT_REACHED"),
    PASSWORD_REQUIRED("SHARE_PASSWORD_REQUIRED"),
    PASSWORD_INVALID("SHARE_PASSWORD_INVALID"),
    CHANGED("SHARE_CHANGED"),
    INVALID_REQUEST("INVALID_SHARE_REQUEST"),
    UNAVAILABLE("SHARE_UNAVAILABLE"),
}
