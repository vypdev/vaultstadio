package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.version.repository.VersionItemScopeRepository
import com.vaultstadio.infrastructure.persistence.entities.FileVersionsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.sql.SQLException

/** Resolve only the item ID; ownership and full version retrieval follow in the application. */
class ExposedVersionItemScopeRepository : VersionItemScopeRepository {
    override suspend fun findItemId(versionId: String): Either<StorageException, String?> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            FileVersionsTable.select(FileVersionsTable.itemId)
                .where { FileVersionsTable.id eq versionId }
                .singleOrNull()?.get(FileVersionsTable.itemId)
        }.right()
    } catch (error: SQLException) {
        DatabaseException("Failed to resolve version scope", error).left()
    }
}
