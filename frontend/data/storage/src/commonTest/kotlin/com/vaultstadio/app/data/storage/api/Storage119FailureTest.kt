package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class Storage119FailureTest {
    private suspend fun variants(body: String, status: HttpStatusCode = HttpStatusCode.OK) {
        val engine = MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
        withStorage119Client(engine) { client ->
            val api = StorageApi(client)
            for (result in listOf(
                api.uploadFile("x", byteArrayOf(1), "text/plain"),
                api.uploadFolder(emptyList(), null, { null }),
                api.uploadChunk("upload119", 0, byteArrayOf(1)),
            )) { assertIs<ApiResult.Error>(result); assertNull(result.getOrNull()) }
        }
    }
    @Test fun sto119SemanticTransferErrorsCannotBecomeSuccess() = runTest {
        variants("""{"success":false,"error":{"code":"QUOTA","message":"Full"}}""")
    }
    @Test fun sto119MissingTransferPayloadCannotBecomeSuccess() = runTest { variants("""{"success":true}""") }
    @Test fun sto119AbsentErrorDetailsHaveExplicitFallback() = runTest { variants("""{"success":false}""") }
    @Test fun sto119HttpTransferFailureCannotBecomeSuccess() = runTest { variants("private diagnostic", HttpStatusCode.Forbidden) }
    @Test fun sto119FailedDownloadDoesNotReturnAnEmptyFile() = runTest {
        val engine = MockEngine { respond("private diagnostic", HttpStatusCode.NotFound) }
        withStorage119Client(engine) { client ->
            assertEquals(ApiResult.Error("HTTP_404", "Not Found"), StorageApi(client).downloadFile("missing119"))
        }
    }
    @Test fun sto119OfflineAndMalformedTransfersRemainFailures() = runTest {
        val engine = MockEngine { throw IllegalStateException("offline") }
        withStorage119Client(engine) { client ->
            val api = StorageApi(client)
            for (result in listOf(api.downloadFile("x"), api.uploadFile("x", byteArrayOf(), "text/plain"),
                api.uploadFolder(emptyList(), null, { null }), api.uploadChunk("x", 0, byteArrayOf()))) {
                assertEquals(ApiResult.NetworkError("offline"), result)
            }
        }
        val malformed = MockEngine { respond("not json", headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        withStorage119Client(malformed) { client ->
            assertIs<ApiResult.NetworkError>(StorageApi(client).uploadFile("x", byteArrayOf(), "text/plain"))
            assertIs<ApiResult.NetworkError>(StorageApi(client).uploadFolder(emptyList(), null, { null }))
            assertIs<ApiResult.NetworkError>(StorageApi(client).uploadChunk("x", 0, byteArrayOf()))
        }
    }
}
