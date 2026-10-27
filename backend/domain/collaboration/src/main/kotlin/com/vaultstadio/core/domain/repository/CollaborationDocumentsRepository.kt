package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow

interface CollaborationDocumentsRepository {
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
     * Apply an operation to a document.
     *
     * @param itemId Storage item ID
     * @param operation The operation to apply
     * @return The new document state
     */
    suspend fun applyOperation(
        itemId: String,
        operation: CollaborationOperation,
    ): Either<StorageException, DocumentState>

    /**
     * Get operations since a version.
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
