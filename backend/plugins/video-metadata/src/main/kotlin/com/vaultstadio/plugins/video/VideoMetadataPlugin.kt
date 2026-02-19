/**
 * VaultStadio Video Metadata Plugin
 *
 * Extracts metadata from video files using FFprobe (must be installed on the system).
 * Supports MP4, MKV, AVI, MOV, WebM, and other common video formats.
 */

package com.vaultstadio.plugins.video

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Plugin for extracting metadata from video files.
 *
 * Requires FFprobe to be installed and available in PATH.
 * Extracts:
 * - Duration, dimensions, frame rate
 * - Codec information (video and audio)
 * - Bitrate, file format
 * - Chapter information (if available)
 */
class VideoMetadataPlugin : AbstractPlugin(), MetadataExtractionHook, ThumbnailHook {

    override val metadata = PluginMetadata(
        id = "com.vaultstadio.plugins.video-metadata",
        name = "Video Metadata Extractor",
        version = "1.0.0",
        description = "Extracts metadata from video files using FFprobe",
        author = "VaultStadio",
        website = "https://vaultstadio.io",
        permissions = setOf(
            PluginPermission.READ_FILES,
            PluginPermission.WRITE_METADATA,
            PluginPermission.EXECUTE_COMMANDS,
        ),
        supportedMimeTypes = setOf(
            "video/mp4",
            "video/x-matroska",
            "video/webm",
            "video/quicktime",
            "video/x-msvideo",
            "video/x-flv",
            "video/x-ms-wmv",
            "video/mpeg",
            "video/3gpp",
            "video/3gpp2",
            "video/ogg",
        ),
    )

    private val configSchema = pluginConfiguration {
        group("general", "General Settings") {
            field("ffprobePath", "FFprobe Path", FieldType.STRING) {
                description = "Path to FFprobe executable (leave empty for system PATH)"
                defaultValue = "ffprobe"
            }
            field("generateThumbnails", "Generate Thumbnails", FieldType.BOOLEAN) {
                description = "Generate thumbnail images from videos"
                defaultValue = true
            }
            field("thumbnailPosition", "Thumbnail Position", FieldType.NUMBER) {
                description = "Position in seconds to capture thumbnail"
                defaultValue = 5
                validation = "value >= 0"
            }
            field("thumbnailSize", "Thumbnail Size", FieldType.NUMBER) {
                description = "Maximum thumbnail dimension in pixels"
                defaultValue = 320
                validation = "value >= 64 && value <= 1280"
            }
        }
        group("advanced", "Advanced Settings") {
            field("extractChapters", "Extract Chapters", FieldType.BOOLEAN) {
                description = "Extract chapter information if available"
                defaultValue = true
            }
            field("extractSubtitles", "List Subtitle Tracks", FieldType.BOOLEAN) {
                description = "List available subtitle tracks"
                defaultValue = true
            }
            field("timeout", "Command Timeout", FieldType.NUMBER) {
                description = "Timeout for FFprobe commands in seconds"
                defaultValue = 30
                validation = "value >= 5 && value <= 300"
            }
        }
    }

    override fun getConfigurationSchema() = configSchema

    private lateinit var pluginContext: PluginContext
    private var ffprobePath: String = "ffprobe"
    private var generateThumbnails: Boolean = true
    private var thumbnailPosition: Int = 5
    private var thumbnailSize: Int = 320
    private var extractChapters: Boolean = true
    private var timeout: Int = 30

    // Cache FFprobe availability check
    private var ffprobeAvailable: Boolean? = null
    private var ffmpegAvailable: Boolean? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun onInitialize(context: PluginContext) {
        pluginContext = context
        loadConfiguration()

        // Check if FFprobe/FFmpeg are available and cache results
        ffprobeAvailable = checkFFprobeAvailable()
        ffmpegAvailable = checkFFmpegAvailable()

        if (ffprobeAvailable != true) {
            logger.warn {
                "FFprobe not found at '$ffprobePath'. Video metadata extraction will be limited to basic info."
            }
        }
        if (generateThumbnails && ffmpegAvailable != true) {
            logger.warn { "FFmpeg not found. Video thumbnail generation will be disabled." }
        }

        // Subscribe to file upload events
        context.eventBus.subscribe<FileEvent.Uploaded>(metadata.id) { event ->
            handleFileUploaded(event)
        }

        logger.info { "Video Metadata Plugin initialized (FFprobe: $ffprobeAvailable, FFmpeg: $ffmpegAvailable)" }
    }

