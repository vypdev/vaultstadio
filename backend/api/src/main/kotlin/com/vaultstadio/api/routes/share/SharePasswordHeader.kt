package com.vaultstadio.api.routes.share

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.vaultstadio.domain.share.access.PublicShareFailure
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.util.Base64

/** ASCII wire encoding for strict UTF-8 passwords, including characters forbidden in Fetch headers. */
internal fun decodeSharePassword(value: String?): Either<PublicShareFailure, String?> {
    if (value == null) return null.right()
    return either {
        ensure(value.length in 1..96) { PublicShareFailure.INVALID_REQUEST }
        val bytes = try {
            Base64.getUrlDecoder().decode(value)
        } catch (_: IllegalArgumentException) {
            raise(PublicShareFailure.INVALID_REQUEST)
        }
        ensure(bytes.size in 1..72) { PublicShareFailure.INVALID_REQUEST }
        ensure(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) == value) {
            PublicShareFailure.INVALID_REQUEST
        }
        try {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            raise(PublicShareFailure.INVALID_REQUEST)
        }
    }
}
