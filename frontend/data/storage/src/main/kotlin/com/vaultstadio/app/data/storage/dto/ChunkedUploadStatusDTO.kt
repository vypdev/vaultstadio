/**
 * Chunked upload status DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChunkedUploadStatusDTO(
    val uploadId: String,
    val fileName: String,
    val totalSize: Long,
    val uploadedBytes: Long,
    val progress: Float,
    val receivedChunks: List<Int>,
    val missingChunks: List<Int>,
    val isComplete: Boolean,
)
