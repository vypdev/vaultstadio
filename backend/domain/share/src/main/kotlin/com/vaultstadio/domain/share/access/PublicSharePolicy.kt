package com.vaultstadio.domain.share.access

import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.datetime.Instant

/** The same decisions apply to the read snapshot and the atomic download admission. */
object PublicSharePolicy {
    fun linkFailure(share: ShareLink, now: Instant): PublicShareFailure? = when {
        !share.isActive -> PublicShareFailure.REVOKED
        share.isExpired(now) -> PublicShareFailure.EXPIRED
        share.downloadCount < 0 -> PublicShareFailure.NOT_FOUND
        share.maxDownloads?.let { it <= 0 } == true -> PublicShareFailure.NOT_FOUND
        share.downloadCount == Int.MAX_VALUE -> PublicShareFailure.LIMIT_REACHED
        share.isDownloadLimitReached() -> PublicShareFailure.LIMIT_REACHED
        else -> null
    }

    fun itemFailure(share: ShareLink, item: StorageItem?): PublicShareFailure? = when {
        item == null -> PublicShareFailure.NOT_FOUND
        item.id != share.itemId -> PublicShareFailure.NOT_FOUND
        item.ownerId != share.createdBy -> PublicShareFailure.NOT_FOUND
        item.type != ItemType.FILE -> PublicShareFailure.NOT_FOUND
        item.isTrashed -> PublicShareFailure.NOT_FOUND
        item.storageKey.isNullOrBlank() -> PublicShareFailure.NOT_FOUND
        item.size < 0 -> PublicShareFailure.NOT_FOUND
        else -> null
    }

    fun sameCredential(expected: ShareLink, current: ShareLink): Boolean =
        expected.id == current.id &&
            expected.token == current.token &&
            expected.itemId == current.itemId &&
            expected.createdBy == current.createdBy &&
            expected.password == current.password

    fun validPasswordInput(password: String): Boolean =
        password.toByteArray(Charsets.UTF_8).size in 1..72 && Charsets.UTF_8.newEncoder().canEncode(password)

    fun validCreation(expirationDays: Int?, maxDownloads: Int?, password: String?): Boolean =
        (expirationDays == null || expirationDays in 1..365) &&
            (maxDownloads == null || maxDownloads > 0) &&
            (password == null || validPasswordInput(password))
}
