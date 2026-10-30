package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.storage.dto.BatchCopyRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchDeleteRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchMoveRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchStarRequestDTO
import com.vaultstadio.app.data.storage.dto.ChunkedUploadInitRequestDTO
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class Storage119BatchChunkTest {
    @Test fun sto119BatchDeletePreservesAllIdentitiesAndPermanentFlag() = runTest {
        storage119Request(
            HttpMethod.Post,
            "/api/v1/storage/batch/delete",
            STORAGE119_BATCH,
            body = """{"itemIds":["file119","denied119"],"permanent":true}""",
        ) {
            it.batchDelete(BatchDeleteRequestDTO(listOf("file119", "denied119"), true))
        }
    }

    @Test fun sto119BatchMovePreservesNullRoot() = runTest {
        storage119Request(
            HttpMethod.Post,
            "/api/v1/storage/batch/move",
            STORAGE119_BATCH,
            body = """{"itemIds":["file119"],"destinationId":null}""",
        ) {
            it.batchMove(BatchMoveRequestDTO(listOf("file119"), null))
        }
    }

    @Test fun sto119BatchCopyPreservesDestination() = runTest {
        storage119Request(
            HttpMethod.Post,
            "/api/v1/storage/batch/copy",
            STORAGE119_BATCH,
            body = """{"itemIds":["file119"],"destinationId":"dest119"}""",
        ) {
            it.batchCopy(BatchCopyRequestDTO(listOf("file119"), "dest119"))
        }
    }

    @Test fun sto119BatchStarPreservesFalseAndAllIdentities() = runTest {
        storage119Request(
            HttpMethod.Post,
            "/api/v1/storage/batch/star",
            STORAGE119_BATCH,
            body = """{"itemIds":["file119"],"starred":false}""",
        ) {
            it.batchStar(BatchStarRequestDTO(listOf("file119"), false))
        }
    }

    @Test fun sto119EmptyTrashIsAnExplicitCommand() = runTest {
        storage119Request(HttpMethod.Post, "/api/v1/storage/batch/empty-trash", STORAGE119_BATCH) { it.emptyTrash() }
    }

    @Test fun sto119ChunkInitPreservesExactSizesAndParent() = runTest {
        storage119Request(
            HttpMethod.Post,
            "/api/v1/storage/upload/init",
            """{"success":true,"data":{"uploadId":"upload119","chunkSize":2,"totalChunks":2}}""",
            body = """
{
  "fileName":"é.txt",
  "totalSize":4,
  "mimeType":"text/plain",
  "parentId":"parent119",
  "chunkSize":2
}
""",
        ) {
            it.initChunkedUpload(ChunkedUploadInitRequestDTO("é.txt", 4, "text/plain", "parent119", 2))
        }
    }

    @Test fun sto119ChunkStatusReadsActualTransferFields() = runTest {
        storage119Request(HttpMethod.Get, "/api/v1/storage/upload/upload119/status", STORAGE119_CHUNK) {
            it.getUploadStatus("upload119")
        }
    }

    @Test fun sto119CompleteAndCancelHaveDistinctMethodsAndShapes() = runTest {
        storage119Request(HttpMethod.Post, "/api/v1/storage/upload/upload119/complete") {
            it.completeChunkedUpload("upload119")
        }
        storage119Request(HttpMethod.Delete, "/api/v1/storage/upload/upload119", STORAGE119_UNIT) {
            it.cancelChunkedUpload("upload119")
        }
    }
}
