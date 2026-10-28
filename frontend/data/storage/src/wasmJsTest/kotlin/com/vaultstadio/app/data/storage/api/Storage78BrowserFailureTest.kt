package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

/** Actual browser engine and failed fetch; no MockEngine, network credentials, or remote host. */
class Storage78BrowserFailureTest {
    @Test fun transfer78ActualBrowserFetchFailureMapsUploadToNetworkError() = runTest(timeout = 15.seconds) {
        withUnreachableClient { client ->
            assertIs<ApiResult.NetworkError>(
                StorageApi(client).uploadFile("unreachable78.bin", byteArrayOf(1), "test/binary"),
            )
        }
    }

    @Test fun transfer78ActualBrowserFetchFailureMapsChunkToNetworkError() = runTest(timeout = 15.seconds) {
        withUnreachableClient { client ->
            assertIs<ApiResult.NetworkError>(StorageApi(client).uploadChunk("session78", 0, byteArrayOf(1)))
        }
    }

    @Test fun transfer78ActualBrowserFetchFailureMapsMetadataToNetworkError() = runTest(timeout = 15.seconds) {
        withUnreachableClient { client ->
            assertIs<ApiResult.NetworkError>(StorageApi(client).getItem("unreachable78"))
        }
    }

    @Test fun transfer78ActualBrowserFetchFailureMapsFileToNetworkError() = runTest(timeout = 15.seconds) {
        withUnreachableClient { client ->
            assertIs<ApiResult.NetworkError>(StorageApi(client).downloadFile("unreachable78"))
        }
    }

    @Test fun transfer78ActualBrowserFetchFailureMapsArchiveToNetworkError() = runTest(timeout = 15.seconds) {
        withUnreachableClient { client ->
            assertIs<ApiResult.NetworkError>(StorageDownloadApi(client).archive(listOf("unreachable78")))
        }
    }

    private suspend fun withUnreachableClient(block: suspend (HttpClient) -> Unit) {
        // Browsers refuse port 1 immediately: deterministic failed fetch without a new listening service.
        val client = HttpClientFactory.create(
            ApiClientConfig("http://127.0.0.1:1", timeout = 1000),
            TokenProvider {
                null
            },
        )
        try {
            block(client)
        } finally {
            client.close()
        }
    }
}
