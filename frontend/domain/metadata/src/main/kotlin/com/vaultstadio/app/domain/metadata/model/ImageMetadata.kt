/**
 * Image metadata domain model (EXIF, dimensions, etc.).
 */

package com.vaultstadio.app.domain.metadata.model

import kotlinx.datetime.Instant

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
