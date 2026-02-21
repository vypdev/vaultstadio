/**
 * VaultStadio Thumbnail Routes
 *
 * Endpoints for generating and serving file thumbnails with caching.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.service.CachedThumbnail
import com.vaultstadio.api.service.ThumbnailCache
import com.vaultstadio.api.service.ThumbnailCacheKey
import com.vaultstadio.application.usecase.storage.DownloadFileUseCase
import com.vaultstadio.application.usecase.storage.GetItemUseCase
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.koin.ktor.ext.get as koinGet

/**
 * Thumbnail size options.
 */
enum class ThumbnailSize(val maxDimension: Int) {
    SMALL(64),
    MEDIUM(128),
    LARGE(256),
    XLARGE(512),
}

fun Route.thumbnailRoutes() {
    route("/storage") {
        // Get thumbnail for an item (with caching)
        get("/item/{itemId}/thumbnail") {
            val getItemUseCase: GetItemUseCase = call.application.koinGet()
            val downloadFileUseCase: DownloadFileUseCase = call.application.koinGet()
            val thumbnailCache: ThumbnailCache = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!
            val sizeParam = call.request.queryParameters["size"] ?: "medium"

            val size = when (sizeParam.lowercase()) {
                "small" -> ThumbnailSize.SMALL
                "medium" -> ThumbnailSize.MEDIUM
                "large" -> ThumbnailSize.LARGE
                "xlarge" -> ThumbnailSize.XLARGE
                else -> ThumbnailSize.MEDIUM
            }

            getItemUseCase(itemId, user.id).fold(
                { error -> throw error },
                { item ->
                    val mimeType = item.mimeType

                    // Check if it's an image
                    if (mimeType?.startsWith("image/") != true) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError("NOT_SUPPORTED", "Thumbnails only available for images"),
                            ),
                        )
                        return@get
                    }

                    // Check cache first
                    val cacheKey = ThumbnailCacheKey(
                        itemId = itemId,
                        size = sizeParam.lowercase(),
                        version = item.version,
                    )

                    val cached = thumbnailCache.get(cacheKey)
                    if (cached != null) {
                        // Return cached thumbnail
                        call.respondBytes(
                            bytes = cached.data,
                            contentType = ContentType.parse(cached.contentType),
                        )
                        return@get
                    }

                    // Generate thumbnail
                    downloadFileUseCase(itemId, user.id).fold(
                        { error -> throw error },
                        { (_, stream) ->
                            try {
                                val originalImage = ImageIO.read(stream)
                                if (originalImage == null) {
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        ApiResponse<Unit>(
                                            success = false,
                                            error = ApiError("THUMBNAIL_ERROR", "Failed to read image"),
                                        ),
                                    )
                                    return@get
                                }

                                val thumbnail = generateThumbnail(originalImage, size.maxDimension)

                                val outputStream = ByteArrayOutputStream()
                                ImageIO.write(thumbnail, "png", outputStream)
                                val thumbnailBytes = outputStream.toByteArray()

                                // Cache the thumbnail
                                thumbnailCache.put(
                                    cacheKey,
                                    CachedThumbnail(
                                        data = thumbnailBytes,
                                        contentType = "image/png",
                                        createdAt = Clock.System.now(),
                                        itemVersion = item.version,
                                    ),
                                )

                                call.respondBytes(
                                    bytes = thumbnailBytes,
                                    contentType = ContentType.Image.PNG,
                                )
                            } catch (e: Exception) {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ApiResponse<Unit>(
                                        success = false,
                                        error = ApiError(
                                            "THUMBNAIL_ERROR",
                                            e.message ?: "Failed to generate thumbnail",
                                        ),
                                    ),
                                )
                            }
                        },
                    )
                },
            )
        }

        // Get preview for a file (returns file content for preview)
        get("/item/{itemId}/preview") {
            val getItemUseCase: GetItemUseCase = call.application.koinGet()
            val downloadFileUseCase: DownloadFileUseCase = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            getItemUseCase(itemId, user.id).fold(
                { error -> throw error },
                { item ->
                    val mimeType = item.mimeType

                    val isPreviewable = when {
                        mimeType?.startsWith("image/") == true -> true
                        mimeType?.startsWith("text/") == true -> true
                        mimeType?.startsWith("video/") == true -> true
                        mimeType?.startsWith("audio/") == true -> true
                        mimeType == "application/pdf" -> true
                        mimeType == "application/json" -> true
                        else -> false
                    }

                    if (!isPreviewable) {
                        call.respond(
                            HttpStatusCode.UnsupportedMediaType,
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError("NOT_PREVIEWABLE", "Preview not available for this file type"),
                            ),
                        )
                        return@get
                    }

                    downloadFileUseCase(itemId, user.id).fold(
                        { error -> throw error },
                        { (downloadItem, stream) ->
                            val contentType = when {
                                mimeType?.startsWith("image/") == true -> ContentType.parse(mimeType)
                                mimeType?.startsWith("video/") == true -> ContentType.parse(mimeType)
                                mimeType?.startsWith("audio/") == true -> ContentType.parse(mimeType)
                                mimeType == "application/pdf" -> ContentType.Application.Pdf
                                mimeType == "application/json" -> ContentType.Application.Json
                                else -> ContentType.Text.Plain
                            }

                            call.respondBytes(
                                bytes = stream.readBytes(),
                                contentType = contentType,
                            )
                        },
                    )
                },
            )
        }
    }
}

/**
 * Generate a thumbnail from an image.
 */
private fun generateThumbnail(original: BufferedImage, maxDimension: Int): BufferedImage {
    val width = original.width
    val height = original.height

    // Calculate new dimensions maintaining aspect ratio
    val (newWidth, newHeight) = if (width > height) {
        val scaledHeight = (height.toDouble() / width * maxDimension).toInt()
        Pair(maxDimension, scaledHeight.coerceAtLeast(1))
    } else {
        val scaledWidth = (width.toDouble() / height * maxDimension).toInt()
        Pair(scaledWidth.coerceAtLeast(1), maxDimension)
    }

    // Create scaled image
    val scaledImage = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)

    // Create buffered image
    val thumbnail = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
    val graphics = thumbnail.createGraphics()
    graphics.drawImage(scaledImage, 0, 0, null)
    graphics.dispose()

    return thumbnail
}
