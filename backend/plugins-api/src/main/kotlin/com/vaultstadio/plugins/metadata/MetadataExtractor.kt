/**
 * VaultStadio Metadata Extractor API
 *
 * Base classes for metadata extraction plugins.
 */

package com.vaultstadio.plugins.metadata

import com.vaultstadio.core.domain.event.EventHandlerResult
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.plugins.api.AbstractPlugin
import com.vaultstadio.plugins.context.PluginContext
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.plugins.hooks.MetadataExtractionHook
import kotlinx.serialization.Serializable
import java.io.InputStream

/**
 * Standard metadata keys used across extractors.
 */
object MetadataKeys {
    // Common
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val AUTHOR = "author"
    const val CREATED_DATE = "created_date"
    const val MODIFIED_DATE = "modified_date"
    const val LANGUAGE = "language"
    const val KEYWORDS = "keywords"
    const val CATEGORY = "category"

    // Media
    const val DURATION = "duration"
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val RESOLUTION = "resolution"
    const val CODEC = "codec"
    const val VIDEO_CODEC = "video_codec"
    const val AUDIO_CODEC = "audio_codec"
    const val BITRATE = "bitrate"
    const val FRAME_RATE = "frame_rate"
    const val ASPECT_RATIO = "aspect_ratio"
    const val BIT_DEPTH = "bit_depth"

    // Audio
    const val ARTIST = "artist"
    const val ALBUM = "album"
    const val TRACK = "track"
    const val GENRE = "genre"
    const val YEAR = "year"
    const val SAMPLE_RATE = "sample_rate"
    const val CHANNELS = "channels"

    // Image
    const val CAMERA_MAKE = "camera_make"
    const val CAMERA_MODEL = "camera_model"
    const val DATE_TAKEN = "date_taken"
    const val EXPOSURE = "exposure"
    const val EXPOSURE_TIME = "exposure_time"
    const val APERTURE = "aperture"
    const val ISO = "iso"
    const val FOCAL_LENGTH = "focal_length"
    const val GPS_LATITUDE = "gps_latitude"
    const val GPS_LONGITUDE = "gps_longitude"
    const val GPS_ALTITUDE = "gps_altitude"
    const val COLOR_SPACE = "color_space"
    const val COLOR_PROFILE = "color_profile"

    // Document
    const val PAGE_COUNT = "page_count"
    const val WORD_COUNT = "word_count"
    const val CHARACTER_COUNT = "character_count"
    const val CREATOR = "creator"
    const val PRODUCER = "producer"
    const val SUBJECT = "subject"

    // Archive
    const val FILE_COUNT = "file_count"
    const val COMPRESSED_SIZE = "compressed_size"
    const val COMPRESSION_RATIO = "compression_ratio"

    // AI/Analysis
    const val AI_TAGS = "ai_tags"
    const val AI_DESCRIPTION = "ai_description"
    const val AI_OBJECTS = "ai_objects"
    const val AI_TEXT = "ai_text"
    const val AI_FACES = "ai_faces"
    const val AI_NSFW_SCORE = "ai_nsfw_score"
    const val AI_SENTIMENT = "ai_sentiment"
    const val CLASSIFICATION = "classification"
    const val CONFIDENCE = "confidence"
}

/**
 * Extracted metadata result.
 */
@Serializable
data class ExtractedMetadata(
    val values: Map<String, String>,
    val thumbnail: ByteArray? = null,
    val textContent: String? = null,
    val warnings: List<String> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExtractedMetadata) return false
        return values == other.values &&
            thumbnail?.contentEquals(other.thumbnail ?: byteArrayOf()) ?: (other.thumbnail == null) &&
            textContent == other.textContent
    }

    override fun hashCode(): Int {
        var result = values.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        result = 31 * result + (textContent?.hashCode() ?: 0)
        return result
    }
}

/**
 * Abstract base class for metadata extraction plugins.
 *
 * Extend this class to create a metadata extractor that automatically
 * processes uploaded files matching supported MIME types.
 */
abstract class MetadataExtractorPlugin : AbstractPlugin(), MetadataExtractionHook {

    /**
     * Plugin configuration.
     */
    abstract val extractorConfig: MetadataExtractorConfig

    override suspend fun onInitialize(context: PluginContext) {
        super.onInitialize(context)

        // Subscribe to file upload events
        context.eventBus.subscribe<FileEvent.Uploaded>(metadata.id) { event ->
            try {
                val item = event.item
                val mimeType = item.mimeType ?: return@subscribe EventHandlerResult.Success

                // Check if we support this MIME type
                if (!supportsMimeType(mimeType)) {
                    return@subscribe EventHandlerResult.Success
                }

                // Read file and extract metadata
                context.storage.readFile(item.id).fold(
                    { error ->
                        context.logger.warn { "Failed to read file for metadata extraction: ${error.message}" }
                    },
                    { inputStream ->
                        try {
                            val extracted = extract(item, inputStream)

                            // Save extracted metadata
                            if (extracted.values.isNotEmpty()) {
                                context.metadata.setValues(item.id, extracted.values)
                                context.logger.info {
                                    "Extracted ${extracted.values.size} metadata fields for ${item.name}"
                                }
                            }

                            // Handle warnings
                            extracted.warnings.forEach { warning ->
                                context.logger.warn { "Metadata extraction warning for ${item.name}: $warning" }
                            }
                        } finally {
                            inputStream.close()
                        }
                    },
                )

                EventHandlerResult.Success
            } catch (e: Exception) {
                context.logger.error(e) { "Error extracting metadata for ${event.item.name}" }
                EventHandlerResult.Error(e)
            }
        }
    }

    /**
     * Checks if this extractor supports the given MIME type.
     */
    private fun supportsMimeType(mimeType: String): Boolean {
        return getSupportedMimeTypes().any { pattern ->
            if (pattern.endsWith("/*")) {
                mimeType.startsWith(pattern.dropLast(1))
            } else {
                mimeType == pattern
            }
        }
    }

    /**
     * Extracts metadata from a file.
     *
     * Implement this method to perform the actual extraction.
     *
     * @param item The storage item
     * @param inputStream Stream to read file content
     * @return Extracted metadata
     */
    abstract suspend fun extract(item: StorageItem, inputStream: InputStream): ExtractedMetadata

    // Implementation of MetadataExtractionHook
    override suspend fun extractMetadata(item: StorageItem, inputStream: InputStream): Map<String, String> {
        return extract(item, inputStream).values
    }
}

/**
 * Configuration for metadata extractors.
 */
@Serializable
data class MetadataExtractorConfig(
    /** Whether to extract metadata automatically on upload */
    val autoExtract: Boolean = true,

    /** Maximum file size to process (in bytes, 0 for unlimited) */
    val maxFileSize: Long = 0,

    /** Whether to extract text content for full-text search */
    val extractTextContent: Boolean = false,

    /** Whether to generate thumbnails */
    val generateThumbnails: Boolean = false,

    /** Thumbnail dimensions */
    val thumbnailWidth: Int = 256,
    val thumbnailHeight: Int = 256,
)
