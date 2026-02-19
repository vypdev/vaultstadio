/**
 * Share link domain model.
 */

package com.vaultstadio.app.domain.share.model

import kotlinx.datetime.Instant

data class ShareLink(
    val id: String,
    val itemId: String,
    val token: String,
    val url: String,
    val expiresAt: Instant?,
    val hasPassword: Boolean,
    val maxDownloads: Int?,
    val downloadCount: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val createdBy: String = "",
    val sharedWithUsers: List<String> = emptyList(),
)
