/**
 * VaultStadio WebDAV Routes
 *
 * WebDAV protocol implementation for mounting VaultStadio as a network drive.
 * Supports standard WebDAV methods: PROPFIND, PROPPATCH, MKCOL, GET, PUT, DELETE, COPY, MOVE, LOCK, UNLOCK.
 *
 * Authentication:
 * - Uses "webdav-basic" authentication provider (HTTP Basic Auth)
 * - Falls back to JWT if Basic Auth not configured
 *
 * Mounting:
 * - Windows: net use Z: https://server/webdav /user:username password
 * - macOS: mount_webdav https://server/webdav /Volumes/VaultStadio
 * - Linux: sudo mount -t davfs https://server/webdav /mnt/vaultstadio
 *
 * @see docs/PHASE6_ADVANCED_FEATURES.md for full configuration details
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import io.ktor.http.HttpMethod
import org.koin.ktor.ext.get as koinGet
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.options
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext

/**
 * WebDAV HTTP methods not in Ktor by default.
 */
object WebDAVMethods {
    val PROPFIND = HttpMethod("PROPFIND")
    val PROPPATCH = HttpMethod("PROPPATCH")
    val MKCOL = HttpMethod("MKCOL")
    val COPY = HttpMethod("COPY")
    val MOVE = HttpMethod("MOVE")
    val LOCK = HttpMethod("LOCK")
    val UNLOCK = HttpMethod("UNLOCK")
}

/**
 * WebDAV property names.
 */
object WebDAVProperties {
    const val DISPLAY_NAME = "displayname"
    const val RESOURCE_TYPE = "resourcetype"
    const val CONTENT_TYPE = "getcontenttype"
    const val CONTENT_LENGTH = "getcontentlength"
    const val CREATION_DATE = "creationdate"
    const val LAST_MODIFIED = "getlastmodified"
    const val ETAG = "getetag"
    const val LOCK_DISCOVERY = "lockdiscovery"
    const val SUPPORTED_LOCK = "supportedlock"
}

/**
 * Get the authenticated user ID from session principal (User) or Basic Auth fallback.
 */
private fun ApplicationCall.getWebDAVUserId(): String? =
    user?.id ?: principal<UserIdPrincipal>()?.name

/**
 * Configure WebDAV routes.
 * Resolves WebDAVOperations via Koin; no service injection in Routing.
 */
fun Route.webDAVRoutes() {
    authenticate("webdav-basic", "jwt", optional = true) {
        route("/webdav/{path...}") {
            options {
                call.application.koinGet<WebDAVOperations>().handleOptions(call)
            }

            get {
                val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                val userId = call.getWebDAVUserId() ?: "anonymous"
                call.application.koinGet<WebDAVOperations>().handleGet(call, path, userId)
            }

            put {
                val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                val userId = call.getWebDAVUserId() ?: "anonymous"
                call.application.koinGet<WebDAVOperations>().handlePut(call, path, userId)
            }

            delete {
                val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                val userId = call.getWebDAVUserId() ?: "anonymous"
                call.application.koinGet<WebDAVOperations>().handleDelete(call, path, userId)
            }

            head {
                val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                val userId = call.getWebDAVUserId() ?: "anonymous"
                call.application.koinGet<WebDAVOperations>().handleHead(call, path, userId)
            }

            route("", WebDAVMethods.PROPFIND) {
                handle {
                    val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                    val userId = call.getWebDAVUserId() ?: "anonymous"
                    call.application.koinGet<WebDAVOperations>().handlePropfind(call, path, userId)
                }
            }

            route("", WebDAVMethods.PROPPATCH) {
                handle {
                    val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                    val userId = call.getWebDAVUserId() ?: "anonymous"
                    call.application.koinGet<WebDAVOperations>().handleProppatch(call, path, userId)
                }
            }

            route("", WebDAVMethods.MKCOL) {
                handle {
                    val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                    val userId = call.getWebDAVUserId() ?: "anonymous"
                    call.application.koinGet<WebDAVOperations>().handleMkcol(call, path, userId)
                }
            }

            route("", WebDAVMethods.COPY) {
                handle {
                    val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                    val userId = call.getWebDAVUserId() ?: "anonymous"
                    call.application.koinGet<WebDAVOperations>().handleCopy(call, path, userId)
                }
            }

            route("", WebDAVMethods.MOVE) {
                handle {
                    val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                    val userId = call.getWebDAVUserId() ?: "anonymous"
                    call.application.koinGet<WebDAVOperations>().handleMove(call, path, userId)
                }
            }

            route("", WebDAVMethods.LOCK) {
                handle {
                    val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                    val userId = call.getWebDAVUserId() ?: "anonymous"
                    call.application.koinGet<WebDAVOperations>().handleLock(call, path, userId)
                }
            }

            route("", WebDAVMethods.UNLOCK) {
                handle {
                    val path = "/" + (call.parameters.getAll("path")?.joinToString("/") ?: "")
                    call.application.koinGet<WebDAVOperations>().handleUnlock(call, path)
                }
            }
        }
    }
}

/**
 * Extension to add handle block for custom method routes.
 */
private suspend fun PipelineContext<Unit, ApplicationCall>.handle(
    block: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit,
) {
    block()
}
