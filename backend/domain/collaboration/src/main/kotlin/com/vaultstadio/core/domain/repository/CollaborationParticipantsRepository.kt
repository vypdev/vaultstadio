package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.domain.common.exception.StorageException

interface CollaborationParticipantsRepository {
    /**
     * Add a participant to a session.
     *
     * @param sessionId Session ID
     * @param participant The participant to add
     * @return The updated session
     */
    suspend fun addParticipant(
        sessionId: String,
        participant: CollaborationParticipant,
    ): Either<StorageException, CollaborationSession>

    /**
     * Remove a participant from a session.
     *
     * @param sessionId Session ID
     * @param participantId Participant ID
     * @return The updated session
     */
    suspend fun removeParticipant(
        sessionId: String,
        participantId: String,
    ): Either<StorageException, CollaborationSession>

    /**
     * Update a participant's state.
     *
     * @param sessionId Session ID
     * @param participant Updated participant
     * @return The updated participant
     */
    suspend fun updateParticipant(
        sessionId: String,
        participant: CollaborationParticipant,
    ): Either<StorageException, CollaborationParticipant>

    /**
     * Get all participants in a session.
     *
     * @param sessionId Session ID
     * @return List of participants
     */
    suspend fun getParticipants(sessionId: String): Either<StorageException, List<CollaborationParticipant>>
}
