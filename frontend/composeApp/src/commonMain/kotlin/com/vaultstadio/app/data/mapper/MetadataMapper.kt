/**
 * Metadata Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.metadata.DocumentMetadataDTO
import com.vaultstadio.app.data.dto.metadata.FileMetadataDTO
import com.vaultstadio.app.data.dto.metadata.ImageMetadataDTO
import com.vaultstadio.app.data.dto.metadata.MetadataSearchResultDTO
import com.vaultstadio.app.data.dto.metadata.VideoMetadataDTO
import com.vaultstadio.app.domain.model.DocumentMetadata
import com.vaultstadio.app.domain.model.FileMetadata
import com.vaultstadio.app.domain.model.ImageMetadata
import com.vaultstadio.app.domain.model.MetadataSearchResult
import com.vaultstadio.app.domain.model.VideoMetadata

fun FileMetadataDTO.toDomain() = FileMetadata(itemId, metadata, extractedBy)
fun ImageMetadataDTO.toDomain() = ImageMetadata(
    width, height, cameraMake, cameraModel, dateTaken, aperture, exposureTime, iso,
    focalLength, gpsLatitude, gpsLongitude, gpsAltitude, colorSpace, bitDepth,
    orientation, description, keywords, copyright, artist,
)
fun VideoMetadataDTO.toDomain() = VideoMetadata(
    width, height, duration, durationFormatted, videoCodec, audioCodec, frameRate,
    bitrate, aspectRatio, colorSpace, isHDR, channels, sampleRate, title, artist,
    chapterCount, subtitleTracks, audioLanguages,
)
fun DocumentMetadataDTO.toDomain() = DocumentMetadata(
    title, author, subject, keywords, creator, producer, creationDate, modificationDate,
    pageCount, wordCount, isIndexed, indexedAt,
)
fun MetadataSearchResultDTO.toDomain() = MetadataSearchResult(itemId, itemName, itemPath, pluginId, key, value)
