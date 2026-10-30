package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Comment on a document section.
 *
 * @property id Unique identifier
 * @property itemId Storage item ID
 * @property userId User who created the comment
 * @property content Comment text
 * @property anchor Position anchor in the document
 * @property resolvedAt When the comment was resolved
 * @property replies Threaded replies
 * @property createdAt When the comment was created
 * @property updatedAt When the comment was last updated
 */
@Serializable
data class DocumentComment(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val userId: String,
    val content: String,
    val anchor: CommentAnchor,
    val resolvedAt: Instant? = null,
    val replies: List<CommentReply> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isResolved: Boolean get() = resolvedAt != null
}

/**
 * Anchor position for a comment.
 *
 * @property startLine Starting line
 * @property startColumn Starting column
 * @property endLine Ending line
 * @property endColumn Ending column
 * @property quotedText The text being commented on
 */
@Serializable
data class CommentAnchor(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)

/**
 * Reply to a comment.
 *
 * @property id Unique identifier
 * @property userId User who created the reply
 * @property content Reply text
 * @property createdAt When the reply was created
 */
@Serializable
data class CommentReply(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val content: String,
    val createdAt: Instant,
)
