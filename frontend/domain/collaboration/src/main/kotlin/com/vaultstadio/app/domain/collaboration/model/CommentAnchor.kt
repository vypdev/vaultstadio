/**
 * Comment Anchor
 */

package com.vaultstadio.app.domain.collaboration.model

data class CommentAnchor(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)
