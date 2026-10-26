package com.vaultstadio.domain.version.repository

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException

/** Minimal liveness predicate, used only after canonical ownership and version binding. */
interface VersionContentReferenceRepository {
    suspend fun hasOtherReferences(storageKey: String, excludingVersionId: String): Either<StorageException, Boolean>
}
