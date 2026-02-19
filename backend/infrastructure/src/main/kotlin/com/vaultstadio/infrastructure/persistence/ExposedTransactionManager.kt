/**
 * VaultStadio Exposed Transaction Manager
 *
 * Implementation of TransactionManager using Jetbrains Exposed.
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.service.TransactionManager
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Exposed implementation of TransactionManager.
 *
 * Wraps suspend functions in Exposed transactions with proper
 * coroutine context switching to IO dispatcher.
 */
class ExposedTransactionManager : TransactionManager {

    /**
     * Executes the given block within a single database transaction.
     *
     * Uses newSuspendedTransaction with IO dispatcher for proper
     * coroutine handling with database operations.
     *
     * @param block The suspend block to execute within the transaction
     * @return The result of the block
     */
    override suspend fun <T> transaction(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) {
            block()
        }
    }

    /**
     * Executes the given block within a read-only transaction.
     *
     * Currently uses the same implementation as transaction().
     * Future optimization could use read replicas or different
     * isolation levels for read-only queries.
     *
     * @param block The suspend block to execute within the transaction
     * @return The result of the block
     */
    override suspend fun <T> readOnly(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) {
            // Note: Exposed doesn't have built-in read-only transaction support.
            // For PostgreSQL, you could add: exec("SET TRANSACTION READ ONLY")
            // For now, we use the same transaction semantics.
            block()
        }
    }
}
