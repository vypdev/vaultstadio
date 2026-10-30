package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.ChunkedUploadInit
import com.vaultstadio.app.domain.storage.model.ChunkedUploadStatus
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.upload.ChunkedFileSource
import com.vaultstadio.app.domain.upload.UploadQueueEntry

internal class Upload78ChunkRepository : Upload78Repository() {
    var initialized: Result<ChunkedUploadInit> = Result.Success(ChunkedUploadInit("session78", 2, 3))
    var finalized: Result<StorageItem> = Result.Success(upload78Item("chunks.bin"))
    var chunk: suspend () -> Result<ChunkedUploadStatus> = {
        Result.Success(ChunkedUploadStatus("session78", "chunks.bin", 5, 2, 0.4f, listOf(0), listOf(1, 2), false))
    }
    val parts = mutableListOf<Pair<Int, ByteArray>>()
    var initCalls = 0
    var completeCalls = 0
    var cancelCalls = 0
    var canceledSession: String? = null

    override suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): Result<ChunkedUploadInit> {
        initCalls++
        return initialized
    }

    override suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): Result<ChunkedUploadStatus> {
        check(uploadId == "session78")
        parts += chunkIndex to chunkData.copyOf()
        return chunk()
    }

    override suspend fun completeChunkedUpload(uploadId: String): Result<StorageItem> {
        check(uploadId == "session78")
        completeCalls++
        return finalized
    }

    override suspend fun cancelChunkedUpload(uploadId: String): Result<Unit> {
        canceledSession = uploadId
        cancelCalls++
        return Result.Success(Unit)
    }
}

internal fun upload78ChunkEntry(bytes: ByteArray = byteArrayOf(0, 1, -1, 3, 4), truncate: Boolean = false) =
    UploadQueueEntry.Chunked(
        "chunks.bin",
        bytes.size.toLong(),
        "test/binary",
        object : ChunkedFileSource {
            override val name = "chunks.bin"
            override val size = bytes.size.toLong()
            override val mimeType = "test/binary"
            override suspend fun readChunk(start: Long, end: Long): ByteArray =
                if (truncate) byteArrayOf() else bytes.copyOfRange(start.toInt(), end.toInt())
        },
    )
