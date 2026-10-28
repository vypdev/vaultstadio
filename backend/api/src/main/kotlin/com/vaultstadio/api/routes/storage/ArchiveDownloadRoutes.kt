package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import com.vaultstadio.application.usecase.storage.DownloadArchiveUseCase
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get as koinGet

internal fun Route.archiveDownloadRoutes() {
    post("/download-zip") {
        val download: DownloadArchiveUseCase = call.application.koinGet()
        val request = call.receive<DownloadZipRequest>()
        download(request.itemIds, call.user!!.id).fold(
            { throw it },
            { bytes ->
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        "VaultStadio.zip",
                    ).toString(),
                )
                call.respondBytes(bytes, ContentType.Application.Zip)
            },
        )
    }
}
