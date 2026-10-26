package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.version.repository.VersionContentReferenceRepository
import com.vaultstadio.infrastructure.persistence.entities.FileVersionsTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.sql.SQLException

/** Query existence only; never hydrate another resource's names, owner, size or content metadata. */
class ExposedVersionContentReferenceRepository : VersionContentReferenceRepository {
    override suspend fun hasOtherReferences(
        storageKey: String,
        excludingVersionId: String,
    ): Either<StorageException, Boolean> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val historical = FileVersionsTable.select(FileVersionsTable.id)
                .where {
                    FileVersionsTable.storageKey eq storageKey and (FileVersionsTable.id neq excludingVersionId)
                }
                .limit(1).singleOrNull() != null
            historical ||
                StorageItemsTable.select(StorageItemsTable.id)
                    .where { StorageItemsTable.storageKey eq storageKey }
                    .limit(1).singleOrNull() != null
        }.right()
    } catch (error: SQLException) {
        DatabaseException("Failed to check version content references", error).left()
    }
}
