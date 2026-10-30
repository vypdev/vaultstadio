package com.vaultstadio.domain.version.repository

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException

/** Resolve only the canonical item scope; this must not retrieve full version metadata or keys. */
interface VersionItemScopeRepository {
    suspend fun findItemId(versionId: String): Either<StorageException, String?>
}
