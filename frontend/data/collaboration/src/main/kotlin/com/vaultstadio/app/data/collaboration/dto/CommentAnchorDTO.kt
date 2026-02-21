/**
 * Comment Anchor DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentAnchorDTO(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)
