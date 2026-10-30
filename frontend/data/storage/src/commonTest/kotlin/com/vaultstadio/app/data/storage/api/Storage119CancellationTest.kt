package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiResult
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Storage119CancellationTest {
    private suspend fun cancelled(operation: suspend (StorageApi) -> ApiResult<*>) {
        val engine = MockEngine { throw CancellationException("owned storage cancellation") }
        withStorage119Client(engine) { client ->
            assertFailsWith<CancellationException> { operation(StorageApi(client)) }
        }
    }

    @Test fun sto119DownloadCancellationRemainsControlFlow() = runTest { cancelled { it.downloadFile("file119") } }

    @Test fun sto119UploadCancellationRemainsControlFlow() = runTest {
        cancelled { it.uploadFile("x", byteArrayOf(1), "text/plain") }
    }

    @Test fun sto119FolderCancellationRemainsControlFlow() = runTest {
        cancelled { it.uploadFolder(emptyList(), null, { null }) }
    }

    @Test fun sto119ChunkCancellationRemainsControlFlow() = runTest {
        cancelled { it.uploadChunk("upload119", 0, byteArrayOf(1)) }
    }
}
