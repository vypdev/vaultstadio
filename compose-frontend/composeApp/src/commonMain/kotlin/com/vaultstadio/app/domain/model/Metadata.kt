/**
 * Metadata Domain Models
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

data class FileMetadata(
    val itemId: String,
    val metadata: Map<String, String> = emptyMap(),
    val extractedBy: List<String> = emptyList(),
)

data class ImageMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val dateTaken: Instant? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val iso: Int? = null,
    val focalLength: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAltitude: Double? = null,
    val colorSpace: String? = null,
    val bitDepth: Int? = null,
    val orientation: Int? = null,
    val description: String? = null,
    val keywords: List<String> = emptyList(),
    val copyright: String? = null,
    val artist: String? = null,
) {
    val hasLocation: Boolean get() = gpsLatitude != null && gpsLongitude != null
    val resolution: String? get() = if (width != null && height != null) "${width}x$height" else null
}

data class VideoMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
    val durationFormatted: String? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val frameRate: Double? = null,
    val bitrate: Long? = null,
    val aspectRatio: String? = null,
    val colorSpace: String? = null,
    val isHDR: Boolean = false,
    val channels: Int? = null,
    val sampleRate: Int? = null,
    val title: String? = null,
    val artist: String? = null,
    val chapterCount: Int? = null,
    val subtitleTracks: List<String> = emptyList(),
    val audioLanguages: List<String> = emptyList(),
) {
    val resolution: String? get() = if (width != null && height != null) "${width}x$height" else null
    val hasAudio: Boolean get() = audioCodec != null
}

data class DocumentMetadata(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: List<String> = emptyList(),
    val creator: String? = null,
    val producer: String? = null,
    val creationDate: Instant? = null,
    val modificationDate: Instant? = null,
    val pageCount: Int? = null,
    val wordCount: Int? = null,
    val isIndexed: Boolean = false,
    val indexedAt: Instant? = null,
)

data class MetadataSearchResult(
    val itemId: String,
    val itemName: String,
    val itemPath: String,
    val pluginId: String,
    val key: String,
    val value: String,
)

enum class ThumbnailSize { SMALL, MEDIUM, LARGE, XLARGE }
