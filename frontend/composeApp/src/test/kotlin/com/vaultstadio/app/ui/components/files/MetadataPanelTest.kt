/**
 * VaultStadio Metadata Panel Tests
 */

package com.vaultstadio.app.ui.components.files

import com.vaultstadio.app.domain.metadata.model.DocumentMetadata
import com.vaultstadio.app.domain.metadata.model.ImageMetadata
import com.vaultstadio.app.domain.metadata.model.ThumbnailSize
import com.vaultstadio.app.domain.metadata.model.VideoMetadata
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetadataPanelTest {

    @Test
    fun testImageMetadataModel() {
        val metadata = ImageMetadata(
            width = 1920,
            height = 1080,
            cameraMake = "Canon",
            cameraModel = "EOS R5",
            iso = 400,
            gpsLatitude = 40.7128,
            gpsLongitude = -74.0060,
            keywords = listOf("landscape", "nature"),
        )

        assertTrue(metadata.hasLocation)
        assertEquals("1920x1080", metadata.resolution)
        assertEquals(2, metadata.keywords.size)
    }

    @Test
    fun testImageMetadataWithoutLocation() {
        val metadata = ImageMetadata(
            width = 800,
            height = 600,
        )

        assertFalse(metadata.hasLocation)
        assertEquals("800x600", metadata.resolution)
    }

    @Test
    fun testImageMetadataWithoutDimensions() {
        val metadata = ImageMetadata()

        assertEquals(null, metadata.resolution)
        assertFalse(metadata.hasLocation)
    }

    @Test
    fun testImageMetadataCameraInfo() {
        val metadata = ImageMetadata(
            cameraMake = "Nikon",
            cameraModel = "D850",
            aperture = "f/2.8",
            exposureTime = "1/250",
            iso = 200,
            focalLength = "50mm",
        )

        assertEquals("Nikon", metadata.cameraMake)
        assertEquals("D850", metadata.cameraModel)
        assertEquals("f/2.8", metadata.aperture)
        assertEquals(200, metadata.iso)
    }

    @Test
    fun testVideoMetadataModel() {
        val metadata = VideoMetadata(
            width = 3840,
            height = 2160,
            duration = 120.5,
            durationFormatted = "2:00",
            videoCodec = "H.265",
            audioCodec = "AAC",
            frameRate = 60.0,
            bitrate = 50_000_000,
            isHDR = true,
            subtitleTracks = listOf("English", "Spanish"),
            audioLanguages = listOf("English"),
        )

        assertEquals("3840x2160", metadata.resolution)
        assertTrue(metadata.hasAudio)
        assertTrue(metadata.isHDR)
        assertEquals(2, metadata.subtitleTracks.size)
    }

    @Test
    fun testVideoMetadataWithoutAudio() {
        val metadata = VideoMetadata(
            width = 1920,
            height = 1080,
            videoCodec = "H.264",
        )

        assertFalse(metadata.hasAudio)
        assertFalse(metadata.isHDR)
    }

    @Test
    fun testVideoMetadataWithoutDimensions() {
        val metadata = VideoMetadata(
            duration = 60.0,
            durationFormatted = "1:00",
        )

        assertEquals(null, metadata.resolution)
        assertEquals(60.0, metadata.duration)
    }

    @Test
    fun testDocumentMetadataModel() {
        val now = Clock.System.now()
        val metadata = DocumentMetadata(
            title = "Annual Report",
            author = "John Doe",
            subject = "Financial Analysis",
            keywords = listOf("finance", "report", "2024"),
            pageCount = 50,
            wordCount = 15000,
            isIndexed = true,
            indexedAt = now,
        )

        assertEquals("Annual Report", metadata.title)
        assertEquals(50, metadata.pageCount)
        assertTrue(metadata.isIndexed)
        assertEquals(3, metadata.keywords.size)
    }

    @Test
    fun testDocumentMetadataNotIndexed() {
        val metadata = DocumentMetadata(
            title = "Draft Document",
            author = "Jane Doe",
            isIndexed = false,
        )

        assertFalse(metadata.isIndexed)
        assertEquals(null, metadata.indexedAt)
    }

    @Test
    fun testThumbnailSizeEnumValues() {
        val sizes = ThumbnailSize.entries
        assertEquals(4, sizes.size)
        assertTrue(sizes.contains(ThumbnailSize.SMALL))
        assertTrue(sizes.contains(ThumbnailSize.MEDIUM))
        assertTrue(sizes.contains(ThumbnailSize.LARGE))
        assertTrue(sizes.contains(ThumbnailSize.XLARGE))
    }
}
