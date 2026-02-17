/**
 * VaultStadio Share Routes
 */

package com.vaultstadio.api.routes.share

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.CreateShareRequest
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.core.domain.service.AccessShareInput
import com.vaultstadio.core.domain.service.CreateShareInput
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.domain.service.StorageService
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.get as koinGet

fun Route.shareRoutes() {
    route("/shares") {
        // List user's shares (created by user)
        get {
            val shareService: ShareService = call.application.koinGet()
            val user = call.user!!
            val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: true

            shareService.getSharesByUser(user.id, activeOnly).fold(
                { error -> throw error },
                { shares ->
                    val baseUrl = getBaseUrl(call)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = shares.map { it.toResponse(baseUrl) },
                        ),
                    )
                },
            )
        }

        // List shares shared with the current user
        get("/shared-with-me") {
            val shareService: ShareService = call.application.koinGet()
            val user = call.user!!
            val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: true

            shareService.getSharesSharedWithUser(user.id, activeOnly).fold(
                { error -> throw error },
                { shares ->
                    val baseUrl = getBaseUrl(call)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = shares.map { it.toResponse(baseUrl) },
                        ),
                    )
                },
            )
        }

        // Create share
        post {
            val shareService: ShareService = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<CreateShareRequest>()

            val input = CreateShareInput(
                itemId = request.itemId,
                userId = user.id,
                expirationDays = request.expirationDays,
                password = request.password,
                maxDownloads = request.maxDownloads,
            )

            shareService.createShare(input).fold(
                { error -> throw error },
                { share ->
                    val baseUrl = getBaseUrl(call)
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(success = true, data = share.toResponse(baseUrl)),
                    )
                },
            )
        }

        // Get shares for an item
        get("/item/{itemId}") {
            val shareService: ShareService = call.application.koinGet()
            val user = call.user!!
            val itemId = call.parameters["itemId"]!!

            shareService.getSharesByItem(itemId, user.id).fold(
                { error -> throw error },
                { shares ->
                    val baseUrl = getBaseUrl(call)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = shares.map { it.toResponse(baseUrl) },
                        ),
                    )
                },
            )
        }

        // Delete share
        delete("/{shareId}") {
            val shareService: ShareService = call.application.koinGet()
            val user = call.user!!
            val shareId = call.parameters["shareId"]!!

            shareService.deleteShare(shareId, user.id).fold(
                { error -> throw error },
                {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<Unit>(success = true),
                    )
                },
            )
        }
    }
}

// Public share access routes (no auth required)
fun Route.publicShareRoutes() {
    route("/share/{token}") {
        // Get share info
        get {
            val shareService: ShareService = call.application.koinGet()
            val token = call.parameters["token"]!!
            val password = call.request.queryParameters["password"]

            val input = AccessShareInput(
                token = token,
                password = password,
                ipAddress = call.request.local.remoteAddress,
                userAgent = call.request.headers["User-Agent"],
            )

            shareService.accessShare(input).fold(
                { error -> throw error },
                { (share, item) ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = mapOf(
                                "item" to item.toResponse(),
                                "hasPassword" to (share.password != null),
                                "expiresAt" to share.expiresAt,
                            ),
                        ),
                    )
                },
            )
        }

        // Download shared file
        get("/download") {
            val shareService: ShareService = call.application.koinGet()
            val storageService: StorageService = call.application.koinGet()
            val token = call.parameters["token"]!!
            val password = call.request.queryParameters["password"]

            val input = AccessShareInput(
                token = token,
                password = password,
                ipAddress = call.request.local.remoteAddress,
                userAgent = call.request.headers["User-Agent"],
            )

            shareService.accessShare(input).fold(
                { error -> throw error },
                { (_, item) ->
                    // Use a system user context for downloads
                    storageService.downloadFile(item.id, item.ownerId).fold(
                        { error -> throw error },
                        { (downloadItem, stream) ->
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    downloadItem.name,
                                ).toString(),
                            )
                            call.respondOutputStream(
                                contentType = downloadItem.mimeType?.let { ContentType.parse(it) }
                                    ?: ContentType.Application.OctetStream,
                                contentLength = downloadItem.size,
                            ) {
                                stream.copyTo(this)
                            }
                        },
                    )
                },
            )
        }
    }
}

private fun getBaseUrl(call: ApplicationCall): String {
    val scheme = call.request.headers["X-Forwarded-Proto"] ?: "http"
    val host = call.request.headers["X-Forwarded-Host"] ?: call.request.local.serverHost
    val port = call.request.local.serverPort

    return if (port == 80 || port == 443) {
        "$scheme://$host"
    } else {
        "$scheme://$host:$port"
    }
}
