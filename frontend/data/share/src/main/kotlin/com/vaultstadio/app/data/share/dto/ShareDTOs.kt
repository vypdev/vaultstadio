/**
 * Share Data Transfer Objects
 */

package com.vaultstadio.app.data.share.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ShareLinkDTO(
    val id: String,
    val itemId: String,
    val token: String,
    val url: String,
    @Contextual val expiresAt: Instant?,
    val hasPassword: Boolean,
    val maxDownloads: Int?,
    val downloadCount: Int,
    val isActive: Boolean,
    @Contextual val createdAt: Instant,
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
