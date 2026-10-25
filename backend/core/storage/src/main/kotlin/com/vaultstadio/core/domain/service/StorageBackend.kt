package com.vaultstadio.core.domain.service

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException
import java.io.InputStream

/**
 * Storage backend interface for actual file operations.
 */
interface StorageBackend {
    suspend fun store(inputStream: InputStream, size: Long, mimeType: String?): Either<StorageException, String>
    suspend fun retrieve(storageKey: String): Either<StorageException, InputStream>
    suspend fun delete(storageKey: String): Either<StorageException, Unit>
    suspend fun exists(storageKey: String): Either<StorageException, Boolean>
    suspend fun getSize(storageKey: String): Either<StorageException, Long>
    suspend fun copy(sourceKey: String): Either<StorageException, String>
    suspend fun getPresignedUrl(storageKey: String, expirationSeconds: Long): Either<StorageException, String?>

    /**
     * Checks if the storage backend is available and operational.
     * Used for health checks.
     */
    suspend fun isAvailable(): Either<StorageException, Boolean>
}