    private fun loadConfiguration() {
        ffprobePath = pluginContext.config.getString("ffprobePath") ?: "ffprobe"
        generateThumbnails = pluginContext.config.getBoolean("generateThumbnails") ?: true
        thumbnailPosition = pluginContext.config.getInt("thumbnailPosition") ?: 5
        thumbnailSize = pluginContext.config.getInt("thumbnailSize") ?: 320
        extractChapters = pluginContext.config.getBoolean("extractChapters") ?: true
        timeout = pluginContext.config.getInt("timeout") ?: 30
    }

    /**
     * Checks if FFprobe is available on the system.
     */
    private suspend fun checkFFprobeAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(ffprobePath, "-version")
                .redirectErrorStream(true)
                .start()

            val result = process.waitFor()
            result == 0
        } catch (e: Exception) {
            logger.debug { "FFprobe check failed: ${e.message}" }
            false
        }
    }

    /**
     * Checks if FFmpeg is available on the system (for thumbnail generation).
     */
    private suspend fun checkFFmpegAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("ffmpeg", "-version")
                .redirectErrorStream(true)
                .start()

            val result = process.waitFor()
            result == 0
        } catch (e: Exception) {
            logger.debug { "FFmpeg check failed: ${e.message}" }
            false
        }
    }

    /**
     * Returns true if FFprobe is available (cached).
     */
    fun isFFprobeAvailable(): Boolean = ffprobeAvailable ?: false

    /**
     * Returns true if FFmpeg is available (cached).
     */
    fun isFFmpegAvailable(): Boolean = ffmpegAvailable ?: false

    private suspend fun handleFileUploaded(event: FileEvent.Uploaded): EventHandlerResult {
        val item = event.item

        if (!metadata.supportedMimeTypes.contains(item.mimeType)) {
            return EventHandlerResult.Success
        }

        // If FFprobe is not available, save basic metadata only
        if (!isFFprobeAvailable()) {
            saveBasicMetadata(item)
            return EventHandlerResult.Success
        }

        return try {
            // For video files, we need to save to a temp file for FFprobe
            val streamResult = pluginContext.storage.retrieve(item.storageKey ?: return EventHandlerResult.Success)

            streamResult.fold(
                { error ->
                    logger.error { "Failed to retrieve file: ${error.message}" }
                    // Fallback to basic metadata
                    saveBasicMetadata(item)
                    EventHandlerResult.Success // Don't fail the event
                },
                { stream ->
                    val tempFile = createTempFile(item, stream)
                    try {
                        extractAndSaveMetadata(item, tempFile)
                        EventHandlerResult.Success
                    } finally {
                        tempFile.delete()
                    }
                },
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to extract video metadata for ${item.id}, saving basic info" }
            saveBasicMetadata(item)
            EventHandlerResult.Success // Don't fail the event
        }
    }

    /**
     * Saves basic metadata when FFprobe is not available.
     * This includes file extension based codec detection and MIME type info.
     */
    private suspend fun saveBasicMetadata(item: StorageItem) {
        val now = Clock.System.now()
        val metadataList = mutableListOf<StorageItemMetadata>()

        // Add MIME type info
        item.mimeType?.let { mimeType ->
            metadataList.add(createMetadata(item.id, "mimeType", mimeType, now))

            // Try to infer container format from extension
            val extension = item.name.substringAfterLast(".", "").lowercase()
            val format = when (extension) {
                "mp4", "m4v" -> "MPEG-4"
                "mkv" -> "Matroska"
                "webm" -> "WebM"
                "avi" -> "AVI"
                "mov" -> "QuickTime"
                "wmv" -> "Windows Media"
                "flv" -> "Flash Video"
                "mpg", "mpeg" -> "MPEG"
                "3gp" -> "3GPP"
                "ogv", "ogg" -> "Ogg"
                else -> null
            }
            format?.let { metadataList.add(createMetadata(item.id, "containerFormat", it, now)) }
        }

        // Mark that FFprobe was not available
        metadataList.add(createMetadata(item.id, "extractionLimited", "true", now))
        metadataList.add(createMetadata(item.id, "extractionNote", "FFprobe not available, metadata is limited", now))

        if (metadataList.isNotEmpty()) {
            pluginContext.metadata.saveAll(metadataList)
            logger.debug { "Saved basic metadata for ${item.name} (FFprobe not available)" }
        }
    }

    private suspend fun createTempFile(item: StorageItem, stream: InputStream): File =
        withContext(Dispatchers.IO) {
            val extension = item.name.substringAfterLast(".", "mp4")
            val tempFile = File.createTempFile(
                "vaultstadio_video_",
                ".$extension",
                pluginContext.tempDirectory.toFile(),
            )
            tempFile.outputStream().use { output ->
                stream.copyTo(output)
            }
            tempFile
        }

    private suspend fun extractAndSaveMetadata(item: StorageItem, file: File) {
        val probeData = runFFprobe(file) ?: return

        val metadataList = mutableListOf<StorageItemMetadata>()
        val now = Clock.System.now()

        // Format info
        probeData.format?.let { format ->
            format.duration?.toDoubleOrNull()?.let {
                metadataList.add(createMetadata(item.id, MetadataKeys.DURATION, it.toLong().toString(), now))
                metadataList.add(createMetadata(item.id, "durationFormatted", formatDuration(it), now))
            }
            format.bit_rate?.toLongOrNull()?.let {
                metadataList.add(createMetadata(item.id, MetadataKeys.BITRATE, it.toString(), now))
            }
            format.format_name?.let {
                metadataList.add(createMetadata(item.id, "containerFormat", it, now))
            }
            format.format_long_name?.let {
                metadataList.add(createMetadata(item.id, "formatDescription", it, now))
            }

            // Extract tags
            format.tags?.title?.let {
                metadataList.add(createMetadata(item.id, MetadataKeys.TITLE, it, now))
            }
            format.tags?.artist?.let {
                metadataList.add(createMetadata(item.id, "artist", it, now))
            }
            format.tags?.comment?.let {
                metadataList.add(createMetadata(item.id, MetadataKeys.DESCRIPTION, it, now))
            }
            format.tags?.creation_time?.let {
                metadataList.add(createMetadata(item.id, "creationTime", it, now))
            }
        }

        // Stream info
        probeData.streams?.forEach { stream ->
            when (stream.codec_type) {
                "video" -> extractVideoStreamInfo(stream, item.id, metadataList, now)
                "audio" -> extractAudioStreamInfo(stream, item.id, metadataList, now)
                "subtitle" -> {
                    val language = stream.tags?.language ?: stream.tags?.title ?: "unknown"
                    val existing = metadataList.find { it.key == "subtitleTracks" }
                    val current = existing?.value ?: ""
                    val updated = if (current.isEmpty()) language else "$current,$language"
                    if (existing != null) {
                        metadataList.remove(existing)
                    }
                    metadataList.add(createMetadata(item.id, "subtitleTracks", updated, now))
                }
            }
        }

        // Chapters
        if (extractChapters && probeData.chapters?.isNotEmpty() == true) {
            val chapterList = probeData.chapters.mapIndexed { index, chapter ->
                val title = chapter.tags?.title ?: "Chapter ${index + 1}"
                val startTime = (chapter.start_time?.toDoubleOrNull() ?: 0.0)
                "$title:${formatDuration(startTime)}"
            }
            metadataList.add(createMetadata(item.id, "chapters", chapterList.joinToString(";"), now))
            metadataList.add(createMetadata(item.id, "chapterCount", probeData.chapters.size.toString(), now))
        }

        // Save all metadata
        if (metadataList.isNotEmpty()) {
            pluginContext.metadata.saveAll(metadataList)
            logger.info { "Saved ${metadataList.size} metadata entries for ${item.name}" }
        }
    }

    private fun extractVideoStreamInfo(
        stream: FFprobeStream,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        stream.width?.let {
            list.add(createMetadata(itemId, MetadataKeys.WIDTH, it.toString(), now))
        }
        stream.height?.let {
            list.add(createMetadata(itemId, MetadataKeys.HEIGHT, it.toString(), now))
        }
        stream.codec_name?.let {
            list.add(createMetadata(itemId, MetadataKeys.VIDEO_CODEC, it, now))
        }
        stream.codec_long_name?.let {
            list.add(createMetadata(itemId, "videoCodecDescription", it, now))
        }
        stream.profile?.let {
            list.add(createMetadata(itemId, "videoProfile", it, now))
        }
        stream.pix_fmt?.let {
            list.add(createMetadata(itemId, "pixelFormat", it, now))
        }
        stream.r_frame_rate?.let { frameRate ->
            // Parse "30000/1001" format
            val parts = frameRate.split("/")
            if (parts.size == 2) {
                val fps = parts[0].toDoubleOrNull()?.div(parts[1].toDoubleOrNull() ?: 1.0) ?: 0.0
                list.add(createMetadata(itemId, MetadataKeys.FRAME_RATE, String.format("%.2f", fps), now))
            } else {
                list.add(createMetadata(itemId, MetadataKeys.FRAME_RATE, frameRate, now))
            }
        }
        stream.bit_rate?.toLongOrNull()?.let {
            list.add(createMetadata(itemId, "videoBitrate", it.toString(), now))
        }
        stream.display_aspect_ratio?.let {
            list.add(createMetadata(itemId, "aspectRatio", it, now))
        }

        // HDR info
        stream.color_space?.let {
            list.add(createMetadata(itemId, MetadataKeys.COLOR_SPACE, it, now))
        }
        stream.color_transfer?.let {
            list.add(createMetadata(itemId, "colorTransfer", it, now))
            if (it.contains("smpte2084") || it.contains("arib-std-b67")) {
                list.add(createMetadata(itemId, "isHDR", "true", now))
            }
        }
    }

    private fun extractAudioStreamInfo(
        stream: FFprobeStream,
        itemId: String,
        list: MutableList<StorageItemMetadata>,
        now: Instant,
    ) {
        stream.codec_name?.let {
            list.add(createMetadata(itemId, MetadataKeys.AUDIO_CODEC, it, now))
        }
        stream.sample_rate?.toIntOrNull()?.let {
            list.add(createMetadata(itemId, MetadataKeys.SAMPLE_RATE, it.toString(), now))
        }
        stream.channels?.let {
            list.add(createMetadata(itemId, MetadataKeys.CHANNELS, it.toString(), now))
        }
        stream.channel_layout?.let {
            list.add(createMetadata(itemId, "channelLayout", it, now))
        }
        stream.bit_rate?.toLongOrNull()?.let {
            list.add(createMetadata(itemId, "audioBitrate", it.toString(), now))
        }
        stream.tags?.language?.let {
            val existing = list.find { m -> m.key == "audioLanguages" }
            val current = existing?.value ?: ""
            val updated = if (current.isEmpty()) it else "$current,$it"
            if (existing != null) {
                list.remove(existing)
            }
            list.add(createMetadata(itemId, "audioLanguages", updated, now))
        }
    }

    private suspend fun runFFprobe(file: File): FFprobeOutput? = withContext(Dispatchers.IO) {
        try {
            val command = listOf(
                ffprobePath,
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                "-show_chapters",
                file.absolutePath,
            )

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0 && output.isNotEmpty()) {
                json.decodeFromString<FFprobeOutput>(output)
            } else {
                logger.warn { "FFprobe failed with exit code $exitCode" }
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to run FFprobe" }
            null
        }
    }

    private fun formatDuration(seconds: Double): String {
        val totalSeconds = seconds.toLong()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
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

        // For quick metadata, we need a file
        val tempFile = createTempFile(item, stream)
        return try {
            val result = mutableMapOf<String, String>()
            val probeData = runFFprobe(tempFile)

            probeData?.format?.duration?.toDoubleOrNull()?.let {
                result[MetadataKeys.DURATION] = it.toLong().toString()
            }

            probeData?.streams?.find { it.codec_type == "video" }?.let { video ->
                video.width?.let { result[MetadataKeys.WIDTH] = it.toString() }
                video.height?.let { result[MetadataKeys.HEIGHT] = it.toString() }
                video.codec_name?.let { result[MetadataKeys.VIDEO_CODEC] = it }
            }

            result
        } finally {
            tempFile.delete()
        }
    }

    override fun getSupportedMimeTypes(): Set<String> = metadata.supportedMimeTypes

    // ThumbnailHook implementation
    override suspend fun generateThumbnail(item: StorageItem, stream: InputStream): ByteArray? {
        // Check if thumbnail generation is enabled and FFmpeg is available
        if (!generateThumbnails || !isFFmpegAvailable() || !metadata.supportedMimeTypes.contains(item.mimeType)) {
            return null
        }

        val tempFile = createTempFile(item, stream)
        return try {
            generateVideoThumbnail(tempFile)
        } finally {
            tempFile.delete()
        }
    }

    private suspend fun generateVideoThumbnail(videoFile: File): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val outputFile = File.createTempFile("vaultstadio_thumb_", ".jpg", pluginContext.tempDirectory.toFile())

            val command = listOf(
                "ffmpeg",
                "-ss", thumbnailPosition.toString(),
                "-i", videoFile.absolutePath,
                "-vf", "scale='min($thumbnailSize,iw)':-1",
                "-frames:v", "1",
                "-y",
                outputFile.absolutePath,
            )

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()

            if (exitCode == 0 && outputFile.exists()) {
                val bytes = outputFile.readBytes()
                outputFile.delete()
                bytes
            } else {
                outputFile.delete()
                null
            }
        } catch (e: Exception) {
            logger.warn { "Failed to generate video thumbnail: ${e.message}" }
            null
        }
    }

    override fun getThumbnailMimeType(): String = "image/jpeg"

    override fun getThumbnailMaxSize(): Int = thumbnailSize

    override suspend fun onShutdown() {
        logger.info { "Video Metadata Plugin shutting down" }
    }
}

