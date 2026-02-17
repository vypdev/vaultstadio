/**
 * VaultStadio Image Metadata Plugin
 *
 * Extracts EXIF, IPTC, XMP, and other metadata from image files.
 * Supports JPEG, PNG, TIFF, GIF, WebP, HEIC, and RAW formats.
 */

package com.vaultstadio.plugins.image

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.iptc.IptcDirectory
import com.drew.metadata.jpeg.JpegDirectory
import com.drew.metadata.png.PngDirectory
import com.drew.metadata.xmp.XmpDirectory
import com.vaultstadio.core.domain.event.EventHandlerResult
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.plugins.api.AbstractPlugin
import com.vaultstadio.plugins.api.PluginMetadata
import com.vaultstadio.plugins.api.PluginPermission
import com.vaultstadio.plugins.config.FieldType
import com.vaultstadio.plugins.config.pluginConfiguration
import com.vaultstadio.plugins.context.PluginContext
import com.vaultstadio.plugins.hooks.MetadataExtractionHook
import com.vaultstadio.plugins.hooks.ThumbnailHook
import com.vaultstadio.plugins.metadata.MetadataKeys
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * Plugin for extracting metadata from image files.
 *
 * Supports the following metadata types:
 * - EXIF: Camera settings, date taken, GPS coordinates
 * - IPTC: Keywords, caption, copyright
 * - XMP: Extended metadata
 * - Image dimensions, format, color space
 */
class ImageMetadataPlugin : AbstractPlugin(), MetadataExtractionHook, ThumbnailHook {

