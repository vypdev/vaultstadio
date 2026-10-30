package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.domain.collaboration.repository.CollaborationMembershipRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.hours

internal class CollaborationSessions(
    private val access: CollaborationAccess,
    private val membership: CollaborationMembershipRepository,
) {
    private val colors = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD",
        "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9", "#F8B500", "#00CED1",
    )

    suspend fun join(
        input: JoinSessionInput,
        actor: String,
        name: String,
    ): Either<StorageException, Pair<CollaborationSession, CollaborationParticipant>> =
        access.item(input.itemId, actor).flatMap { item ->
            val now = Clock.System.now()
            var planned: Pair<CollaborationSession, CollaborationParticipant>? = null
            membership.admit(item.id, actor, now) { existing ->
                prepare(item.id, actor, name, now, existing).map { plan ->
                    planned = plan
                    plan
                }
            }.flatMap { result ->
                checkedAdmission(item.id, actor, planned, result)
            }
        }

    private suspend fun checkedAdmission(
        item: String,
        actor: String,
        expected: Pair<CollaborationSession, CollaborationParticipant>?,
        result: Pair<CollaborationSession, CollaborationParticipant>,
    ): Either<StorageException, Pair<CollaborationSession, CollaborationParticipant>> {
        if (expected == null) return collaborationScopeFailure().left()
        if (result.first.id != expected.first.id ||
            result.second.id != expected.second.id ||
            result.second.userId != actor
        ) {
            return collaborationScopeFailure().left()
        }
        return access.bindSession(result.first, item, expected.first.id).flatMap { bound ->
            if (bound.participants.singleOrNull { it.id == result.second.id } != result.second) {
                collaborationScopeFailure().left()
            } else {
                Pair(bound, result.second).right()
            }
        }
    }

    private fun prepare(
        item: String,
        actor: String,
        name: String,
        at: Instant,
        existing: CollaborationSession?,
    ): Either<StorageException, Pair<CollaborationSession, CollaborationParticipant>> {
        if (existing != null) {
            if (existing.itemId != item ||
                existing.id.isBlank() ||
                existing.participants.map { it.id }.toSet().size != existing.participants.size
            ) {
                return collaborationScopeFailure().left()
            }
            if (existing.expiresAt <= at) return inactiveCollaborationSession().left()
        }
        val session = existing ?: CollaborationSession(
            id = UUID.randomUUID().toString(),
            itemId = item,
            createdAt = at,
            expiresAt = at + 24.hours,
        )
        val participant = CollaborationParticipant(
            id = UUID.randomUUID().toString(),
            userId = actor,
            userName = name,
            color = colors[session.participantCount % colors.size],
            joinedAt = at,
            lastActiveAt = at,
        )
        return Pair(session, participant).right()
    }

    suspend fun leave(session: String, participant: String, actor: String): Either<StorageException, Unit> =
        access.participant(session, participant, actor, active = false).flatMap { bound ->
            membership.leave(bound.context.item.id, session, participant, actor, Clock.System.now())
        }

    suspend fun cursor(
        session: String,
        participant: String,
        cursor: CursorPosition,
        actor: String,
    ): Either<StorageException, CollaborationParticipant> = access.participant(session, participant, actor).flatMap {
        update(it, it.participant.copy(cursor = cursor, lastActiveAt = Clock.System.now()), actor)
    }

    suspend fun selection(
        session: String,
        participant: String,
        selection: TextSelection?,
        actor: String,
    ): Either<StorageException, CollaborationParticipant> = access.participant(session, participant, actor).flatMap {
        update(it, it.participant.copy(selection = selection, lastActiveAt = Clock.System.now()), actor)
    }

    private suspend fun update(
        bound: BoundCollaborationParticipant,
        participant: CollaborationParticipant,
        actor: String,
    ): Either<StorageException, CollaborationParticipant> =
        membership.update(bound.context.item.id, bound.context.session.id, participant, actor, participant.lastActiveAt)
            .flatMap { row ->
                if (row.id != participant.id || row.userId != actor) collaborationScopeFailure().left() else row.right()
            }

    suspend fun cleanup(actor: String): Either<StorageException, Int> {
        if (actor.isBlank()) return AuthorizationException().left()
        return membership.closeExpiredForOwner(actor, Clock.System.now()).flatMap {
            if (it < 0) collaborationScopeFailure().left() else it.right()
        }
    }
}
