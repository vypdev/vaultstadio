package com.vaultstadio.core.share.access

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.vaultstadio.domain.share.access.AccessShareInput
import com.vaultstadio.domain.share.access.PublicShareAccessRepository
import com.vaultstadio.domain.share.access.PublicShareClock
import com.vaultstadio.domain.share.access.PublicShareDownload
import com.vaultstadio.domain.share.access.PublicShareFailure
import com.vaultstadio.domain.share.access.PublicShareInfo
import com.vaultstadio.domain.share.access.PublicSharePolicy
import com.vaultstadio.domain.share.access.PublicShareSnapshot
import com.vaultstadio.domain.share.access.ShareContentReader
import com.vaultstadio.domain.share.access.SharePasswordVerifier
import com.vaultstadio.domain.share.model.ShareLink
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class PublicShareService(
    private val repository: PublicShareAccessRepository,
    private val passwords: SharePasswordVerifier,
    private val content: ShareContentReader,
    private val clock: PublicShareClock,
) {
    suspend fun information(input: AccessShareInput): Either<PublicShareFailure, PublicShareInfo> =
        snapshot(input).map { it.information() }

    suspend fun download(input: AccessShareInput): Either<PublicShareFailure, PublicShareDownload> {
        val snapshot = when (val result = snapshot(input)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        val prepared = when (val result = content.prepare(snapshot.item)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        var transferred = false
        try {
            currentCoroutineContext().ensureActive()
            return repository.admit(snapshot).map {
                transferred = true
                PublicShareDownload(snapshot.information(), prepared)
            }
        } finally {
            if (!transferred) prepared.close()
        }
    }

    private suspend fun snapshot(input: AccessShareInput): Either<PublicShareFailure, PublicShareSnapshot> = either {
        ensure(input.token.matches(TOKEN_PATTERN)) { PublicShareFailure.NOT_FOUND }
        val share = repository.find(input.token).bind() ?: raise(PublicShareFailure.NOT_FOUND)
        PublicSharePolicy.linkFailure(share, clock.now())?.let { raise(it) }
        passwordFailure(share, input.password)?.let { raise(it) }
        repository.inspect(share).bind()
    }

    private fun passwordFailure(share: ShareLink, password: String?): PublicShareFailure? {
        val hash = share.password
        return when {
            hash == null -> null
            password == null -> PublicShareFailure.PASSWORD_REQUIRED
            !PublicSharePolicy.validPasswordInput(password) -> PublicShareFailure.PASSWORD_INVALID
            !passwords.matches(password, hash) -> PublicShareFailure.PASSWORD_INVALID
            else -> null
        }
    }

    companion object {
        private val TOKEN_PATTERN = Regex("[A-Za-z0-9_-]{1,64}")
    }
}
