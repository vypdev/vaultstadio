/**
 * Request for tagging.
 */

package com.vaultstadio.app.domain.ai.model

data class AITagRequest(
    val itemIds: List<String>,
    val tags: List<String>,
)
