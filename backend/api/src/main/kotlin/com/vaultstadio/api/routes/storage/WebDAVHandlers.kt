/**
 * WebDAV request handlers and path/XML helpers.
 * Extracted from WebDAVRoutes to keep the main file under the line limit.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.core.domain.service.CopyItemInput
import com.vaultstadio.core.domain.service.CreateFolderInput
import com.vaultstadio.core.domain.service.LockManager
import com.vaultstadio.core.domain.service.MoveItemInput
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UploadFileInput
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.util.UUID

internal suspend fun handleOptions(call: ApplicationCall) {
    call.response.header(
        "Allow",
        "OPTIONS, GET, HEAD, PUT, DELETE, PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK, UNLOCK",
    )
    call.response.header("DAV", "1, 2")
    call.response.header("MS-Author-Via", "DAV")
    call.respond(HttpStatusCode.OK)
}

internal suspend fun handleGet(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
) {
    val item = findItemByWebDAVPath(storageService, userId, path)
    if (item == null) {
        call.respond(HttpStatusCode.NotFound, "File not found")
        return
    }
    if (item.type.name == "FOLDER") {
        call.respond(HttpStatusCode.MethodNotAllowed, "Cannot GET a folder")
        return
    }
    storageService.downloadFile(item.id, userId).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Failed to retrieve file") },
        { (storageItem, inputStream) ->
            val content = inputStream.readBytes()
            call.response.header("Content-Type", storageItem.mimeType ?: "application/octet-stream")
            call.response.header("Content-Length", content.size.toString())
            call.response.header("ETag", "\"${storageItem.checksum ?: UUID.randomUUID()}\"")
            call.response.header("Last-Modified", formatWebDAVDate(storageItem.updatedAt))
            call.respondBytes(content)
        },
    )
}

internal suspend fun handlePut(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
    lockManager: LockManager,
) {
    val lockToken = call.request.headers["If"]
    val existingLock = lockManager.getLock(path)
    if (existingLock != null && (lockToken == null || !lockToken.contains(existingLock.token))) {
        call.respond(HttpStatusCode.Locked, "Resource is locked")
        return
    }
    val content = call.receive<ByteArray>()
    val contentType = call.request.headers["Content-Type"] ?: "application/octet-stream"
    val pathParts = path.split("/").filter { it.isNotEmpty() }
    val fileName = pathParts.lastOrNull() ?: "unnamed"
    val parentId = ensureWebDAVParentFolders(storageService, userId, path)
    val existingItem = findItemByWebDAVPath(storageService, userId, path)
    if (existingItem != null) {
        storageService.deleteItem(existingItem.id, userId)
    }
    val input = UploadFileInput(
        name = fileName,
        parentId = parentId,
        ownerId = userId,
        mimeType = contentType,
        size = content.size.toLong(),
        inputStream = ByteArrayInputStream(content),
    )
    storageService.uploadFile(input).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Failed to create file") },
        { call.respond(if (existingItem != null) HttpStatusCode.NoContent else HttpStatusCode.Created) },
    )
}

internal suspend fun handleDelete(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
    lockManager: LockManager,
) {
    if (lockManager.isLocked(path)) {
        call.respond(HttpStatusCode.Locked, "Resource is locked")
        return
    }
    val item = findItemByWebDAVPath(storageService, userId, path)
    if (item == null) {
        call.respond(HttpStatusCode.NotFound, "Resource not found")
        return
    }
    storageService.deleteItem(item.id, userId).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Failed to delete") },
        { call.respond(HttpStatusCode.NoContent) },
    )
}

internal suspend fun handleHead(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
) {
    val item = findItemByWebDAVPath(storageService, userId, path)
    if (item == null) {
        call.respond(HttpStatusCode.NotFound)
        return
    }
    call.response.header("Content-Type", item.mimeType ?: "application/octet-stream")
    call.response.header("Content-Length", item.size.toString())
    call.response.header("ETag", "\"${item.checksum ?: UUID.randomUUID()}\"")
    call.response.header("Last-Modified", formatWebDAVDate(item.updatedAt))
    call.respond(HttpStatusCode.OK)
}

internal suspend fun handlePropfind(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
    lockManager: LockManager,
) {
    val depth = call.request.headers["Depth"] ?: "1"
    val item = findItemByWebDAVPath(storageService, userId, path)
    val response = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<D:multistatus xmlns:D="DAV:">""")
        if (item != null) {
            append(buildWebDAVResponse(path, item, lockManager))
            if (depth != "0" && item.type.name == "FOLDER") {
                val query = StorageItemQuery(ownerId = userId, parentId = item.id, limit = 1000)
                storageService.listFolder(item.id, userId, query).fold(
                    { },
                    { result ->
                        for (child in result.items) {
                            val childPath = "$path/${child.name}".replace("//", "/")
                            append(buildWebDAVResponse(childPath, child, lockManager))
                        }
                    },
                )
            }
        } else {
            val query = StorageItemQuery(ownerId = userId, parentId = null, limit = 1000)
            storageService.listFolder(null, userId, query).fold(
                { },
                { result ->
                    appendLine("""  <D:response>""")
                    appendLine("""    <D:href>/webdav/</D:href>""")
                    appendLine("""    <D:propstat>""")
                    appendLine("""      <D:prop>""")
                    appendLine("""        <D:displayname>root</D:displayname>""")
                    appendLine("""        <D:resourcetype><D:collection/></D:resourcetype>""")
                    appendLine("""        <D:creationdate>${Clock.System.now()}</D:creationdate>""")
                    appendLine("""        <D:getlastmodified>${Clock.System.now()}</D:getlastmodified>""")
                    appendLine("""      </D:prop>""")
                    appendLine("""      <D:status>HTTP/1.1 200 OK</D:status>""")
                    appendLine("""    </D:propstat>""")
                    appendLine("""  </D:response>""")
                    if (depth != "0") {
                        for (childItem in result.items) {
                            append(buildWebDAVResponse("/${childItem.name}", childItem, lockManager))
                        }
                    }
                },
            )
        }
        appendLine("""</D:multistatus>""")
    }
    call.respondText(response, ContentType("application", "xml"), HttpStatusCode.MultiStatus)
}

internal suspend fun handleProppatch(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
) {
    val item = findItemByWebDAVPath(storageService, userId, path)
    if (item == null) {
        call.respond(HttpStatusCode.NotFound, "Resource not found")
        return
    }
    val response = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<D:multistatus xmlns:D="DAV:">""")
        appendLine("""  <D:response>""")
        appendLine("""    <D:href>/webdav$path</D:href>""")
        appendLine("""    <D:propstat>""")
        appendLine("""      <D:prop></D:prop>""")
        appendLine("""      <D:status>HTTP/1.1 200 OK</D:status>""")
        appendLine("""    </D:propstat>""")
        appendLine("""  </D:response>""")
        appendLine("""</D:multistatus>""")
    }
    call.respondText(response, ContentType("application", "xml"), HttpStatusCode.MultiStatus)
}

internal suspend fun handleMkcol(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
) {
    val pathParts = path.split("/").filter { it.isNotEmpty() }
    val folderName = pathParts.lastOrNull()
    if (folderName == null) {
        call.respond(HttpStatusCode.BadRequest, "Invalid path")
        return
    }
    val parentPath = pathParts.dropLast(1)
    val parentId = if (parentPath.isEmpty()) {
        null
    } else {
        findItemByWebDAVPath(storageService, userId, "/" + parentPath.joinToString("/"))?.id
    }
    val existing = findItemByWebDAVPath(storageService, userId, path)
    if (existing != null) {
        call.respond(HttpStatusCode.MethodNotAllowed, "Collection already exists")
        return
    }
    val input = CreateFolderInput(name = folderName, parentId = parentId, ownerId = userId)
    storageService.createFolder(input).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Failed to create folder") },
        { call.respond(HttpStatusCode.Created) },
    )
}

internal suspend fun handleCopy(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
) {
    val destination = call.request.headers["Destination"]
    if (destination == null) {
        call.respond(HttpStatusCode.BadRequest, "Missing Destination header")
        return
    }
    val overwrite = call.request.headers["Overwrite"] != "F"
    val sourceItem = findItemByWebDAVPath(storageService, userId, path)
    if (sourceItem == null) {
        call.respond(HttpStatusCode.NotFound, "Source not found")
        return
    }
    val destPath = extractPathFromDestination(destination)
    val destParentPath = destPath.split("/").filter { it.isNotEmpty() }.dropLast(1).joinToString("/")
    val destParentId = if (destParentPath.isEmpty()) {
        null
    } else {
        findItemByWebDAVPath(storageService, userId, "/$destParentPath")?.id
    }
    val existingDest = findItemByWebDAVPath(storageService, userId, destPath)
    if (existingDest != null && !overwrite) {
        call.respond(HttpStatusCode.PreconditionFailed, "Destination exists and Overwrite is F")
        return
    }
    val input = CopyItemInput(
        itemId = sourceItem.id,
        destinationParentId = destParentId,
        userId = userId,
    )
    storageService.copyItem(input).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Failed to copy") },
        { call.respond(if (existingDest != null) HttpStatusCode.NoContent else HttpStatusCode.Created) },
    )
}

internal suspend fun handleMove(
    call: ApplicationCall,
    path: String,
    storageService: StorageService,
    userId: String,
    lockManager: LockManager,
) {
    val destination = call.request.headers["Destination"]
    if (destination == null) {
        call.respond(HttpStatusCode.BadRequest, "Missing Destination header")
        return
    }
    val overwrite = call.request.headers["Overwrite"] != "F"
    if (lockManager.isLocked(path)) {
        call.respond(HttpStatusCode.Locked, "Resource is locked")
        return
    }
    val sourceItem = findItemByWebDAVPath(storageService, userId, path)
    if (sourceItem == null) {
        call.respond(HttpStatusCode.NotFound, "Source not found")
        return
    }
    val destPath = extractPathFromDestination(destination)
    val destPathParts = destPath.split("/").filter { it.isNotEmpty() }
    val destName = destPathParts.lastOrNull() ?: sourceItem.name
    val destParentPath = destPathParts.dropLast(1).joinToString("/")
    val destParentId = if (destParentPath.isEmpty()) {
        null
    } else {
        findItemByWebDAVPath(storageService, userId, "/$destParentPath")?.id
    }
    val existingDest = findItemByWebDAVPath(storageService, userId, destPath)
    if (existingDest != null && !overwrite) {
        call.respond(HttpStatusCode.PreconditionFailed, "Destination exists and Overwrite is F")
        return
    }
    if (existingDest != null) {
        storageService.deleteItem(existingDest.id, userId)
    }
    val input = MoveItemInput(
        itemId = sourceItem.id,
        newParentId = destParentId,
        newName = if (destName != sourceItem.name) destName else null,
        userId = userId,
    )
    storageService.moveItem(input).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Failed to move") },
        { call.respond(if (existingDest != null) HttpStatusCode.NoContent else HttpStatusCode.Created) },
    )
}

internal suspend fun handleLock(
    call: ApplicationCall,
    path: String,
    userId: String,
    lockManager: LockManager,
) {
    val depth = call.request.headers["Depth"] ?: "0"
    val timeout = call.request.headers["Timeout"]?.let { parseTimeout(it) } ?: 3600L
    val body = call.receiveText()
    val exclusive = body.contains("exclusive") || !body.contains("shared")
    val lock = lockManager.lock(path, userId, depth, timeout, exclusive)
    if (lock == null) {
        call.respond(HttpStatusCode.Locked, "Resource is already locked")
        return
    }
    val response = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<D:prop xmlns:D="DAV:">""")
        appendLine("""  <D:lockdiscovery>""")
        appendLine("""    <D:activelock>""")
        appendLine("""      <D:locktype><D:write/></D:locktype>""")
        appendLine("""      <D:lockscope>${if (lock.exclusive) "<D:exclusive/>" else "<D:shared/>"}</D:lockscope>""")
        appendLine("""      <D:depth>$depth</D:depth>""")
        appendLine("""      <D:owner><D:href>$userId</D:href></D:owner>""")
        appendLine("""      <D:timeout>Second-$timeout</D:timeout>""")
        appendLine("""      <D:locktoken><D:href>${lock.token}</D:href></D:locktoken>""")
        appendLine("""    </D:activelock>""")
        appendLine("""  </D:lockdiscovery>""")
        appendLine("""</D:prop>""")
    }
    call.response.header("Lock-Token", "<${lock.token}>")
    call.respondText(response, ContentType("application", "xml"), HttpStatusCode.OK)
}

internal suspend fun handleUnlock(call: ApplicationCall, path: String, lockManager: LockManager) {
    val lockToken = call.request.headers["Lock-Token"]?.removeSurrounding("<", ">")
    if (lockToken == null) {
        call.respond(HttpStatusCode.BadRequest, "Missing Lock-Token header")
        return
    }
    if (lockManager.unlock(path, lockToken)) {
        call.respond(HttpStatusCode.NoContent)
    } else {
        call.respond(HttpStatusCode.Conflict, "Lock token does not match")
    }
}

internal suspend fun findItemByWebDAVPath(
    storageService: StorageService,
    userId: String,
    path: String,
): StorageItem? {
    val pathParts = path.split("/").filter { it.isNotEmpty() }
    if (pathParts.isEmpty()) return null
    var currentParentId: String? = null
    var currentItem: StorageItem? = null
    for (part in pathParts) {
        val query = StorageItemQuery(ownerId = userId, parentId = currentParentId, limit = 1000)
        val result = storageService.listFolder(currentParentId, userId, query).getOrNull() ?: return null
        currentItem = result.items.find { it.name == part } ?: return null
        currentParentId = currentItem.id
    }
    return currentItem
}

internal suspend fun ensureWebDAVParentFolders(
    storageService: StorageService,
    userId: String,
    path: String,
): String? {
    val pathParts = path.split("/").filter { it.isNotEmpty() }
    if (pathParts.size <= 1) return null
    val folderParts = pathParts.dropLast(1)
    var currentParentId: String? = null
    for (folderName in folderParts) {
        val query = StorageItemQuery(ownerId = userId, parentId = currentParentId, limit = 1000)
        val result = storageService.listFolder(currentParentId, userId, query).getOrNull()
        val existingFolder = result?.items?.find { it.name == folderName && it.type.name == "FOLDER" }
        if (existingFolder != null) {
            currentParentId = existingFolder.id
        } else {
            val input = CreateFolderInput(name = folderName, parentId = currentParentId, ownerId = userId)
            val newFolder = storageService.createFolder(input).getOrNull()
            currentParentId = newFolder?.id
        }
    }
    return currentParentId
}

internal suspend fun buildWebDAVResponse(path: String, item: StorageItem, lockManager: LockManager): String {
    return buildString {
        appendLine("""  <D:response>""")
        appendLine("""    <D:href>/webdav${escapeWebDAVPath(path)}</D:href>""")
        appendLine("""    <D:propstat>""")
        appendLine("""      <D:prop>""")
        appendLine("""        <D:displayname>${escapeXmlContent(item.name)}</D:displayname>""")
        if (item.type.name == "FOLDER") {
            appendLine("""        <D:resourcetype><D:collection/></D:resourcetype>""")
        } else {
            appendLine("""        <D:resourcetype/>""")
            appendLine(
                """        <D:getcontenttype>${item.mimeType ?: "application/octet-stream"}</D:getcontenttype>""",
            )
            appendLine("""        <D:getcontentlength>${item.size}</D:getcontentlength>""")
        }
        appendLine("""        <D:creationdate>${item.createdAt}</D:creationdate>""")
        appendLine("""        <D:getlastmodified>${formatWebDAVDate(item.updatedAt)}</D:getlastmodified>""")
        appendLine("""        <D:getetag>"${item.checksum ?: UUID.randomUUID()}"</D:getetag>""")
        val lock = lockManager.getLock(path)
        if (lock != null) {
            appendLine("""        <D:lockdiscovery>""")
            appendLine("""          <D:activelock>""")
            appendLine("""            <D:locktype><D:write/></D:locktype>""")
            appendLine(
                """            <D:lockscope>${if (lock.exclusive) "<D:exclusive/>" else "<D:shared/>"}</D:lockscope>""",
            )
            appendLine("""            <D:depth>${lock.depth}</D:depth>""")
            appendLine("""            <D:owner><D:href>${lock.owner}</D:href></D:owner>""")
            appendLine("""            <D:timeout>Second-${lock.timeoutSeconds}</D:timeout>""")
            appendLine("""            <D:locktoken><D:href>${lock.token}</D:href></D:locktoken>""")
            appendLine("""          </D:activelock>""")
            appendLine("""        </D:lockdiscovery>""")
        } else {
            appendLine("""        <D:lockdiscovery/>""")
        }
        appendLine("""        <D:supportedlock>""")
        appendLine(
            """          <D:lockentry><D:lockscope><D:exclusive/></D:lockscope><D:locktype><D:write/></D:locktype></D:lockentry>""",
        )
        appendLine(
            """          <D:lockentry><D:lockscope><D:shared/></D:lockscope><D:locktype><D:write/></D:locktype></D:lockentry>""",
        )
        appendLine("""        </D:supportedlock>""")
        appendLine("""      </D:prop>""")
        appendLine("""      <D:status>HTTP/1.1 200 OK</D:status>""")
        appendLine("""    </D:propstat>""")
        appendLine("""  </D:response>""")
    }
}

internal fun extractPathFromDestination(destination: String): String = when {
    destination.contains("/webdav/") -> destination.substringAfter("/webdav")
    destination.startsWith("http") -> {
        val path = destination.substringAfter("://").substringAfter("/")
        if (path.startsWith("webdav")) path.removePrefix("webdav") else "/$path"
    }
    else -> destination
}

internal fun formatWebDAVDate(instant: Instant): String = instant.toString()

internal fun escapeXmlContent(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

internal fun escapeWebDAVPath(path: String): String =
    path.split("/").joinToString("/") { segment ->
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
    }

internal fun parseTimeout(timeout: String): Long = when {
    timeout == "Infinite" -> Long.MAX_VALUE
    timeout.startsWith("Second-") -> timeout.removePrefix("Second-").toLongOrNull() ?: 3600L
    else -> 3600L
}
