/**
 * Comment Reply
 */

package com.vaultstadio.app.domain.collaboration.model

import kotlinx.datetime.Instant

data class CommentReply(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val content: String,
    val createdAt: Instant,
)
