package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.repository.CollaborationDocumentsRepository
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.CollaborationOperationsTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.selectAll

internal class ExposedCollaborationDocuments : CollaborationDocumentsRepository {
    override suspend fun getDocumentState(itemId: String): Either<StorageException, DocumentState?> {
        return collaborationRepositoryCall("Failed to get document state") {
            collaborationDbQuery {
                DocumentStatesTable.selectAll()
                    .where { DocumentStatesTable.itemId eq itemId }
                    .map { it.toDocumentState() }
                    .singleOrNull()
            }.right()
        }
    }

    override suspend fun saveDocumentState(state: DocumentState): Either<StorageException, DocumentState> {
        return collaborationRepositoryCall("Failed to save document state") {
            collaborationDbQuery {
                val exists =
                    DocumentStatesTable.selectAll().where { DocumentStatesTable.itemId eq state.itemId }.count() > 0
                persistCollaborationDocument(state, exists)
            }
            state.right()
        }
    }

    override suspend fun applyOperation(
        itemId: String,
        operation: CollaborationOperation,
    ): Either<StorageException, DocumentState> {
        return collaborationRepositoryCall("Failed to apply operation") {
            collaborationDbQuery {
                insertCollaborationOperation(itemId, operation)

                // Get current state and apply operation
                val currentState = DocumentStatesTable.selectAll()
                    .where { DocumentStatesTable.itemId eq itemId }
                    .map { it.toDocumentState() }
                    .singleOrNull()

                val newContent = if (currentState != null) {
                    applyOperationToContent(currentState.content, operation)
                } else {
                    ""
                }

                val newVersion = (currentState?.version ?: 0) + 1
                val newState = DocumentState(
                    itemId = itemId,
                    version = newVersion,
                    content = newContent,
                    operations = emptyList(),
                    lastModified = Clock.System.now(),
                )

                persistCollaborationDocument(newState, currentState != null)

                newState
            }.right()
        }
    }

    override suspend fun getOperationsSince(
        itemId: String,
        sinceVersion: Long,
    ): Either<StorageException, List<CollaborationOperation>> {
        return collaborationRepositoryCall("Failed to get operations") {
            collaborationDbQuery {
                CollaborationOperationsTable.selectAll()
                    .where {
                        CollaborationOperationsTable.itemId eq itemId and
                            (CollaborationOperationsTable.baseVersion greater sinceVersion)
                    }
                    .orderBy(CollaborationOperationsTable.baseVersion, SortOrder.ASC)
                    .map { it.toCollaborationOperation() }
            }.right()
        }
    }

    override fun streamOperations(itemId: String): Flow<CollaborationOperation> = flow {
        // Real-time streaming would use a different mechanism
        // For now, just emit existing operations
    }
}
