package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.domain.collaboration.repository.CollaborationScopeRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageAccessPolicy
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import kotlinx.datetime.Clock

internal data class BoundCollaborationSession(val item: StorageItem, val session: CollaborationSession)
internal data class BoundCollaborationParticipant(
    val context: BoundCollaborationSession,
    val participant: CollaborationParticipant,
)

/** One canonical owner gate shared by REST, sockets, streams and application calls. */
internal class CollaborationAccess(
    private val items: StorageItemRepository,
    private val scopes: CollaborationScopeRepository,
    private val repository: CollaborationRepository,
) {
    suspend fun item(id: String, actor: String): Either<StorageException, StorageItem> {
        if (actor.isBlank()) return AuthorizationException().left()
        return items.findById(id).flatMap { row ->
            when {
                row == null -> ItemNotFoundException().left()
                row.id != id -> collaborationScopeFailure().left()
                else -> StorageAccessPolicy.requireOwner(row, actor).flatMap {
                    if (it.type != ItemType.FILE || it.isTrashed) {
                        InvalidOperationException("collaboration", "An active file is required").left()
                    } else {
                        it.right()
                    }
                }
            }
        }
    }

    suspend fun session(
        id: String,
        actor: String,
        active: Boolean = true,
    ): Either<StorageException, BoundCollaborationSession> {
        if (actor.isBlank()) return AuthorizationException().left()
        return scopes.session(id).flatMap { scope ->
            if (scope == null) return@flatMap missingCollaborationResource().left()
            item(scope.itemId, actor).flatMap { item ->
                readSession(id, item, scope.closed, active)
            }
        }
    }

    private suspend fun readSession(
        id: String,
        item: StorageItem,
        closed: Boolean,
        active: Boolean,
    ): Either<StorageException, BoundCollaborationSession> = repository.findSession(id).flatMap { row ->
        if (row == null) return@flatMap missingCollaborationResource().left()
        bindSession(row, item.id, id).flatMap { bound ->
            if (active && (closed || bound.expiresAt <= Clock.System.now())) {
                inactiveCollaborationSession().left()
            } else {
                BoundCollaborationSession(item, bound).right()
            }
        }
    }

    suspend fun bindSession(
        row: CollaborationSession,
        itemId: String,
        sessionId: String,
    ): Either<StorageException, CollaborationSession> {
        if (row.id != sessionId ||
            row.itemId != itemId ||
            row.participants.map { it.id }.toSet().size != row.participants.size
        ) {
            return collaborationScopeFailure().left()
        }
        for (participant in row.participants) {
            when (val scope = scopes.participantSession(participant.id)) {
                is Either.Left -> return scope
                is Either.Right -> if (scope.value != row.id) return collaborationScopeFailure().left()
            }
        }
        return row.right()
    }

    suspend fun participant(
        sessionId: String,
        participantId: String,
        actor: String,
        active: Boolean = true,
    ): Either<StorageException, BoundCollaborationParticipant> = session(sessionId, actor, active).flatMap { context ->
        val participant = context.session.participants.find { it.id == participantId }
        when {
            participant == null -> missingCollaborationResource().left()
            participant.userId != actor -> AuthorizationException().left()
            else -> BoundCollaborationParticipant(context, participant).right()
        }
    }

    suspend fun comment(itemId: String, id: String, actor: String): Either<StorageException, DocumentComment> =
        item(itemId, actor).flatMap {
            scopes.commentItem(id).flatMap { scope ->
                if (scope != itemId) return@flatMap missingCollaborationResource().left()
                repository.findComment(id).flatMap { row ->
                    when {
                        row == null -> missingCollaborationResource().left()
                        row.id != id || row.itemId != itemId -> collaborationScopeFailure().left()
                        else -> row.right()
                    }
                }
            }
        }
}

internal fun collaborationScopeFailure() = DatabaseException("Collaboration repository returned an inconsistent scope")
internal fun missingCollaborationResource() = ItemNotFoundException(message = "Collaboration resource not found")
internal fun inactiveCollaborationSession() = InvalidOperationException("collaboration", "Session is not active")
internal fun <T> Either<StorageException, T>.collaborationValue(): T = fold({ throw it }, { it })