// FFprobe JSON models
@Serializable
data class FFprobeOutput(
    val format: FFprobeFormat? = null,
    val streams: List<FFprobeStream>? = null,
    val chapters: List<FFprobeChapter>? = null,
)

@Serializable
data class FFprobeFormat(
    val filename: String? = null,
    val format_name: String? = null,
    val format_long_name: String? = null,
    val duration: String? = null,
    val size: String? = null,
    val bit_rate: String? = null,
    val tags: FFprobeFormatTags? = null,
)

@Serializable
data class FFprobeFormatTags(
    val title: String? = null,
    val artist: String? = null,
    val comment: String? = null,
    val creation_time: String? = null,
)

@Serializable
data class FFprobeStream(
    val index: Int? = null,
    val codec_name: String? = null,
    val codec_long_name: String? = null,
    val codec_type: String? = null,
    val profile: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val pix_fmt: String? = null,
    val r_frame_rate: String? = null,
    val display_aspect_ratio: String? = null,
    val sample_rate: String? = null,
    val channels: Int? = null,
    val channel_layout: String? = null,
    val bit_rate: String? = null,
    val color_space: String? = null,
    val color_transfer: String? = null,
    val tags: FFprobeStreamTags? = null,
)

@Serializable
data class FFprobeStreamTags(
    val language: String? = null,
    val title: String? = null,
)

@Serializable
data class FFprobeChapter(
    val id: Int? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val tags: FFprobeChapterTags? = null,
)

@Serializable
data class FFprobeChapterTags(
    val title: String? = null,
)
