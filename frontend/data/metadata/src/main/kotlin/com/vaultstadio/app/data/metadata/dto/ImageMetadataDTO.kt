/**
 * Image metadata DTO.
 */

package com.vaultstadio.app.data.metadata.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ImageMetadataDTO(
    val width: Int? = null,
    val height: Int? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    @kotlinx.serialization.Contextual
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
)
