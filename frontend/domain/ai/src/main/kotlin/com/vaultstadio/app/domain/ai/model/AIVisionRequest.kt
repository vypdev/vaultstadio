/**
 * Request for vision/describe image.
 */

package com.vaultstadio.app.domain.ai.model

data class AIVisionRequest(
    val prompt: String,
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
)
