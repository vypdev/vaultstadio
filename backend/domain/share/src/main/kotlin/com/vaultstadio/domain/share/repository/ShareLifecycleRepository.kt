package com.vaultstadio.domain.share.repository

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink
import kotlinx.datetime.Instant

interface ShareLifecycleRepository {

    suspend fun create(share: ShareLink): Either<StorageException, ShareLink>
    suspend fun update(share: ShareLink): Either<StorageException, ShareLink>
    suspend fun delete(id: String): Either<StorageException, Unit>
    suspend fun deleteByItemId(itemId: String): Either<StorageException, Int>
    suspend fun deactivateExpired(before: Instant): Either<StorageException, Int>
}
