package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageAccessPolicy
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.sql.SQLException

/** Keep the parent lock through content deletion so no child can appear after the leaf check. */
@Suppress("RedundantSuspendModifier") // Exposed newSuspendedTransaction is a suspending call.
internal suspend fun deleteStorageLeaf(
    id: String,
    actorId: String? = null,
    deleteContent: suspend (StorageItem) -> Either<StorageException, Unit> = { Unit.right() },
): Either<StorageException, StorageItem?> = try {
    newSuspendedTransaction(Dispatchers.IO) {
        val item = StorageItemsTable.selectAll().where { StorageItemsTable.id eq id }
            .forUpdate().singleOrNull()?.toStorageItemModel()
        if (item == null) {
            return@newSuspendedTransaction null.right()
        }
        if (actorId != null) {
            when (val access = StorageAccessPolicy.requireOwner(item, actorId)) {
                is Either.Left -> return@newSuspendedTransaction access
                is Either.Right -> Unit
            }
        }
        if (!StorageItemsTable.selectAll().where { StorageItemsTable.parentId eq id }.limit(1).empty()) {
            return@newSuspendedTransaction InvalidOperationException(
                operation = "delete-nonempty-folder",
                message = "Delete the folder contents before permanently deleting this folder",
            ).left()
        }
        when (val content = deleteContent(item)) {
            is Either.Left -> content
            is Either.Right -> {
                StorageItemsTable.deleteWhere { StorageItemsTable.id eq id }
                item.right()
            }
        }
    }
} catch (error: SQLException) {
    DatabaseException("Failed to delete storage item", error).left()
}
