package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.repository.CollaborationDocumentsRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.CollaborationOperationsTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.selectAll

internal class ExposedCollaborationDocuments : CollaborationDocumentsRepository {
    override suspend fun saveExistingSnapshot(
        expected: DocumentState,
        actorId: String,
    ): Either<StorageException, DocumentState> = collaborationRepositoryCall(
        "Failed to persist collaboration snapshot",
    ) {
        collaborationDbQuery {
            lockCollaborationItem(expected.itemId, actorId)
            requireCollaborationSnapshot(expected)
            // This boundary acknowledges an already-persisted working snapshot, never a canonical byte commit.
            expected
        }.right()
    }

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
        actorId: String,
        expected: DocumentState,
    ): Either<StorageException, DocumentState> = collaborationRepositoryCall("Failed to apply operation") {
        collaborationDbQuery {
            lockCollaborationItem(itemId, actorId)
            if (operation.userId != actorId) throw AuthorizationException()
            if (expected.itemId != itemId) throw DatabaseException("Inconsistent document snapshot scope")
            val current = requireCollaborationSnapshot(expected)
            val updated = current.copy(
                version = current.version + 1,
                content = applyOperationToContent(current.content, operation),
                operations = emptyList(),
                lastModified = Clock.System.now(),
            )
            insertCollaborationOperation(itemId, operation)
            persistCollaborationDocument(updated, true)
            updated
        }.right()
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
                            (CollaborationOperationsTable.baseVersion greaterEq sinceVersion)
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
