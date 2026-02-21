package com.vaultstadio.domain.auth.repository

import arrow.core.Either
import com.vaultstadio.domain.auth.model.UserSession
import com.vaultstadio.domain.common.exception.StorageException

interface SessionRepository {
    suspend fun create(session: UserSession): Either<StorageException, UserSession>
    suspend fun findById(id: String): Either<StorageException, UserSession?>
    suspend fun findByTokenHash(tokenHash: String): Either<StorageException, UserSession?>
    suspend fun findByRefreshTokenHash(refreshTokenHash: String): Either<StorageException, UserSession?>
    suspend fun findByUserId(userId: String): Either<StorageException, List<UserSession>>
    suspend fun update(session: UserSession): Either<StorageException, UserSession>
    suspend fun delete(id: String): Either<StorageException, Unit>
    suspend fun deleteByUserId(userId: String): Either<StorageException, Unit>
    suspend fun deleteExpired(): Either<StorageException, Int>
    suspend fun updateLastActivity(id: String): Either<StorageException, Unit>
}