    override val metadata = PluginMetadata(
        id = "com.vaultstadio.plugins.image-metadata",
        name = "Image Metadata Extractor",
        version = "1.0.0",
        description = "Extracts EXIF, IPTC, and XMP metadata from images",
        author = "VaultStadio",
        website = "https://vaultstadio.io",
        permissions = setOf(
            PluginPermission.READ_FILES,
            PluginPermission.WRITE_METADATA,
        ),
        supportedMimeTypes = setOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/tiff",
            "image/heic",
            "image/heif",
            "image/bmp",
            "image/x-raw",
            "image/x-canon-cr2",
            "image/x-nikon-nef",
            "image/x-sony-arw",
        ),
    )

    private val configSchema = pluginConfiguration {
        group("general", "General Settings") {
            field("extractGps", "Extract GPS Coordinates", FieldType.BOOLEAN) {
                description = "Extract GPS location data from images"
                defaultValue = true
            }
            field("extractThumbnails", "Generate Thumbnails", FieldType.BOOLEAN) {
                description = "Generate thumbnail images"
                defaultValue = true
            }
            field("thumbnailSize", "Thumbnail Size", FieldType.NUMBER) {
                description = "Maximum thumbnail dimension in pixels"
                defaultValue = 256
                validation = "value >= 64 && value <= 1024"
            }
        }
        group("advanced", "Advanced Settings") {
            field("extractAllTags", "Extract All Tags", FieldType.BOOLEAN) {
                description = "Extract all available metadata tags"
                defaultValue = false
            }
            field("logUnknownTags", "Log Unknown Tags", FieldType.BOOLEAN) {
                description = "Log unknown or unrecognized metadata tags"
                defaultValue = false
            }
        }
    }

    override fun getConfigurationSchema() = configSchema

    private lateinit var pluginContext: PluginContext
    private var extractGps: Boolean = true
    private var extractThumbnails: Boolean = true
    private var thumbnailSize: Int = 256
    private var extractAllTags: Boolean = false

    override suspend fun onInitialize(context: PluginContext) {
        pluginContext = context
        loadConfiguration()

        // Subscribe to file upload events
        context.eventBus.subscribe<FileEvent.Uploaded>(metadata.id) { event ->
            handleFileUploaded(event)
        }

        logger.info { "Image Metadata Plugin initialized" }
    }

    private fun loadConfiguration() {
        extractGps = pluginContext.config.getBoolean("extractGps") ?: true
        extractThumbnails = pluginContext.config.getBoolean("extractThumbnails") ?: true
        thumbnailSize = pluginContext.config.getInt("thumbnailSize") ?: 256
        extractAllTags = pluginContext.config.getBoolean("extractAllTags") ?: false
    }

    private suspend fun handleFileUploaded(event: FileEvent.Uploaded): EventHandlerResult {
        val item = event.item

        // Check if it's an image
        if (!metadata.supportedMimeTypes.contains(item.mimeType)) {
            return EventHandlerResult.Success
        }

        return try {
            // Get the file stream
            val streamResult = pluginContext.storage.retrieve(item.storageKey ?: return EventHandlerResult.Success)

            streamResult.fold(
                { error ->
                    logger.error { "Failed to retrieve file: ${error.message}" }
                    EventHandlerResult.Error(error)
                },
                { stream ->
                    stream.use { extractAndSaveMetadata(item, it) }
                    EventHandlerResult.Success
                },
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to extract image metadata for ${item.id}" }
            EventHandlerResult.Error(e)
        }
    }

    private suspend fun extractAndSaveMetadata(item: StorageItem, stream: InputStream) {
        try {
            val imageMetadata = ImageMetadataReader.readMetadata(stream)
            val metadataList = mutableListOf<StorageItemMetadata>()
            val now = Clock.System.now()

            // Extract EXIF data
            imageMetadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)?.let { exif ->
                extractExifData(exif, item.id, metadataList, now)
            }

            imageMetadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)?.let { exifSub ->
                extractExifSubData(exifSub, item.id, metadataList, now)
            }

            // Extract GPS data
            if (extractGps) {
                imageMetadata.getFirstDirectoryOfType(GpsDirectory::class.java)?.let { gps ->
                    extractGpsData(gps, item.id, metadataList, now)
                }
            }

            // Extract JPEG-specific data
            imageMetadata.getFirstDirectoryOfType(JpegDirectory::class.java)?.let { jpeg ->
                extractJpegData(jpeg, item.id, metadataList, now)
            }

            // Extract PNG-specific data
            imageMetadata.getFirstDirectoryOfType(PngDirectory::class.java)?.let { png ->
                extractPngData(png, item.id, metadataList, now)
            }

            // Extract IPTC data
            imageMetadata.getFirstDirectoryOfType(IptcDirectory::class.java)?.let { iptc ->
                extractIptcData(iptc, item.id, metadataList, now)
            }

            // Extract XMP data
            imageMetadata.getFirstDirectoryOfType(XmpDirectory::class.java)?.let { xmp ->
                extractXmpData(xmp, item.id, metadataList, now)
            }

            // Save all metadata
            if (metadataList.isNotEmpty()) {
                pluginContext.metadata.saveAll(metadataList)
                logger.info { "Saved ${metadataList.size} metadata entries for ${item.name}" }
            }
        } catch (e: ImageProcessingException) {
            logger.warn { "Could not process image metadata: ${e.message}" }
        }
    }

    private fun extractExifData(
        exif: ExifIFD0Directory,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        exif.getString(ExifIFD0Directory.TAG_MAKE)?.let {
            list.add(createMetadata(itemId, MetadataKeys.CAMERA_MAKE, it, now))
        }
        exif.getString(ExifIFD0Directory.TAG_MODEL)?.let {
            list.add(createMetadata(itemId, MetadataKeys.CAMERA_MODEL, it, now))
        }
        exif.getString(ExifIFD0Directory.TAG_SOFTWARE)?.let {
            list.add(createMetadata(itemId, "software", it, now))
        }
        exif.getString(ExifIFD0Directory.TAG_ARTIST)?.let {
            list.add(createMetadata(itemId, "artist", it, now))
        }
        exif.getString(ExifIFD0Directory.TAG_COPYRIGHT)?.let {
            list.add(createMetadata(itemId, "copyright", it, now))
        }
        exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION)?.let {
            list.add(createMetadata(itemId, "orientation", it.toString(), now))
        }
        exif.getInteger(ExifIFD0Directory.TAG_X_RESOLUTION)?.let {
            list.add(createMetadata(itemId, "xResolution", it.toString(), now))
        }
        exif.getInteger(ExifIFD0Directory.TAG_Y_RESOLUTION)?.let {
            list.add(createMetadata(itemId, "yResolution", it.toString(), now))
        }
    }

    private fun extractExifSubData(
        exifSub: ExifSubIFDDirectory,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        exifSub.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)?.let {
            list.add(createMetadata(itemId, MetadataKeys.DATE_TAKEN, it.toInstant().toString(), now))
        }
        exifSub.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)?.let {
            list.add(createMetadata(itemId, MetadataKeys.EXPOSURE_TIME, "${it.numerator}/${it.denominator}", now))
        }
        exifSub.getRational(ExifSubIFDDirectory.TAG_FNUMBER)?.let {
            list.add(createMetadata(itemId, MetadataKeys.APERTURE, "f/${it.toDouble()}", now))
        }
        exifSub.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)?.let {
            list.add(createMetadata(itemId, MetadataKeys.ISO, it.toString(), now))
        }
        exifSub.getRational(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)?.let {
            list.add(createMetadata(itemId, MetadataKeys.FOCAL_LENGTH, "${it.toDouble()}mm", now))
        }
        exifSub.getInteger(ExifSubIFDDirectory.TAG_FLASH)?.let {
            val flashUsed = (it and 1) == 1
            list.add(createMetadata(itemId, "flashUsed", flashUsed.toString(), now))
        }
        exifSub.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)?.let {
            list.add(createMetadata(itemId, MetadataKeys.WIDTH, it.toString(), now))
        }
        exifSub.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)?.let {
            list.add(createMetadata(itemId, MetadataKeys.HEIGHT, it.toString(), now))
        }
        exifSub.getString(ExifSubIFDDirectory.TAG_LENS_MODEL)?.let {
            list.add(createMetadata(itemId, "lensModel", it, now))
        }
    }

    private fun extractGpsData(
        gps: GpsDirectory,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        gps.geoLocation?.let { location ->
            list.add(createMetadata(itemId, MetadataKeys.GPS_LATITUDE, location.latitude.toString(), now))
            list.add(createMetadata(itemId, MetadataKeys.GPS_LONGITUDE, location.longitude.toString(), now))
            list.add(createMetadata(itemId, "gpsCoordinates", "${location.latitude},${location.longitude}", now))
        }
        gps.getRational(GpsDirectory.TAG_ALTITUDE)?.let {
            list.add(createMetadata(itemId, MetadataKeys.GPS_ALTITUDE, it.toDouble().toString(), now))
        }
    }

    private fun extractJpegData(
        jpeg: JpegDirectory,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        jpeg.getInteger(JpegDirectory.TAG_IMAGE_WIDTH)?.let {
            // Only add if not already set from EXIF
            if (list.none { m -> m.key == MetadataKeys.WIDTH }) {
                list.add(createMetadata(itemId, MetadataKeys.WIDTH, it.toString(), now))
            }
        }
        jpeg.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT)?.let {
            if (list.none { m -> m.key == MetadataKeys.HEIGHT }) {
                list.add(createMetadata(itemId, MetadataKeys.HEIGHT, it.toString(), now))
            }
        }
        jpeg.getInteger(JpegDirectory.TAG_DATA_PRECISION)?.let {
            list.add(createMetadata(itemId, MetadataKeys.BIT_DEPTH, it.toString(), now))
        }
    }

    private fun extractPngData(
        png: PngDirectory,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        png.getInteger(PngDirectory.TAG_IMAGE_WIDTH)?.let {
            if (list.none { m -> m.key == MetadataKeys.WIDTH }) {
                list.add(createMetadata(itemId, MetadataKeys.WIDTH, it.toString(), now))
            }
        }
        png.getInteger(PngDirectory.TAG_IMAGE_HEIGHT)?.let {
            if (list.none { m -> m.key == MetadataKeys.HEIGHT }) {
                list.add(createMetadata(itemId, MetadataKeys.HEIGHT, it.toString(), now))
            }
        }
        png.getInteger(PngDirectory.TAG_BITS_PER_SAMPLE)?.let {
            list.add(createMetadata(itemId, MetadataKeys.BIT_DEPTH, it.toString(), now))
        }
        png.getString(PngDirectory.TAG_COLOR_TYPE)?.let {
            list.add(createMetadata(itemId, MetadataKeys.COLOR_SPACE, it, now))
        }
    }

    private fun extractIptcData(
        iptc: IptcDirectory,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        iptc.getString(IptcDirectory.TAG_HEADLINE)?.let {
            list.add(createMetadata(itemId, "headline", it, now))
        }
        iptc.getString(IptcDirectory.TAG_CAPTION)?.let {
            list.add(createMetadata(itemId, MetadataKeys.DESCRIPTION, it, now))
        }
        iptc.getStringArray(IptcDirectory.TAG_KEYWORDS)?.let { keywords ->
            list.add(createMetadata(itemId, "keywords", keywords.joinToString(","), now))
        }
        iptc.getString(IptcDirectory.TAG_COPYRIGHT_NOTICE)?.let {
            list.add(createMetadata(itemId, "iptcCopyright", it, now))
        }
        iptc.getString(IptcDirectory.TAG_BY_LINE)?.let {
            list.add(createMetadata(itemId, "photographer", it, now))
        }
        iptc.getString(IptcDirectory.TAG_CITY)?.let {
            list.add(createMetadata(itemId, "city", it, now))
        }
        iptc.getString(IptcDirectory.TAG_PROVINCE_OR_STATE)?.let {
            list.add(createMetadata(itemId, "state", it, now))
        }
        iptc.getString(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_NAME)?.let {
            list.add(createMetadata(itemId, "country", it, now))
        }
    }

    private fun extractXmpData(
        xmp: XmpDirectory,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        xmp.xmpProperties?.forEach { (key, value) ->
            if (extractAllTags || isImportantXmpKey(key)) {
                val cleanKey = key.substringAfterLast(":")
                list.add(createMetadata(itemId, "xmp_$cleanKey", value, now))
            }
        }
    }

    private fun isImportantXmpKey(key: String): Boolean {
        val importantKeys = setOf(
            "dc:title", "dc:description", "dc:creator", "dc:rights",
            "xmp:Rating", "xmp:Label", "xmp:CreateDate",
            "photoshop:ColorMode", "photoshop:ICCProfile",
        )
        return importantKeys.any { key.contains(it, ignoreCase = true) }
    }

    private fun createMetadata(
        itemId: String,
        key: String,
        value: String,
        now: Instant,
    ) = StorageItemMetadata(
        id = "",
        itemId = itemId,
        pluginId = metadata.id,
        key = key,
        value = value,
        createdAt = now,
        updatedAt = now,
    )

    // MetadataExtractionHook implementation
    override suspend fun extractMetadata(item: StorageItem, stream: InputStream): Map<String, String> {
        if (!metadata.supportedMimeTypes.contains(item.mimeType)) {
            return emptyMap()
        }

        return try {
            val result = mutableMapOf<String, String>()
            val imageMetadata = ImageMetadataReader.readMetadata(stream)

            // Quick extraction for dimensions
            imageMetadata.getFirstDirectoryOfType(JpegDirectory::class.java)?.let { jpeg ->
                jpeg.getInteger(JpegDirectory.TAG_IMAGE_WIDTH)?.let { result[MetadataKeys.WIDTH] = it.toString() }
                jpeg.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT)?.let { result[MetadataKeys.HEIGHT] = it.toString() }
            }

            imageMetadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)?.let { exif ->
                exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)?.let {
                    result[MetadataKeys.DATE_TAKEN] = it.toInstant().toString()
                }
            }

            imageMetadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)?.let { exif ->
                exif.getString(ExifIFD0Directory.TAG_MODEL)?.let { result[MetadataKeys.CAMERA_MODEL] = it }
            }

            result
        } catch (e: Exception) {
            logger.warn { "Failed to extract metadata: ${e.message}" }
            emptyMap()
        }
    }

    override fun getSupportedMimeTypes(): Set<String> = metadata.supportedMimeTypes

    // ThumbnailHook implementation
    override suspend fun generateThumbnail(item: StorageItem, stream: InputStream): ByteArray? {
        if (!extractThumbnails || !metadata.supportedMimeTypes.contains(item.mimeType)) {
            return null
        }

        return try {
            val originalImage = ImageIO.read(stream) ?: run {
                logger.warn { "Failed to read image for thumbnail: ${item.name}" }
                return null
            }

            val thumbnail = createThumbnail(originalImage, thumbnailSize)
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(thumbnail, "jpeg", outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to generate thumbnail for ${item.name}: ${e.message}" }
            null
        }
    }

    /**
     * Creates a scaled thumbnail from the original image.
     * Maintains aspect ratio and uses high-quality scaling.
     */
    private fun createThumbnail(original: BufferedImage, maxSize: Int): BufferedImage {
        val originalWidth = original.width
        val originalHeight = original.height

        // Calculate scaled dimensions maintaining aspect ratio
        val scale = minOf(maxSize.toDouble() / originalWidth, maxSize.toDouble() / originalHeight)
        val scaledWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (originalHeight * scale).toInt().coerceAtLeast(1)

        // Create thumbnail with high-quality scaling
        val thumbnail = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = thumbnail.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.drawImage(original, 0, 0, scaledWidth, scaledHeight, null)
        } finally {
            graphics.dispose()
        }

        return thumbnail
    }

    override fun getThumbnailMimeType(): String = "image/jpeg"

    override fun getThumbnailMaxSize(): Int = thumbnailSize

    override suspend fun onShutdown() {
        logger.info { "Image Metadata Plugin shutting down" }
    }
}
