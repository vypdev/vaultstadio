package com.vaultstadio.domain.auth.repository

import arrow.core.Either
import com.vaultstadio.domain.auth.model.ApiKey
import com.vaultstadio.domain.common.exception.StorageException

interface ApiKeyRepository {
    suspend fun create(apiKey: ApiKey): Either<StorageException, ApiKey>
    suspend fun findById(id: String): Either<StorageException, ApiKey?>
    suspend fun findByKeyHash(keyHash: String): Either<StorageException, ApiKey?>
    suspend fun findByUserId(userId: String): Either<StorageException, List<ApiKey>>
    suspend fun update(apiKey: ApiKey): Either<StorageException, ApiKey>
    suspend fun delete(id: String): Either<StorageException, Unit>
    suspend fun deleteByUserId(userId: String): Either<StorageException, Unit>
}
