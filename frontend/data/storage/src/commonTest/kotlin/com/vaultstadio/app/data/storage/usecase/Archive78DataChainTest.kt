package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.data.storage.api.StorageApi
import com.vaultstadio.app.data.storage.api.withStorage119Client
import com.vaultstadio.app.data.storage.service.StorageService
import com.vaultstadio.app.domain.result.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** Actual application data chain: use case -> service -> streaming API -> configured client. */
class Archive78DataChainTest {
    @Test fun transfer78ArchiveUseCasePreservesSelectionAndBinaryThroughProductionDataChain() = runTest {
        val bytes = byteArrayOf(80, 75, 3, 4, 0, -1)
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/storage/batch/download-zip", request.url.encodedPath)
            assertEquals("Bearer ephemeral119", request.headers[HttpHeaders.Authorization])
            assertEquals(
                Json.parseToJsonElement("{\"itemIds\":[\"second\",\"first\"]}"),
                Json.parseToJsonElement(request.body.toByteArray().decodeToString()),
            )
            respond(bytes, headers = headersOf(HttpHeaders.ContentType, "application/zip"))
        }
        withStorage119Client(engine) { client ->
            val useCase = DownloadArchiveUseCaseImpl(StorageService(StorageApi(client)))
            assertContentEquals(bytes, assertIs<Result.Success<ByteArray>>(useCase(listOf("second", "first"))).data)
        }
    }

    @Test fun transfer78ArchiveOwnerDenialRemainsDomainErrorAndNeverContainsErrorBodyBytes() = runTest {
        val engine = MockEngine { respond("private error body", HttpStatusCode.Forbidden) }
        withStorage119Client(engine) { client ->
            val useCase = DownloadArchiveUseCaseImpl(StorageService(StorageApi(client)))
            assertEquals("HTTP_403", assertIs<Result.Error>(useCase(listOf("foreign"))).code)
        }
    }

    @Test fun transfer78ArchiveConnectionFailureRemainsDomainNetworkFailure() = runTest {
        val engine = MockEngine { throw IllegalStateException("owned connection failure") }
        withStorage119Client(engine) { client ->
            val useCase = DownloadArchiveUseCaseImpl(StorageService(StorageApi(client)))
            assertIs<Result.NetworkError>(useCase(listOf("owned")))
        }
    }

    @Test fun transfer78ArchiveCallerCancellationPropagatesAcrossEveryDataBoundary() = runTest {
        val engine = MockEngine { throw CancellationException("owned archive canceled") }
        withStorage119Client(engine) { client ->
            val useCase = DownloadArchiveUseCaseImpl(StorageService(StorageApi(client)))
            assertFailsWith<CancellationException> { useCase(listOf("owned")) }
        }
    }
}
