package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.datetime.Clock

internal class CollaborationPresence(
    private val access: CollaborationAccess,
    private val repository: CollaborationRepository,
) {
    fun targets(users: List<String>, actor: String): Either<StorageException, Unit> =
        if (actor.isBlank() || users.any { it != actor }) AuthorizationException().left() else Unit.right()

    suspend fun bind(row: UserPresence, actor: String): Either<StorageException, UserPresence> {
        if (row.userId != actor) return collaborationScopeFailure().left()
        return pointers(row.activeSession, row.activeDocument, actor).map { row }
    }

    private suspend fun pointers(session: String?, document: String?, actor: String): Either<StorageException, Unit> {
        if (session != null) {
            return access.session(session, actor).flatMap { bound ->
                if (document != null && document != bound.item.id) {
                    AuthorizationException().left()
                } else {
                    Unit.right()
                }
            }
        }
        return if (document == null) Unit.right() else access.item(document, actor).map { Unit }
    }

    suspend fun update(
        actor: String,
        status: PresenceStatus,
        session: String?,
        document: String?,
    ): Either<StorageException, UserPresence> = targets(listOf(actor), actor).flatMap {
        pointers(session, document, actor).flatMap {
            repository.updatePresence(UserPresence(actor, status, Clock.System.now(), session, document))
                .flatMap { bind(it, actor) }
        }
    }

    suspend fun read(users: List<String>, actor: String): Either<StorageException, Map<String, UserPresence>> =
        targets(users, actor).flatMap {
            if (users.isEmpty()) return@flatMap emptyMap<String, UserPresence>().right()
            repository.getPresenceBulk(users).flatMap { bindRows(it, actor) }
        }

    private suspend fun bindRows(
        rows: Map<String, UserPresence>,
        actor: String,
    ): Either<StorageException, Map<String, UserPresence>> {
        for ((key, row) in rows) {
            if (key != actor || key != row.userId) return collaborationScopeFailure().left()
            when (val checked = bind(row, actor)) {
                is Either.Left -> return checked
                is Either.Right -> Unit
            }
        }
        return rows.right()
    }

    suspend fun offline(actor: String): Either<StorageException, Unit> =
        targets(listOf(actor), actor).flatMap { repository.setOffline(actor) }
}
