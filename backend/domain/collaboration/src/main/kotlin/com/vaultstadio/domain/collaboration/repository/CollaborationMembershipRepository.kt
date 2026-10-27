package com.vaultstadio.domain.collaboration.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.datetime.Instant

/**
 * Atomic membership effects. Implementations recheck canonical ownership under item/session locks.
 * The synchronous admission plan runs after the lock and before writes; it performs no provider I/O.
 */
typealias CollaborationAdmissionPlan = Pair<CollaborationSession, CollaborationParticipant>
typealias CollaborationAdmission = (CollaborationSession?) -> Either<StorageException, CollaborationAdmissionPlan>

interface CollaborationMembershipRepository {
    suspend fun admit(
        itemId: String,
        actorId: String,
        at: Instant,
        prepare: CollaborationAdmission,
    ): Either<StorageException, Pair<CollaborationSession, CollaborationParticipant>>

    suspend fun leave(
        itemId: String,
        sessionId: String,
        participantId: String,
        actorId: String,
        at: Instant,
    ): Either<StorageException, Unit>

    suspend fun update(
        itemId: String,
        sessionId: String,
        participant: CollaborationParticipant,
        actorId: String,
        at: Instant,
    ): Either<StorageException, CollaborationParticipant>

    suspend fun closeExpiredForOwner(actorId: String, at: Instant): Either<StorageException, Int>
}
