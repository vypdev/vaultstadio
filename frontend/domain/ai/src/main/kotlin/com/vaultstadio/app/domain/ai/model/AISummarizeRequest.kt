/**
 * Request for summarization.
 */

package com.vaultstadio.app.domain.ai.model

data class AISummarizeRequest(
    val itemId: String,
    val maxLength: Int? = null,
)
