package com.vaultstadio.domain.share.repository

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink

interface ShareLookupRepository {
    suspend fun findById(id: String): Either<StorageException, ShareLink?>
    suspend fun findByToken(token: String): Either<StorageException, ShareLink?>
    suspend fun findByItemId(itemId: String): Either<StorageException, List<ShareLink>>
    suspend fun findByCreatedBy(
        userId: String,
        activeOnly: Boolean = true,
    ): Either<StorageException, List<ShareLink>>

    suspend fun findSharedWithUser(
        userId: String,
        activeOnly: Boolean = true,
    ): Either<StorageException, List<ShareLink>>
}
