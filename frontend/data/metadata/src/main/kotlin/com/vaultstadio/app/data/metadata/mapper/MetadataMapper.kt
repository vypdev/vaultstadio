/**
 * Metadata DTO to domain mappers.
 */

package com.vaultstadio.app.data.metadata.mapper

import com.vaultstadio.app.data.metadata.dto.DocumentMetadataDTO
import com.vaultstadio.app.data.metadata.dto.FileMetadataDTO
import com.vaultstadio.app.data.metadata.dto.ImageMetadataDTO
import com.vaultstadio.app.data.metadata.dto.MetadataSearchResultDTO
import com.vaultstadio.app.data.metadata.dto.VideoMetadataDTO
import com.vaultstadio.app.domain.metadata.model.DocumentMetadata
import com.vaultstadio.app.domain.metadata.model.FileMetadata
import com.vaultstadio.app.domain.metadata.model.ImageMetadata
import com.vaultstadio.app.domain.metadata.model.MetadataSearchResult
import com.vaultstadio.app.domain.metadata.model.VideoMetadata

fun FileMetadataDTO.toDomain(): FileMetadata = FileMetadata(itemId, metadata, extractedBy)

fun ImageMetadataDTO.toDomain(): ImageMetadata = ImageMetadata(
    width, height, cameraMake, cameraModel, dateTaken, aperture, exposureTime, iso,
    focalLength, gpsLatitude, gpsLongitude, gpsAltitude, colorSpace, bitDepth,
    orientation, description, keywords, copyright, artist,
)

fun VideoMetadataDTO.toDomain(): VideoMetadata = VideoMetadata(
    width, height, duration, durationFormatted, videoCodec, audioCodec, frameRate,
    bitrate, aspectRatio, colorSpace, isHDR, channels, sampleRate, title, artist,
    chapterCount, subtitleTracks, audioLanguages,
)

fun DocumentMetadataDTO.toDomain(): DocumentMetadata = DocumentMetadata(
    title, author, subject, keywords, creator, producer, creationDate, modificationDate,
    pageCount, wordCount, isIndexed, indexedAt,
)

fun MetadataSearchResultDTO.toDomain(): MetadataSearchResult =
    MetadataSearchResult(itemId, itemName, itemPath, pluginId, key, value)
