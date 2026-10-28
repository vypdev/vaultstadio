package com.vaultstadio.core.domain.service

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.ArchiveEntry
import java.io.InputStream

/** The adapter owns encoding and closing every content stream, including failed/canceled work. */
interface ArchiveWriter {
    suspend fun write(
        entries: List<ArchiveEntry>,
        open: suspend (ArchiveEntry) -> Either<StorageException, InputStream>,
    ): Either<StorageException, ByteArray>
}
