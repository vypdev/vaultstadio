/**
 * S3 request handlers and XML/path helpers.
 * Extracted from S3Routes to keep the main file under the line limit.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.core.domain.service.CopyItemInput
import com.vaultstadio.core.domain.service.CreateFolderInput
import com.vaultstadio.core.domain.service.MultipartUploadManagerInterface
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UploadFileInput
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.SortField
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import kotlinx.datetime.Instant
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.UUID

internal fun calculateMD5(data: ByteArray): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(data)
    return digest.joinToString("") { "%02x".format(it) }
}

internal suspend fun handleListBuckets(
    call: ApplicationCall,
    storageService: StorageService,
    userId: String,
) {
    val query = StorageItemQuery(
        ownerId = userId,
        parentId = null,
        limit = 1000,
    )
    storageService.listFolder(null, userId, query).fold(
        { error ->
            val errorXml = buildS3Error(
                S3ErrorCodes.INTERNAL_ERROR,
                error.message ?: "Failed to list buckets",
            )
            call.respondText(
                errorXml,
                ContentType("application", "xml"),
                HttpStatusCode.InternalServerError,
            )
        },
        { result ->
            val response = buildListBucketsResponse(userId, result.items)
            call.respondText(response, ContentType("application", "xml"))
        },
    )
}

internal suspend fun handleListObjects(
    call: ApplicationCall,
    storageService: StorageService,
    userId: String,
    bucket: String,
) {
    val prefix = call.request.queryParameters["prefix"] ?: ""
    val delimiter = call.request.queryParameters["delimiter"] ?: "/"
    val maxKeys = call.request.queryParameters["max-keys"]?.toIntOrNull() ?: 1000
    val query = StorageItemQuery(
        ownerId = userId,
        parentId = null,
        searchQuery = if (prefix.isNotEmpty()) prefix else null,
        limit = maxKeys,
        sortField = SortField.NAME,
        sortOrder = SortOrder.ASC,
    )
    storageService.listFolder(null, userId, query).fold(
        { error ->
            val errorXml = buildS3Error(
                S3ErrorCodes.INTERNAL_ERROR,
                error.message ?: "Failed to list objects",
            )
            call.respondText(
                errorXml,
                ContentType("application", "xml"),
                HttpStatusCode.InternalServerError,
            )
        },
        { result ->
            val response = buildListObjectsResponse(
                bucket = bucket,
                prefix = prefix,
                delimiter = delimiter,
                maxKeys = maxKeys,
                items = result.items,
                isTruncated = result.hasMore,
            )
            call.respondText(response, ContentType("application", "xml"))
        },
    )
}

internal suspend fun handleHeadBucket(call: ApplicationCall, userId: String?) {
    if (userId == null) {
        call.respond(HttpStatusCode.Forbidden)
        return
    }
    call.respond(HttpStatusCode.OK)
}

internal suspend fun handleCreateBucket(
    call: ApplicationCall,
    storageService: StorageService,
    bucket: String,
    userId: String?,
) {
    if (userId == null) {
        val errorXml = buildS3Error(S3ErrorCodes.ACCESS_DENIED, "Access denied")
        call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.Forbidden)
        return
    }
    val input = CreateFolderInput(
        name = bucket,
        parentId = null,
        ownerId = userId,
    )
    storageService.createFolder(input).fold(
        { error ->
            val errorXml = buildS3Error(
                S3ErrorCodes.BUCKET_ALREADY_EXISTS,
                error.message ?: "Bucket already exists",
            )
            call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.Conflict)
        },
        { call.respond(HttpStatusCode.OK) },
    )
}

internal suspend fun handleDeleteBucket(call: ApplicationCall, userId: String?) {
    if (userId == null) {
        val errorXml = buildS3Error(S3ErrorCodes.ACCESS_DENIED, "Access denied")
        call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.Forbidden)
        return
    }
    call.respond(HttpStatusCode.NoContent)
}

internal suspend fun handleGetObject(
    call: ApplicationCall,
    storageService: StorageService,
    userId: String,
    bucket: String,
    key: String,
) {
    val item = findItemByPath(storageService, userId, key)
    if (item == null) {
        val error = buildS3Error(
            S3ErrorCodes.NO_SUCH_KEY,
            "The specified key does not exist.",
            "/$bucket/$key",
        )
        call.respondText(error, ContentType("application", "xml"), HttpStatusCode.NotFound)
        return
    }
    storageService.downloadFile(item.id, userId).fold(
        { error ->
            val errorXml = buildS3Error(
                S3ErrorCodes.INTERNAL_ERROR,
                error.message ?: "Failed to retrieve object",
            )
            call.respondText(
                errorXml,
                ContentType("application", "xml"),
                HttpStatusCode.InternalServerError,
            )
        },
        { (storageItem, inputStream) ->
            val content = inputStream.readBytes()
            val rangeHeader = call.request.headers["Range"]
            val responseBytes = handleRangeRequest(content, rangeHeader)
            val statusCode = if (rangeHeader != null) HttpStatusCode.PartialContent else HttpStatusCode.OK
            call.response.header("Content-Type", storageItem.mimeType ?: "application/octet-stream")
            call.response.header("Content-Length", responseBytes.size.toString())
            call.response.header("ETag", "\"${calculateMD5(responseBytes)}\"")
            call.response.header("Last-Modified", formatHttpDate(storageItem.updatedAt))
            call.respondBytes(responseBytes, status = statusCode)
        },
    )
}

internal suspend fun handleHeadObject(
    call: ApplicationCall,
    storageService: StorageService,
    userId: String,
    key: String,
) {
    val item = findItemByPath(storageService, userId, key)
    if (item == null) {
        call.respond(HttpStatusCode.NotFound)
        return
    }
    call.response.header("Content-Type", item.mimeType ?: "application/octet-stream")
    call.response.header("Content-Length", item.size.toString())
    call.response.header("ETag", "\"${item.checksum ?: UUID.randomUUID()}\"")
    call.response.header("Last-Modified", formatHttpDate(item.updatedAt))
    call.respond(HttpStatusCode.OK)
}

internal suspend fun handlePutObject(
    call: ApplicationCall,
    storageService: StorageService,
    multipartUploadManager: MultipartUploadManagerInterface,
    userId: String?,
    bucket: String,
    key: String,
) {
    if (userId == null) {
        val errorXml = buildS3Error(S3ErrorCodes.ACCESS_DENIED, "Access denied")
        call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.Forbidden)
        return
    }
    val uploadId = call.request.queryParameters["uploadId"]
    val partNumber = call.request.queryParameters["partNumber"]?.toIntOrNull()
    if (uploadId != null && partNumber != null) {
        val session = multipartUploadManager.get(uploadId)
        if (session == null) {
            val errorXml = buildS3Error(S3ErrorCodes.NO_SUCH_UPLOAD, "Upload not found")
            call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.NotFound)
            return
        }
        val partData = call.receive<ByteArray>()
        val part = multipartUploadManager.addPart(uploadId, partNumber, partData)
        if (part == null) {
            val errorXml = buildS3Error(S3ErrorCodes.INTERNAL_ERROR, "Failed to upload part")
            call.respondText(
                errorXml,
                ContentType("application", "xml"),
                HttpStatusCode.InternalServerError,
            )
            return
        }
        call.response.header("ETag", part.etag)
        call.respond(HttpStatusCode.OK)
        return
    }
    val copySource = call.request.headers["x-amz-copy-source"]
    if (copySource != null) {
        handleS3Copy(call, storageService, userId, copySource)
        return
    }
    val contentType = call.request.headers["Content-Type"] ?: "application/octet-stream"
    val contentData = call.receive<ByteArray>()
    val etag = "\"${calculateMD5(contentData)}\""
    val parentId = ensureParentFolders(storageService, userId, key)
    val fileName = key.substringAfterLast("/")
    val input = UploadFileInput(
        name = fileName,
        parentId = parentId,
        ownerId = userId,
        mimeType = contentType,
        size = contentData.size.toLong(),
        inputStream = ByteArrayInputStream(contentData),
    )
    storageService.uploadFile(input).fold(
        { error ->
            val errorXml = buildS3Error(
                S3ErrorCodes.INTERNAL_ERROR,
                error.message ?: "Failed to upload object",
            )
            call.respondText(
                errorXml,
                ContentType("application", "xml"),
                HttpStatusCode.InternalServerError,
            )
        },
        {
            call.response.header("ETag", etag)
            call.respond(HttpStatusCode.OK)
        },
    )
}

internal suspend fun handleDeleteObject(
    call: ApplicationCall,
    storageService: StorageService,
    multipartUploadManager: MultipartUploadManagerInterface,
    userId: String?,
    key: String,
) {
    if (userId == null) {
        val errorXml = buildS3Error(S3ErrorCodes.ACCESS_DENIED, "Access denied")
        call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.Forbidden)
        return
    }
    val uploadId = call.request.queryParameters["uploadId"]
    if (uploadId != null) {
        multipartUploadManager.abort(uploadId)
        call.respond(HttpStatusCode.NoContent)
        return
    }
    val item = findItemByPath(storageService, userId, key)
    if (item == null) {
        call.respond(HttpStatusCode.NoContent)
        return
    }
    storageService.deleteItem(item.id, userId).fold(
        { error ->
            val errorXml = buildS3Error(
                S3ErrorCodes.INTERNAL_ERROR,
                error.message ?: "Failed to delete object",
            )
            call.respondText(
                errorXml,
                ContentType("application", "xml"),
                HttpStatusCode.InternalServerError,
            )
        },
        { call.respond(HttpStatusCode.NoContent) },
    )
}

internal suspend fun handleMultipartPost(
    call: ApplicationCall,
    storageService: StorageService,
    multipartUploadManager: MultipartUploadManagerInterface,
    userId: String?,
    bucket: String,
    key: String,
) {
    if (userId == null) {
        val errorXml = buildS3Error(S3ErrorCodes.ACCESS_DENIED, "Access denied")
        call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.Forbidden)
        return
    }
    val uploadsParam = call.request.queryParameters["uploads"]
    val uploadId = call.request.queryParameters["uploadId"]
    when {
        uploadsParam != null -> {
            val session = multipartUploadManager.initiate(bucket, key, userId)
            val response = buildInitiateMultipartUploadResponse(bucket, key, session.uploadId)
            call.respondText(response, ContentType("application", "xml"))
        }
        uploadId != null -> {
            val session = multipartUploadManager.get(uploadId)
            if (session == null) {
                val errorXml = buildS3Error(S3ErrorCodes.NO_SUCH_UPLOAD, "Upload not found")
                call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.NotFound)
                return
            }
            val combinedData = multipartUploadManager.complete(uploadId)
            if (combinedData == null) {
                val errorXml = buildS3Error(S3ErrorCodes.INTERNAL_ERROR, "Failed to complete upload")
                call.respondText(
                    errorXml,
                    ContentType("application", "xml"),
                    HttpStatusCode.InternalServerError,
                )
                return
            }
            val parentId = ensureParentFolders(storageService, userId, key)
            val fileName = key.substringAfterLast("/")
            val etag = "\"${calculateMD5(combinedData)}\""
            val input = UploadFileInput(
                name = fileName,
                parentId = parentId,
                ownerId = userId,
                mimeType = "application/octet-stream",
                size = combinedData.size.toLong(),
                inputStream = ByteArrayInputStream(combinedData),
            )
            storageService.uploadFile(input).fold(
                { error ->
                    val errorXml = buildS3Error(
                        S3ErrorCodes.INTERNAL_ERROR,
                        error.message ?: "Failed to save object",
                    )
                    call.respondText(
                        errorXml,
                        ContentType("application", "xml"),
                        HttpStatusCode.InternalServerError,
                    )
                },
                {
                    val response = buildCompleteMultipartUploadResponse(bucket, key, etag)
                    call.respondText(response, ContentType("application", "xml"))
                },
            )
        }
        else -> call.respond(HttpStatusCode.BadRequest)
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

internal suspend fun handleS3Copy(
    call: ApplicationCall,
    storageService: StorageService,
    userId: String,
    copySource: String,
) {
    val sourcePath = copySource.removePrefix("/")
    val sourcePathWithoutBucket = if (sourcePath.contains("/")) {
        sourcePath.substringAfter("/")
    } else {
        sourcePath
    }
    val sourceItem = findItemByPath(storageService, userId, sourcePathWithoutBucket)
    if (sourceItem == null) {
        val errorXml = buildS3Error(S3ErrorCodes.NO_SUCH_KEY, "Source object not found")
        call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.NotFound)
        return
    }
    val input = CopyItemInput(
        itemId = sourceItem.id,
        destinationParentId = null,
        userId = userId,
    )
    storageService.copyItem(input).fold(
        { error ->
            val errorXml = buildS3Error(
                S3ErrorCodes.INTERNAL_ERROR,
                error.message ?: "Failed to copy object",
            )
            call.respondText(errorXml, ContentType("application", "xml"), HttpStatusCode.InternalServerError)
        },
        { copiedItem ->
            val response = buildString {
                appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
                appendLine("""<CopyObjectResult>""")
                appendLine("""  <LastModified>${copiedItem.updatedAt}</LastModified>""")
                appendLine("""  <ETag>"${copiedItem.checksum ?: UUID.randomUUID()}"</ETag>""")
                appendLine("""</CopyObjectResult>""")
            }
            call.respondText(response, ContentType("application", "xml"))
        },
    )
}

internal suspend fun findItemByPath(
    storageService: StorageService,
    userId: String,
    path: String,
): StorageItem? {
    val pathParts = path.split("/").filter { it.isNotEmpty() }
    if (pathParts.isEmpty()) return null
    var currentParentId: String? = null
    var currentItem: StorageItem? = null
    for (part in pathParts) {
        val query = StorageItemQuery(
            ownerId = userId,
            parentId = currentParentId,
            limit = 1000,
        )
        val result = storageService.listFolder(currentParentId, userId, query).getOrNull() ?: return null
        currentItem = result.items.find { it.name == part } ?: return null
        currentParentId = currentItem.id
    }
    return currentItem
}

internal suspend fun ensureParentFolders(
    storageService: StorageService,
    userId: String,
    path: String,
): String? {
    val pathParts = path.split("/").filter { it.isNotEmpty() }
    if (pathParts.size <= 1) return null
    val folderParts = pathParts.dropLast(1)
    var currentParentId: String? = null
    for (folderName in folderParts) {
        val query = StorageItemQuery(
            ownerId = userId,
            parentId = currentParentId,
            limit = 1000,
        )
        val result = storageService.listFolder(currentParentId, userId, query).getOrNull()
        val existingFolder = result?.items?.find { it.name == folderName && it.type.name == "FOLDER" }
        if (existingFolder != null) {
            currentParentId = existingFolder.id
        } else {
            val input = CreateFolderInput(
                name = folderName,
                parentId = currentParentId,
                ownerId = userId,
            )
            val newFolder = storageService.createFolder(input).getOrNull()
            currentParentId = newFolder?.id
        }
    }
    return currentParentId
}

internal fun handleRangeRequest(content: ByteArray, rangeHeader: String?): ByteArray {
    if (rangeHeader == null) return content
    val rangeMatch = Regex("""bytes=(\d+)-(\d*)""").find(rangeHeader) ?: return content
    val start = rangeMatch.groupValues[1].toIntOrNull() ?: 0
    val end = rangeMatch.groupValues[2].toIntOrNull() ?: (content.size - 1)
    return content.sliceArray(start..minOf(end, content.size - 1))
}

internal fun formatHttpDate(instant: Instant): String = instant.toString()

internal fun escapeXml(text: String): String =
    text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private fun buildListBucketsResponse(userId: String, items: List<StorageItem>): String {
    return buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<ListAllMyBucketsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">""")
        appendLine("""  <Owner>""")
        appendLine("""    <ID>$userId</ID>""")
        appendLine("""    <DisplayName>$userId</DisplayName>""")
        appendLine("""  </Owner>""")
        appendLine("""  <Buckets>""")
        for (item in items.filter { it.type.name == "FOLDER" }) {
            appendLine("""    <Bucket>""")
            appendLine("""      <Name>${escapeXml(item.name)}</Name>""")
            appendLine("""      <CreationDate>${item.createdAt}</CreationDate>""")
            appendLine("""    </Bucket>""")
        }
        appendLine("""  </Buckets>""")
        appendLine("""</ListAllMyBucketsResult>""")
    }
}

private fun buildListObjectsResponse(
    bucket: String,
    prefix: String,
    delimiter: String,
    maxKeys: Int,
    items: List<StorageItem>,
    isTruncated: Boolean,
): String {
    val commonPrefixes = mutableSetOf<String>()
    val objects = mutableListOf<StorageItem>()
    for (item in items) {
        if (item.type.name == "FOLDER" && delimiter.isNotEmpty()) {
            commonPrefixes.add(item.name + delimiter)
        } else {
            objects.add(item)
        }
    }
    return buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">""")
        appendLine("""  <Name>${escapeXml(bucket)}</Name>""")
        appendLine("""  <Prefix>${escapeXml(prefix)}</Prefix>""")
        appendLine("""  <Delimiter>${escapeXml(delimiter)}</Delimiter>""")
        appendLine("""  <MaxKeys>$maxKeys</MaxKeys>""")
        appendLine("""  <IsTruncated>$isTruncated</IsTruncated>""")
        for (obj in objects) {
            appendLine("""  <Contents>""")
            appendLine("""    <Key>${escapeXml(obj.name)}</Key>""")
            appendLine("""    <LastModified>${obj.updatedAt}</LastModified>""")
            appendLine("""    <ETag>"${obj.checksum ?: UUID.randomUUID()}"</ETag>""")
            appendLine("""    <Size>${obj.size}</Size>""")
            appendLine("""    <StorageClass>STANDARD</StorageClass>""")
            appendLine("""  </Contents>""")
        }
        for (cp in commonPrefixes) {
            appendLine("""  <CommonPrefixes>""")
            appendLine("""    <Prefix>${escapeXml(cp)}</Prefix>""")
            appendLine("""  </CommonPrefixes>""")
        }
        appendLine("""</ListBucketResult>""")
    }
}

private fun buildInitiateMultipartUploadResponse(bucket: String, key: String, uploadId: String): String {
    return buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<InitiateMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">""")
        appendLine("""  <Bucket>${escapeXml(bucket)}</Bucket>""")
        appendLine("""  <Key>${escapeXml(key)}</Key>""")
        appendLine("""  <UploadId>$uploadId</UploadId>""")
        appendLine("""</InitiateMultipartUploadResult>""")
    }
}

private fun buildCompleteMultipartUploadResponse(bucket: String, key: String, etag: String): String {
    return buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<CompleteMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">""")
        appendLine("""  <Location>/s3/${escapeXml(bucket)}/${escapeXml(key)}</Location>""")
        appendLine("""  <Bucket>${escapeXml(bucket)}</Bucket>""")
        appendLine("""  <Key>${escapeXml(key)}</Key>""")
        appendLine("""  <ETag>$etag</ETag>""")
        appendLine("""</CompleteMultipartUploadResult>""")
    }
}
