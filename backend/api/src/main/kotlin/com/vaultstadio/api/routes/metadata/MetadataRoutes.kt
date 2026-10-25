/**
 * VaultStadio Metadata Routes
 *
 * Endpoints for retrieving file metadata extracted by plugins.
 */

package com.vaultstadio.api.routes.metadata

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.application.usecase.metadata.GetItemMetadataUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet

/**
 * Metadata response for a file.
 */
@Serializable
data class FileMetadataResponse(
    val itemId: String,
    val metadata: Map<String, String>,
    val extractedBy: List<String>,
)

/**
 * Image-specific metadata response.
 */
@Serializable
data class ImageMetadataResponse(
    val width: Int?,
    val height: Int?,
    val cameraMake: String?,
    val cameraModel: String?,
    val dateTaken: String?,
    val aperture: String?,
    val exposureTime: String?,
    val iso: Int?,
    val focalLength: String?,
    val gpsLatitude: Double?,
    val gpsLongitude: Double?,
    val gpsAltitude: Double?,
    val colorSpace: String?,
    val bitDepth: Int?,
    val orientation: Int?,
    val description: String?,
    val keywords: List<String>,
    val copyright: String?,
    val artist: String?,
)

/**
 * Video-specific metadata response.
 */
@Serializable
data class VideoMetadataResponse(
    val width: Int?,
    val height: Int?,
    val duration: Long?,
    val durationFormatted: String?,
    val videoCodec: String?,
    val audioCodec: String?,
    val frameRate: String?,
    val bitrate: Long?,
    val aspectRatio: String?,
    val colorSpace: String?,
    val isHDR: Boolean,
    val channels: Int?,
    val sampleRate: Int?,
    val title: String?,
    val artist: String?,
    val chapterCount: Int?,
    val subtitleTracks: List<String>,
    val audioLanguages: List<String>,
)

/**
 * Document-specific metadata response.
 */
@Serializable
data class DocumentMetadataResponse(
    val title: String?,
    val author: String?,
    val subject: String?,
    val keywords: List<String>,
    val creator: String?,
    val producer: String?,
    val creationDate: String?,
    val modificationDate: String?,
    val pageCount: Int?,
    val wordCount: Int?,
    val isIndexed: Boolean,
    val indexedAt: String?,
)

fun Route.metadataRoutes() {
    route("/storage") {
        // Get all metadata for an item
        get("/item/{itemId}/metadata") {
            val getItemMetadataUseCase: GetItemMetadataUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            getItemMetadataUseCase(itemId, user.id).fold(
                { error -> throw error },
                { (_, metadataList) ->
                    val metadataMap = metadataList.associate { it.key to it.value }
                    val plugins = metadataList.map { it.pluginId }.distinct()
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = FileMetadataResponse(
                                itemId = itemId,
                                metadata = metadataMap,
                                extractedBy = plugins,
                            ),
                        ),
                    )
                },
            )
        }

        // Get image-specific metadata
        get("/item/{itemId}/metadata/image") {
            val getItemMetadataUseCase: GetItemMetadataUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            getItemMetadataUseCase(itemId, user.id).fold(
                { error -> throw error },
                { (item, metadataList) ->
                    if (item.mimeType?.startsWith("image/") != true) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError("NOT_IMAGE", "Item is not an image"),
                            ),
                        )
                        return@get
                    }
                    val m = metadataList.associate { it.key to it.value }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = ImageMetadataResponse(
                                width = m["width"]?.toIntOrNull(),
                                height = m["height"]?.toIntOrNull(),
                                cameraMake = m["cameraMake"],
                                cameraModel = m["cameraModel"],
                                dateTaken = m["dateTaken"],
                                aperture = m["aperture"],
                                exposureTime = m["exposureTime"],
                                iso = m["iso"]?.toIntOrNull(),
                                focalLength = m["focalLength"],
                                gpsLatitude = m["gpsLatitude"]?.toDoubleOrNull(),
                                gpsLongitude = m["gpsLongitude"]?.toDoubleOrNull(),
                                gpsAltitude = m["gpsAltitude"]?.toDoubleOrNull(),
                                colorSpace = m["colorSpace"],
                                bitDepth = m["bitDepth"]?.toIntOrNull(),
                                orientation = m["orientation"]?.toIntOrNull(),
                                description = m["description"],
                                keywords = m["keywords"]?.split(",") ?: emptyList(),
                                copyright = m["copyright"] ?: m["iptcCopyright"],
                                artist = m["artist"] ?: m["photographer"],
                            ),
                        ),
                    )
                },
            )
        }

        // Get video-specific metadata
        get("/item/{itemId}/metadata/video") {
            val getItemMetadataUseCase: GetItemMetadataUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            getItemMetadataUseCase(itemId, user.id).fold(
                { error -> throw error },
                { (item, metadataList) ->
                    if (item.mimeType?.startsWith("video/") != true) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError("NOT_VIDEO", "Item is not a video"),
                            ),
                        )
                        return@get
                    }
                    val m = metadataList.associate { it.key to it.value }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = VideoMetadataResponse(
                                width = m["width"]?.toIntOrNull(),
                                height = m["height"]?.toIntOrNull(),
                                duration = m["duration"]?.toLongOrNull(),
                                durationFormatted = m["durationFormatted"],
                                videoCodec = m["videoCodec"],
                                audioCodec = m["audioCodec"],
                                frameRate = m["frameRate"],
                                bitrate = m["bitrate"]?.toLongOrNull(),
                                aspectRatio = m["aspectRatio"],
                                colorSpace = m["colorSpace"],
                                isHDR = m["isHDR"]?.toBoolean() ?: false,
                                channels = m["channels"]?.toIntOrNull(),
                                sampleRate = m["sampleRate"]?.toIntOrNull(),
                                title = m["title"],
                                artist = m["artist"],
                                chapterCount = m["chapterCount"]?.toIntOrNull(),
                                subtitleTracks = m["subtitleTracks"]?.split(",") ?: emptyList(),
                                audioLanguages = m["audioLanguages"]?.split(",") ?: emptyList(),
                            ),
                        ),
                    )
                },
            )
        }

        // Get document-specific metadata
        get("/item/{itemId}/metadata/document") {
            val getItemMetadataUseCase: GetItemMetadataUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            getItemMetadataUseCase(itemId, user.id).fold(
                { error -> throw error },
                { (item, metadataList) ->
                    val docMimeTypes = setOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "text/plain",
                        "text/html",
                        "text/markdown",
                    )
                    if (!docMimeTypes.contains(item.mimeType)) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError("NOT_DOCUMENT", "Item is not a document"),
                            ),
                        )
                        return@get
                    }
                    val m = metadataList.associate { it.key to it.value }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = DocumentMetadataResponse(
                                title = m["title"],
                                author = m["author"],
                                subject = m["subject"],
                                keywords = m["keywords"]?.split(",") ?: emptyList(),
                                creator = m["creator"],
                                producer = m["producer"],
                                creationDate = m["creationDate"],
                                modificationDate = m["modificationDate"],
                                pageCount = m["pageCount"]?.toIntOrNull(),
                                wordCount = m["wordCount"]?.toIntOrNull(),
                                isIndexed = m["indexed"]?.toBoolean() ?: false,
                                indexedAt = m["indexedAt"],
                            ),
                        ),
                    )
                },
            )
        }
    }
}
