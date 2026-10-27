package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.CancellationException
import java.sql.SQLException

/** Expected database/mapping failures retain their cause; cancellation is never a database result. */
internal suspend fun <T> storageDatabaseResult(
    message: String,
    operation: suspend () -> T,
): Either<StorageException, T> = try {
    operation().right()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: SQLException) {
    DatabaseException(message, failure).left()
} catch (failure: IllegalArgumentException) {
    DatabaseException(message, failure).left()
} catch (failure: IllegalStateException) {
    DatabaseException(message, failure).left()
} catch (failure: NoSuchElementException) {
    DatabaseException(message, failure).left()
}
