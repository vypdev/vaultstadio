package com.vaultstadio.api.config

import com.vaultstadio.core.share.access.PublicShareService
import com.vaultstadio.domain.share.access.PublicShareAccessRepository
import com.vaultstadio.domain.share.access.PublicShareClock
import com.vaultstadio.domain.share.access.ShareContentReader
import com.vaultstadio.domain.share.access.SharePasswordVerifier
import com.vaultstadio.infrastructure.persistence.ExposedPublicShareAccess
import com.vaultstadio.infrastructure.security.SharePasswordVerifierAdapter
import com.vaultstadio.infrastructure.storage.ShareContentReaderAdapter
import kotlinx.datetime.Clock
import org.koin.dsl.module

fun shareAccessServicesModule() = module {
    single<PublicShareClock> { PublicShareClock { Clock.System.now() } }
    single<PublicShareAccessRepository> { ExposedPublicShareAccess(get()) }
    single<SharePasswordVerifier> { SharePasswordVerifierAdapter(get()) }
    single<ShareContentReader> { ShareContentReaderAdapter(get()) }
    single { PublicShareService(get(), get(), get(), get()) }
}
