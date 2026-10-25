package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.sql.SQLException

/** The parent row lock also conflicts with PostgreSQL's child foreign-key insertion lock. */
@Suppress("RedundantSuspendModifier") // Exposed newSuspendedTransaction is a suspending call.
internal suspend fun deleteStorageLeaf(id: String): Either<StorageException, Unit> = try {
    newSuspendedTransaction(Dispatchers.IO) {
        val item = StorageItemsTable.selectAll().where { StorageItemsTable.id eq id }
            .forUpdate().singleOrNull()
        if (item == null) {
            Unit.right()
        } else if (!StorageItemsTable.selectAll().where { StorageItemsTable.parentId eq id }.limit(1).empty()) {
            InvalidOperationException(
                operation = "delete-nonempty-folder",
                message = "Delete the folder contents before permanently deleting this folder",
            ).left()
        } else {
            StorageItemsTable.deleteWhere { StorageItemsTable.id eq id }
            Unit.right()
        }
    }
} catch (error: SQLException) {
    DatabaseException("Failed to delete storage item", error).left()
}
