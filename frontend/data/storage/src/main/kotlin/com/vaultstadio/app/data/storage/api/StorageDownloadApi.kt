package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.DownloadBody
import com.vaultstadio.app.data.network.rethrowTransportCancellation
import com.vaultstadio.app.domain.storage.transfer.DownloadLimits
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable

/** Streaming response scope prevents Ktor's buffered get() from allocating an unbounded body. */
internal class StorageDownloadApi(private val client: HttpClient) {
    suspend fun file(itemId: String): ApiResult<ByteArray> = execute(
        client.prepareGet {
            url {
                encodedPath = "/api/v1/storage/download"
                appendPathSegments(itemId, encodeSlash = true)
            }
        },
    )

    suspend fun archive(itemIds: List<String>): ApiResult<ByteArray> = execute(
        client.preparePost("/api/v1/storage/batch/download-zip") {
            contentType(ContentType.Application.Json)
            setBody(DownloadArchiveRequest(itemIds))
        },
    )

    private suspend fun execute(statement: HttpStatement): ApiResult<ByteArray> = try {
        statement.execute { response ->
            if (!response.status.isSuccess()) {
                ApiResult.Error("HTTP_${response.status.value}", response.status.description)
            } else {
                readBounded(response)
            }
        }
    } catch (error: Exception) {
        rethrowTransportCancellation(error)
        ApiResult.NetworkError(error.message ?: "Download failed")
    }

    private suspend fun readBounded(response: HttpResponse): ApiResult<ByteArray> {
        val declared = response.contentLength()
        if (declared != null && (declared < 0 || declared > DownloadLimits.MAX_BYTES)) return tooLarge()
        val channel = response.body<DownloadBody>().channel
        val chunks = mutableListOf<ByteArray>()
        var size = 0
        try {
            channel.closedCause?.let { throw it }
            val buffer = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = channel.readAvailable(buffer, 0, buffer.size)
                if (read < 0) break
                if (read == 0) {
                    yield()
                    continue
                }
                if (read > DownloadLimits.MAX_BYTES - size) return tooLarge()
                chunks += buffer.copyOf(read)
                size += read
            }
            channel.closedCause?.let { throw it }
            if (declared != null && declared != size.toLong()) {
                return ApiResult.Error("DOWNLOAD_INCOMPLETE", "Download did not complete")
            }
            val bytes = ByteArray(size)
            var offset = 0
            for (chunk in chunks) {
                chunk.copyInto(bytes, offset)
                offset += chunk.size
            }
            return ApiResult.Success(bytes)
        } finally {
            channel.cancel(null)
        }
    }

    private fun tooLarge() = ApiResult.Error("FILE_TOO_LARGE", "Downloads support at most 32 MiB")
}

@Serializable
private data class DownloadArchiveRequest(val itemIds: List<String>)
