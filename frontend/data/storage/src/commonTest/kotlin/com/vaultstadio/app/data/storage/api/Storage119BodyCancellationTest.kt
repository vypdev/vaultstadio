package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class Storage119BodyCancellationTest {
    private suspend fun probe(operation: suspend (StorageApi) -> ApiResult<*>) {
        val channel = ByteReadChannel(byteArrayOf())
        channel.cancel(CancellationException("owned incoming body cancellation"))
        val engine =
            MockEngine { respond(channel, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        withStorage119Client(engine) { client ->
            assertFailsWith<CancellationException> { operation(StorageApi(client)) }
        }
    }

    @Test fun sto119DownloadIncomingBodyCancellationRemainsControlFlow() = runTest {
        probe { it.downloadFile("file119") }
    }

    @Test fun sto119UploadIncomingBodyCancellationRemainsControlFlow() = runTest {
        probe { it.uploadFile("x", byteArrayOf(1), "text/plain") }
    }

    @Test fun sto119FolderIncomingBodyCancellationRemainsControlFlow() = runTest {
        probe { it.uploadFolder(emptyList(), null, { null }) }
    }

    @Test fun sto119ChunkIncomingBodyCancellationRemainsControlFlow() = runTest {
        probe { it.uploadChunk("upload119", 0, byteArrayOf(1)) }
    }

    @Test fun sto119GenuineRequestDeadlineRemainsANetworkFailure() = runTest {
        val engine = MockEngine { awaitCancellation() }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid", 500),
            TokenProvider {
                null
            },
            engine,
        )
        try {
            withContext(Dispatchers.Default) {
                withTimeout(10_000) { assertIs<ApiResult.NetworkError>(StorageApi(client).downloadFile("file119")) }
            }
        } finally {
            client.close()
            engine.close()
        }
    }
}
