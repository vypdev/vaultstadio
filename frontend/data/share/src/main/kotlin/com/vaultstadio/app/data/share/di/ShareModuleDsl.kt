/**
 * Koin module for share (API, service, repository, use cases).
 * ComposeApp loads it via modules(... + shareModule).
 */

package com.vaultstadio.app.data.share.di

import com.vaultstadio.app.data.share.api.ShareApi
import com.vaultstadio.app.data.share.repository.ShareRepositoryImpl
import com.vaultstadio.app.data.share.service.ShareService
import com.vaultstadio.app.data.share.usecase.CreateShareUseCaseImpl
import com.vaultstadio.app.data.share.usecase.DeleteShareUseCaseImpl
import com.vaultstadio.app.data.share.usecase.GetMySharesUseCaseImpl
import com.vaultstadio.app.data.share.usecase.GetSharedWithMeUseCaseImpl
import com.vaultstadio.app.domain.share.ShareRepository
import com.vaultstadio.app.domain.share.usecase.CreateShareUseCase
import com.vaultstadio.app.domain.share.usecase.DeleteShareUseCase
import com.vaultstadio.app.domain.share.usecase.GetMySharesUseCase
import com.vaultstadio.app.domain.share.usecase.GetSharedWithMeUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val shareModule = module {
    single { ShareApi(get<HttpClient>()) }
    single { ShareService(get()) }
    single<ShareRepository> { ShareRepositoryImpl(get()) }

    factory<GetMySharesUseCase> { GetMySharesUseCaseImpl(get()) }
    factory<GetSharedWithMeUseCase> { GetSharedWithMeUseCaseImpl(get()) }
    factory<CreateShareUseCase> { CreateShareUseCaseImpl(get()) }
    factory<DeleteShareUseCase> { DeleteShareUseCaseImpl(get()) }
}
