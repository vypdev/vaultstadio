/**
 * Document Comment
 */

package com.vaultstadio.app.domain.collaboration.model

import kotlinx.datetime.Instant

data class DocumentComment(
    val id: String,
    val itemId: String,
    val userId: String,
    val userName: String? = null,
    val content: String,
    val anchor: CommentAnchor,
    val isResolved: Boolean,
    val replies: List<CommentReply>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
