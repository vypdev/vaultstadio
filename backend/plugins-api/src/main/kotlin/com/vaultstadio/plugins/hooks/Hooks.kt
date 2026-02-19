/**
 * VaultStadio Plugin Hooks
 *
 * Interfaces for plugin hooks that extend VaultStadio functionality.
 */

package com.vaultstadio.plugins.hooks

import com.vaultstadio.core.domain.model.StorageItem
import java.io.InputStream

/**
 * Hook for extracting metadata from files.
 */
interface MetadataExtractionHook {

    /**
     * Extracts metadata from a file.
     *
     * @param item The storage item
     * @param stream Input stream of the file content
     * @return Map of metadata key-value pairs
     */
    suspend fun extractMetadata(item: StorageItem, stream: InputStream): Map<String, String>

    /**
     * Returns the MIME types this hook can process.
     */
    fun getSupportedMimeTypes(): Set<String>
}

/**
 * Hook for generating thumbnails.
 */
interface ThumbnailHook {

    /**
     * Generates a thumbnail for a file.
     *
     * @param item The storage item
     * @param stream Input stream of the file content
     * @return Thumbnail bytes or null if not supported
     */
    suspend fun generateThumbnail(item: StorageItem, stream: InputStream): ByteArray?

    /**
     * Returns the MIME type of generated thumbnails.
     */
    fun getThumbnailMimeType(): String

    /**
     * Returns the maximum size (pixels) for thumbnails.
     */
    fun getThumbnailMaxSize(): Int
}

/**
 * Hook for file classification.
 */
interface ClassificationHook {

    /**
     * Classifies a file.
     *
     * @param item The storage item
     * @param stream Input stream of the file content
     * @return Classification result with labels and confidence scores
     */
    suspend fun classify(item: StorageItem, stream: InputStream): ClassificationResult
}

/**
 * Classification result.
 */
data class ClassificationResult(
    val labels: List<ClassificationLabel>,
    val confidence: Double = 0.0,
)

/**
 * Classification label with confidence.
 */
data class ClassificationLabel(
    val name: String,
    val confidence: Double,
    val category: String? = null,
)

/**
 * Hook for content analysis (e.g., OCR, speech-to-text).
 */
interface ContentAnalysisHook {

    /**
     * Analyzes content and returns extracted text.
     *
     * @param item The storage item
     * @param stream Input stream of the file content
     * @return Extracted text content
     */
    suspend fun analyzeContent(item: StorageItem, stream: InputStream): ContentAnalysisResult
}

/**
 * Content analysis result.
 */
data class ContentAnalysisResult(
    val text: String?,
    val language: String? = null,
    val confidence: Double = 0.0,
    val segments: List<ContentSegment> = emptyList(),
)

/**
 * Content segment (e.g., paragraph, speech segment).
 */
data class ContentSegment(
    val text: String,
    val startOffset: Long = 0,
    val endOffset: Long = 0,
    val confidence: Double = 0.0,
)

/**
 * Hook for file transformation (e.g., format conversion).
 */
interface TransformationHook {

    /**
     * Transforms a file to a different format.
     *
     * @param item The storage item
     * @param stream Input stream of the file content
     * @param targetFormat Target format/MIME type
     * @return Transformed file content
     */
    suspend fun transform(
        item: StorageItem,
        stream: InputStream,
        targetFormat: String,
    ): TransformationResult

    /**
     * Returns supported source formats.
     */
    fun getSupportedSourceFormats(): Set<String>

    /**
     * Returns supported target formats.
     */
    fun getSupportedTargetFormats(): Set<String>
}

/**
 * Transformation result.
 */
data class TransformationResult(
    val data: ByteArray,
    val mimeType: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformationResult) return false
        return data.contentEquals(other.data) && mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}
