package com.vaultstadio.domain.share.repository

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink
import kotlinx.datetime.Instant

interface ShareRepository {
    suspend fun create(share: ShareLink): Either<StorageException, ShareLink>
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
    suspend fun update(share: ShareLink): Either<StorageException, ShareLink>
    suspend fun incrementDownloadCount(id: String): Either<StorageException, Int>
    suspend fun delete(id: String): Either<StorageException, Unit>
    suspend fun deleteByItemId(itemId: String): Either<StorageException, Int>
    suspend fun deactivateExpired(before: Instant): Either<StorageException, Int>
}
