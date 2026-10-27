package com.vaultstadio.api.routes.share

import com.vaultstadio.domain.share.access.PublicShareDownload
import com.vaultstadio.domain.share.access.PublicShareInfo
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.serialization.Serializable
import java.io.FilterInputStream
import java.net.URLEncoder

@Serializable
data class PublicShareResponse(
    val name: String,
    val size: Long,
    val mimeType: String?,
    val hasPassword: Boolean,
    val expiresAt: String?,
)

internal fun PublicShareInfo.toPublicResponse() = PublicShareResponse(
    name,
    size,
    mimeType,
    hasPassword,
    expiresAt?.toString(),
)

internal fun ApplicationCall.publicShareHeaders() {
    response.header(HttpHeaders.CacheControl, "no-store")
    response.header("Referrer-Policy", "no-referrer")
    response.header("X-Content-Type-Options", "nosniff")
}

internal suspend fun ApplicationCall.streamShare(download: PublicShareDownload) {
    val filename = download.information.name.map { if (it.isISOControl() || it == '/' || it == '\\') '_' else it }
        .joinToString("")
    val fallback = filename.map { if (it.code > 126) '_' else it }.joinToString("")
    val encodedName = URLEncoder.encode(filename, Charsets.UTF_8).replace("+", "%20")
    val disposition = ContentDisposition.Attachment
        .withParameter(ContentDisposition.Parameters.FileName, fallback)
        .withParameter("filename*", "utf-8''$encodedName", encodeValue = false)
    response.header(HttpHeaders.ContentDisposition, disposition.toString())
    // The response channel owns the stream throughout deferred Ktor transmission.
    // Ktor cancels/closes ReadChannelContent on completion, failure and disconnect.
    val channel = object : FilterInputStream(download.content.stream) {
        override fun close() = download.content.close()
    }.toByteReadChannel()
    var accepted = false
    try {
        respond(object : OutgoingContent.ReadChannelContent() {
            override val contentType = ContentType.Application.OctetStream
            override val contentLength = download.information.size
            override fun readFrom() = channel
        })
        accepted = true
    } finally {
        if (!accepted) {
            channel.cancel(null)
            download.content.close()
        }
    }
}
