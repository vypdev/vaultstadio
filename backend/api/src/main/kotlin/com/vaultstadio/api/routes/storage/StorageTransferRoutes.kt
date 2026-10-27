package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.storage.DownloadFileUseCase
import com.vaultstadio.application.usecase.storage.UploadFileUseCase
import com.vaultstadio.core.domain.service.UploadFileInput
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.io.ByteArrayInputStream
import org.koin.ktor.ext.get as koinGet

internal fun Route.transfersStorageRoutes() {
    uploadFileRoute()
    downloadFileRoute()
}

private fun Route.uploadFileRoute() {
    // Upload file (parentId from query or from multipart form field)
    post("/upload") {
        val uploadFileUseCase: UploadFileUseCase = call.application.koinGet()
        val user = call.user!!
        val upload = call.readUploadedFile()
        val parentId = upload.parentId
        val fileName = upload.name
        val fileBytes = upload.bytes
        val mimeType = upload.mimeType

        if (fileName == null || fileBytes == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    error = ApiError("INVALID_REQUEST", "No file provided"),
                ),
            )
            return@post
        }

        val input = UploadFileInput(
            name = fileName!!,
            parentId = parentId,
            ownerId = user.id,
            mimeType = mimeType,
            size = fileBytes!!.size.toLong(),
            inputStream = ByteArrayInputStream(fileBytes),
        )

        uploadFileUseCase(input).fold(
            { error -> throw error },
            { file ->
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(success = true, data = file.toResponse()),
                )
            },
        )
    }
}

private fun Route.downloadFileRoute() {
    // Download file
    get("/download/{itemId}") {
        val downloadFileUseCase: DownloadFileUseCase = call.application.koinGet()
        val user = call.user!!
        val itemId = call.parameters["itemId"]!!

        downloadFileUseCase(itemId, user.id).fold(
            { error -> throw error },
            { (item, stream) ->
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        item.name,
                    ).toString(),
                )
                call.respondOutputStream(
                    contentType = item.mimeType?.let { ContentType.parse(it) }
                        ?: ContentType.Application.OctetStream,
                    contentLength = item.size,
                ) {
                    stream.use { it.copyTo(this) }
                }
            },
        )
    }
}

private data class UploadedFileForm(
    val parentId: String?,
    val name: String?,
    val bytes: ByteArray?,
    val mimeType: String?,
)

private suspend fun ApplicationCall.readUploadedFile(): UploadedFileForm {
    var parentId = request.queryParameters["parentId"]

    val multipart = receiveMultipart(formFieldLimit = 512L * 1024 * 1024) // 512 MB max per part
    var fileName: String? = null
    var fileBytes: ByteArray? = null
    var mimeType: String? = null

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                if (part.name == "parentId") {
                    parentId = part.value.takeIf { it.isNotBlank() }
                }
            }
            is PartData.FileItem -> {
                fileName = part.originalFileName
                mimeType = part.contentType?.toString()
                fileBytes = part.streamProvider().readBytes()
            }
            else -> {}
        }
        part.dispose()
    }

    return UploadedFileForm(parentId, fileName, fileBytes, mimeType)
}
