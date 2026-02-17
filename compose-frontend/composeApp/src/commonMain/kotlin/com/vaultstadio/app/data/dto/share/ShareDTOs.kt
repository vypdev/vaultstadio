/**
 * Share Data Transfer Objects
 */

package com.vaultstadio.app.data.dto.share

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ShareLinkDTO(
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

@Serializable
data class CreateShareRequestDTO(
    val itemId: String,
    val expiresInDays: Int? = null,
    val password: String? = null,
    val maxDownloads: Int? = null,
)
