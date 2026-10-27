package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow

interface CollaborationDocumentsRepository {
    /**
     * Persist only the exact already-read working snapshot. Atomically reject a changed/missing row;
     * never recreate it or overwrite an intervening commit. Bootstrap remains a distinct capability.
     */
    suspend fun saveExistingSnapshot(
        expected: DocumentState,
        actorId: String,
    ): Either<StorageException, DocumentState>

    /**
     * Get or create document state.
     *
     * @param itemId Storage item ID
     * @return Current document state
     */
    suspend fun getDocumentState(itemId: String): Either<StorageException, DocumentState?>

    /**
     * Save document state.
     *
     * @param state Document state to save
     * @return The saved state
     */
    suspend fun saveDocumentState(state: DocumentState): Either<StorageException, DocumentState>

    /**
     * Apply an operation under an authoritative item-owner lock, then a document lock.
     * Reject changed expected state before any operation insert; never recreate a missing document.
     *
     * @param itemId Storage item ID
     * @param operation The operation to apply
     * @return The new document state
     */
    suspend fun applyOperation(
        itemId: String,
        operation: CollaborationOperation,
        actorId: String,
        expected: DocumentState,
    ): Either<StorageException, DocumentState>

    /**
     * Get stored operations with baseVersion >= the requested document version; include its next operation.
     *
     * @param itemId Storage item ID
     * @param sinceVersion Get operations after this version
     * @return List of operations
     */
    suspend fun getOperationsSince(
        itemId: String,
        sinceVersion: Long,
    ): Either<StorageException, List<CollaborationOperation>>

    /**
     * Stream operations in real-time.
     *
     * @param itemId Storage item ID
     * @return Flow of operations
     */
    fun streamOperations(itemId: String): Flow<CollaborationOperation>
}
