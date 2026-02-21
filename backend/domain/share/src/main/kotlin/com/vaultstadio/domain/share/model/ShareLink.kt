package com.vaultstadio.domain.share.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ShareLink(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val token: String,
    val createdBy: String,
    val expiresAt: Instant? = null,
    val password: String? = null,
    val maxDownloads: Int? = null,
    val downloadCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val sharedWithUsers: List<String> = emptyList(),
) {
    fun isExpired(now: Instant): Boolean = expiresAt?.let { it < now } ?: false
    fun isDownloadLimitReached(): Boolean = maxDownloads?.let { downloadCount >= it } ?: false
}
