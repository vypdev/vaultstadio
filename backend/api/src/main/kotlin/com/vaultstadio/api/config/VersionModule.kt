package com.vaultstadio.api.config

import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.domain.version.repository.VersionContentReferenceRepository
import com.vaultstadio.domain.version.repository.VersionItemScopeRepository
import com.vaultstadio.infrastructure.persistence.ExposedFileVersionRepository
import com.vaultstadio.infrastructure.persistence.ExposedVersionContentReferenceRepository
import com.vaultstadio.infrastructure.persistence.ExposedVersionItemScopeRepository
import org.koin.dsl.module

/** Keep minimal scope lookup separate from full version persistence and wire both at composition. */
internal fun versionServicesModule() = module {
    single<FileVersionRepository> { ExposedFileVersionRepository() }
    single<VersionItemScopeRepository> { ExposedVersionItemScopeRepository() }
    single<VersionContentReferenceRepository> { ExposedVersionContentReferenceRepository() }
    single {
        FileVersionService(
            versionRepository = get(),
            itemRepository = get(),
            storageBackend = get(),
            scopeRepository = get(),
            contentReferences = get(),
        )
    }
}
