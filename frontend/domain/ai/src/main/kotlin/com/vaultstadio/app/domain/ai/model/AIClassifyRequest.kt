/**
 * Request for classification.
 */

package com.vaultstadio.app.domain.ai.model

data class AIClassifyRequest(
    val itemId: String,
    val categories: List<String>? = null,
)
