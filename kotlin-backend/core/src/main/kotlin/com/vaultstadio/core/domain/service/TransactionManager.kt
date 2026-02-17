/**
 * VaultStadio Transaction Manager
 *
 * Provides transactional boundaries for service-level operations
 * that span multiple repositories.
 */

package com.vaultstadio.core.domain.service

/**
 * Transaction manager interface for coordinating database transactions.
 *
 * Use this when a service operation needs to modify multiple repositories
 * atomically. Individual repository operations already run in their own
 * transactions, but complex operations that require all-or-nothing
 * semantics should use this wrapper.
 *
 * Example usage:
 * ```kotlin
 * class MyService(
 *     private val transactionManager: TransactionManager,
 *     private val repo1: Repository1,
 *     private val repo2: Repository2,
 * ) {
 *     suspend fun complexOperation(): Either<Error, Result> {
 *         return transactionManager.transaction {
 *             repo1.operation1().bind()
 *             repo2.operation2().bind()
 *         }
 *     }
 * }
 * ```
 */
interface TransactionManager {
    /**
     * Executes the given block within a single database transaction.
     *
     * If the block throws an exception, the transaction is rolled back.
     * If the block completes normally, the transaction is committed.
     *
     * @param block The suspend block to execute within the transaction
     * @return The result of the block
     */
    suspend fun <T> transaction(block: suspend () -> T): T

    /**
     * Executes the given block within a read-only transaction.
     *
     * Optimized for read operations with no write locks.
     *
     * @param block The suspend block to execute within the transaction
     * @return The result of the block
     */
    suspend fun <T> readOnly(block: suspend () -> T): T
}
