package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.sql.SQLException

internal suspend fun <T> collaborationDbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

/** Repository data/mapping failures are typed; cancellation retains its control-flow meaning. */
internal suspend fun <T> collaborationRepositoryCall(
    description: String,
    operation: suspend () -> Either<StorageException, T>,
): Either<StorageException, T> = try {
    operation()
} catch (error: StorageException) {
    error.left()
} catch (error: SQLException) {
    DatabaseException(description, error).left()
} catch (error: IllegalArgumentException) {
    DatabaseException(description, error).left()
}
