package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.storage.model.FolderUploadFile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Storage119TransferTest {
    private val bytes = byteArrayOf(0, 1, 127, -1)
    @Test fun sto119FileMultipartPreservesNameMimeParentAndBinaryBytes() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/storage/upload", request.url.encodedPath)
            assertTrue(request.body.contentType?.match(ContentType.MultiPart.FormData) == true)
            val body = request.body.toByteArray()
            assertTrue(body.asList().windowed(bytes.size).any { it == bytes.asList() })
            val headersAndFields = body.decodeToString()
            assertTrue(headersAndFields.contains("filename=\"é.txt\""))
            assertTrue(headersAndFields.contains("application/octet-stream"))
            assertTrue(headersAndFields.contains("parent119"))
            respond(STORAGE119_ITEM, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withStorage119Client(engine) { client ->
            val result = StorageApi(client).uploadFile("é.txt", bytes, "application/octet-stream", "parent119")
            assertEquals("file119", result.getOrNull()?.id)
        }
    }
    @Test fun sto119FolderMultipartPreservesRelativePathBytesAndTransferProgress() = runTest {
        val progress = mutableListOf<Float>()
        val engine = MockEngine { request ->
            assertEquals("/api/v1/storage/upload-folder", request.url.encodedPath)
            assertTrue(progress.isEmpty(), "Body preparation is not transfer progress")
            val body = request.body.toByteArray()
            assertTrue(body.asList().windowed(bytes.size).any { it == bytes.asList() })
            assertTrue(body.decodeToString().contains("dir/é.txt"))
            assertTrue(body.decodeToString().contains("parent119"))
            respond("""{"success":true,"data":{"uploadedFiles":1,"createdFolders":1,"errors":[]}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withStorage119Client(engine) { client ->
            val result = StorageApi(client).uploadFolder(
                listOf(FolderUploadFile("é.txt", "dir/é.txt", "application/octet-stream", bytes)),
                "parent119", { "ephemeral119" }, progress::add,
            )
            assertEquals(1, result.getOrNull()?.uploadedFiles)
            assertTrue(progress.all { it in 0f..1f })
            assertEquals(1f, progress.lastOrNull())
        }
    }
    @Test fun sto119MissingFolderTokenDoesNotInventBearerNull() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Bearer ephemeral119", request.headers[HttpHeaders.Authorization])
            respond("""{"success":true,"data":{"uploadedFiles":0,"createdFolders":0}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withStorage119Client(engine) { client ->
            assertIs<ApiResult.Success<*>>(StorageApi(client).uploadFolder(emptyList(), null, { null }))
        }
    }
    @Test fun sto119ChunkMultipartPreservesIndexAndOriginalBytes() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/storage/upload/upload119/chunk/3", request.url.encodedPath)
            val body = request.body.toByteArray()
            assertTrue(body.asList().windowed(bytes.size).any { it == bytes.asList() })
            assertTrue(body.decodeToString().contains("chunk_3"))
            respond(STORAGE119_CHUNK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withStorage119Client(engine) { client ->
            val result = StorageApi(client).uploadChunk("upload119", 3, bytes).getOrNull()
            assertEquals("upload119", result?.uploadId); assertEquals(listOf(1), result?.missingChunks)
        }
    }
    @Test fun sto119DownloadRetainsBinaryBytes() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/storage/item/file119/download", request.url.encodedPath)
            respond(bytes, headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"))
        }
        withStorage119Client(engine) { client ->
            assertContentEquals(bytes, StorageApi(client).downloadFile("file119").getOrNull())
        }
    }
    @Test fun sto119SuccessfulEmptyDownloadIsDistinctFromFailure() = runTest {
        val engine = MockEngine { respond(byteArrayOf(), headers = headersOf(HttpHeaders.ContentType, "application/octet-stream")) }
        withStorage119Client(engine) { client ->
            assertContentEquals(byteArrayOf(), StorageApi(client).downloadFile("empty119").getOrNull())
        }
    }
}
