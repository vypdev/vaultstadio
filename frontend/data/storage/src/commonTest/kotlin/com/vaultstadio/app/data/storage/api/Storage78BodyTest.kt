package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.storage.transfer.DownloadLimits
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Storage78BodyTest {
    @Test fun transfer78EmptySuccessfulBodyIsAnEmptyFile() = runTest {
        assertContentEquals(byteArrayOf(), body(byteArrayOf(), 0).getOrNull())
    }

    @Test fun transfer78JsonFileIsSavedAsOriginalBytesRatherThanParsed() = runTest {
        val bytes = "{\"value\":\"á ñ 日本語\"}".encodeToByteArray()
        assertContentEquals(bytes, body(bytes, mime = "application/json").getOrNull())
    }

    @Test fun transfer78BinaryBytesCrossChunkBoundariesWithoutConversion() = runTest {
        val bytes = ByteArray(20001) { (it % 251).toByte() }
        assertContentEquals(bytes, body(bytes, bytes.size.toLong()).getOrNull())
    }

    @Test fun transfer78TruncatedDeclaredBodyIsRejected() = runTest {
        assertEquals("DOWNLOAD_INCOMPLETE", assertIs<ApiResult.Error>(body(byteArrayOf(1), 2)).code)
    }

    @Test fun transfer78MoreBytesThanDeclaredIsRejected() = runTest {
        assertEquals("DOWNLOAD_INCOMPLETE", assertIs<ApiResult.Error>(body(byteArrayOf(1, 2), 1)).code)
    }

    @Test fun transfer78DeclaredOversizeIsRejectedEvenWhenBodyIsEmpty() = runTest {
        assertEquals(
            "FILE_TOO_LARGE",
            assertIs<ApiResult.Error>(body(byteArrayOf(), DownloadLimits.MAX_BYTES.toLong() + 1)).code,
        )
    }

    @Test fun transfer78UndeclaredOversizeBodyIsRejectedWhileReading() = runTest {
        assertEquals(
            "FILE_TOO_LARGE",
            assertIs<ApiResult.Error>(body(ByteArray(DownloadLimits.MAX_BYTES + 1))).code,
        )
    }

    @Test fun transfer78ExactlySupportedMaximumPreservesBytes() = runTest {
        val bytes = ByteArray(DownloadLimits.MAX_BYTES) { (it % 251).toByte() }
        assertContentEquals(bytes, body(bytes, bytes.size.toLong()).getOrNull())
    }

    @Test fun transfer78UnauthorizedResponseNeverBecomesFileBytes() = runTest {
        assertEquals("HTTP_401", assertIs<ApiResult.Error>(body(status = HttpStatusCode.Unauthorized)).code)
    }

    @Test fun transfer78ForbiddenResponseNeverBecomesFileBytes() = runTest {
        assertEquals("HTTP_403", assertIs<ApiResult.Error>(body(status = HttpStatusCode.Forbidden)).code)
    }

    @Test fun transfer78MissingFileResponseNeverBecomesFileBytes() = runTest {
        assertEquals("HTTP_404", assertIs<ApiResult.Error>(body(status = HttpStatusCode.NotFound)).code)
    }

    @Test fun transfer78UnavailableProviderResponseNeverBecomesFileBytes() = runTest {
        assertEquals("HTTP_503", assertIs<ApiResult.Error>(body(status = HttpStatusCode.ServiceUnavailable)).code)
    }

    private suspend fun body(
        bytes: ByteArray = "private error response".encodeToByteArray(),
        length: Long? = null,
        mime: String = "application/octet-stream",
        status: HttpStatusCode = HttpStatusCode.OK,
    ): ApiResult<ByteArray> {
        val headers = Headers.build {
            append(HttpHeaders.ContentType, mime)
            length?.let { append(HttpHeaders.ContentLength, it.toString()) }
        }
        return withStorage119Client(MockEngine { respond(ByteReadChannel(bytes), status, headers) }) { client ->
            StorageApi(client).downloadFile("body78")
        }
    }
}
