package com.vaultstadio.core.domain.service

/**
 * Input for joining a collaboration session.
 *
 * @property itemId Storage item ID to collaborate on
 */
data class JoinSessionInput(
    val itemId: String,
)

/**
 * Input for creating a comment.
 *
 * @property itemId Storage item ID
 * @property content Comment text
 * @property startLine Start line of anchor
 * @property startColumn Start column of anchor
 * @property endLine End line of anchor
 * @property endColumn End column of anchor
 * @property quotedText Text being commented on
 */
data class CreateCommentInput(
    val itemId: String,
    val content: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)
