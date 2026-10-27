package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException

internal class CollaborationDocuments(
    private val access: CollaborationAccess,
    private val repository: CollaborationRepository,
) {
    suspend fun read(item: String, actor: String): Either<StorageException, DocumentState?> =
        access.item(item, actor).flatMap {
            repository.getDocumentState(item).flatMap { bind(item, it) }
        }

    suspend fun apply(
        item: String,
        operation: CollaborationOperation,
        actor: String,
    ): Either<StorageException, DocumentState> = access.item(item, actor).flatMap {
        if (operation.userId != actor) return@flatMap AuthorizationException().left()
        repository.getDocumentState(item).flatMap { state ->
            bind(item, state).flatMap { bound ->
                if (bound == null) return@flatMap missingCollaborationResource().left()
                repository.applyOperation(
                    item,
                    transformCollaborationOperation(operation, bound),
                    actor,
                    bound,
                ).flatMap { updated ->
                    if (updated.itemId != item) collaborationScopeFailure().left() else updated.right()
                }
            }
        }
    }

    suspend fun operations(
        item: String,
        since: Long,
        actor: String,
    ): Either<StorageException, List<CollaborationOperation>> = access.item(item, actor).flatMap {
        repository.getOperationsSince(item, since)
    }

    suspend fun save(item: String, actor: String): Either<StorageException, DocumentState> = read(item, actor).flatMap {
        if (it == null) {
            missingCollaborationResource().left()
        } else {
            repository.saveExistingSnapshot(it, actor).flatMap { row ->
                if (row.itemId != item) collaborationScopeFailure().left() else row.right()
            }
        }
    }

    private fun bind(item: String, row: DocumentState?): Either<StorageException, DocumentState?> =
        if (row != null && row.itemId != item) collaborationScopeFailure().left() else row.right()
}
