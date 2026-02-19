/**
 * Video metadata domain model.
 */

package com.vaultstadio.app.domain.metadata.model

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
