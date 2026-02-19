/**
 * Request for AI describe (item).
 */

package com.vaultstadio.app.domain.ai.model

data class AIDescribeRequest(
    val itemId: String,
    val prompt: String? = null,
)
