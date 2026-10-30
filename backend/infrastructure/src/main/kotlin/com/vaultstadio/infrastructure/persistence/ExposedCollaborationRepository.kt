package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.repository.CollaborationCommentsRepository
import com.vaultstadio.core.domain.repository.CollaborationDocumentsRepository
import com.vaultstadio.core.domain.repository.CollaborationParticipantsRepository
import com.vaultstadio.core.domain.repository.CollaborationPresenceRepository
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.core.domain.repository.CollaborationSessionsRepository

/** Focused persistence adapters preserve the public repository contract. */
class ExposedCollaborationRepository :
    CollaborationRepository,
    CollaborationSessionsRepository by ExposedCollaborationSessions(),
    CollaborationParticipantsRepository by ExposedCollaborationSessions(),
    CollaborationDocumentsRepository by ExposedCollaborationDocuments(),
    CollaborationPresenceRepository by ExposedCollaborationPresence(),
    CollaborationCommentsRepository by ExposedCollaborationComments()
