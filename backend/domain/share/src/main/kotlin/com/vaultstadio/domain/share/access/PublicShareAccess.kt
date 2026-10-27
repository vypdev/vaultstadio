package com.vaultstadio.domain.share.access

import arrow.core.Either
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.datetime.Instant
import java.io.InputStream

// Request metadata such as IP/user agent is intentionally not an authority input.
data class AccessShareInput(val token: String, val password: String? = null)

data class PublicShareInfo(
    val name: String,
    val size: Long,
    val mimeType: String?,
    val hasPassword: Boolean,
    val expiresAt: Instant?,
)

data class PublicShareSnapshot(val share: ShareLink, val item: StorageItem) {
    fun information() = PublicShareInfo(item.name, item.size, item.mimeType, share.password != null, share.expiresAt)
}

fun interface PublicShareClock {
    fun now(): Instant
}

fun interface SharePasswordVerifier {
    fun matches(password: String, hash: String): Boolean
}

/** Implementations serialize item then share and read the clock after acquiring both locks. */
interface PublicShareAccessRepository {
    suspend fun find(token: String): Either<PublicShareFailure, ShareLink?>
    suspend fun inspect(expected: ShareLink): Either<PublicShareFailure, PublicShareSnapshot>
    suspend fun admit(expected: PublicShareSnapshot): Either<PublicShareFailure, Unit>
}

interface PreparedShareContent : AutoCloseable {
    val stream: InputStream
}

fun interface ShareContentReader {
    suspend fun prepare(item: StorageItem): Either<PublicShareFailure, PreparedShareContent>
}

data class PublicShareDownload(val information: PublicShareInfo, val content: PreparedShareContent)
