/**
 * Video metadata DTO.
 */

package com.vaultstadio.app.data.metadata.dto

import kotlinx.serialization.Serializable

@Serializable
data class VideoMetadataDTO(
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
)
