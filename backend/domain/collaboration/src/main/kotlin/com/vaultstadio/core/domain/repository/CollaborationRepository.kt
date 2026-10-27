package com.vaultstadio.core.domain.repository

/** Domain-owned compatibility facade; concrete persistence remains outside the domain. */
interface CollaborationRepository :
    CollaborationSessionsRepository,
    CollaborationParticipantsRepository,
    CollaborationDocumentsRepository,
    CollaborationPresenceRepository,
    CollaborationCommentsRepository
