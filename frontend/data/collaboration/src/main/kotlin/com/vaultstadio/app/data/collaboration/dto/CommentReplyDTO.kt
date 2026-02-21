/**
 * Comment Reply DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CommentReplyDTO(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val content: String,
    @kotlinx.serialization.Contextual val createdAt: Instant,
)
