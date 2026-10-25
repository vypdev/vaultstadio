/**
 * Document Comment DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DocumentCommentDTO(
    val id: String,
    val itemId: String,
    val userId: String,
    val userName: String? = null,
    val content: String,
    val anchor: CommentAnchorDTO,
    val isResolved: Boolean,
    val replies: List<CommentReplyDTO>,
    @kotlinx.serialization.Contextual val createdAt: Instant,
    @kotlinx.serialization.Contextual val updatedAt: Instant,
)
