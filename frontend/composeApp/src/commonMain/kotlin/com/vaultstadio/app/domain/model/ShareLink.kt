/**
 * Share Domain Models
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

/**
 * Share link domain model.
 */
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
