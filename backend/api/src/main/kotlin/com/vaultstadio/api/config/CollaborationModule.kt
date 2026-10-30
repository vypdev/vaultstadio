package com.vaultstadio.api.config

import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.domain.collaboration.repository.CollaborationMembershipRepository
import com.vaultstadio.domain.collaboration.repository.CollaborationScopeRepository
import com.vaultstadio.infrastructure.persistence.ExposedCollaborationMembershipRepository
import com.vaultstadio.infrastructure.persistence.ExposedCollaborationRepository
import com.vaultstadio.infrastructure.persistence.ExposedCollaborationScopeRepository
import org.koin.dsl.module

internal fun collaborationServicesModule() = module {
    single<CollaborationRepository> { ExposedCollaborationRepository() }
    single<CollaborationScopeRepository> { ExposedCollaborationScopeRepository() }
    single<CollaborationMembershipRepository> { ExposedCollaborationMembershipRepository() }
    single {
        CollaborationService(
            collaborationRepository = get(),
            storageItemRepository = get(),
            scopeRepository = get(),
            membershipRepository = get(),
        )
    }
}
