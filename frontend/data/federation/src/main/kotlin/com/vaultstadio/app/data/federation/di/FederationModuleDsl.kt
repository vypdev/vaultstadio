/**
 * Koin module for federation (API, service, repository, use cases).
 * ComposeApp loads it via modules(... + federationModule).
 */

package com.vaultstadio.app.data.federation.di

import com.vaultstadio.app.data.federation.api.FederationApi
import com.vaultstadio.app.data.federation.repository.FederationRepositoryImpl
import com.vaultstadio.app.data.federation.service.FederationService
import com.vaultstadio.app.data.federation.usecase.AcceptFederatedShareUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.BlockInstanceUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.CreateFederatedShareUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.DeclineFederatedShareUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetFederatedActivitiesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetFederatedIdentitiesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetFederatedInstanceUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetFederatedInstancesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetIncomingFederatedSharesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetOutgoingFederatedSharesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.LinkIdentityUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.RemoveInstanceUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.RequestFederationUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.RevokeFederatedShareUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.UnlinkIdentityUseCaseImpl
import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.usecase.AcceptFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.BlockInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.CreateFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.DeclineFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedActivitiesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedIdentitiesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedInstancesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetIncomingFederatedSharesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetOutgoingFederatedSharesUseCase
import com.vaultstadio.app.domain.federation.usecase.LinkIdentityUseCase
import com.vaultstadio.app.domain.federation.usecase.RemoveInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.RequestFederationUseCase
import com.vaultstadio.app.domain.federation.usecase.RevokeFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.UnlinkIdentityUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val federationModule = module {
    single { FederationApi(get<HttpClient>()) }
    single { FederationService(get()) }
    single<FederationRepository> { FederationRepositoryImpl(get()) }

    factory<RequestFederationUseCase> { RequestFederationUseCaseImpl(get()) }
    factory<GetFederatedInstancesUseCase> { GetFederatedInstancesUseCaseImpl(get()) }
    factory<GetFederatedInstanceUseCase> { GetFederatedInstanceUseCaseImpl(get()) }
    factory<BlockInstanceUseCase> { BlockInstanceUseCaseImpl(get()) }
    factory<RemoveInstanceUseCase> { RemoveInstanceUseCaseImpl(get()) }
    factory<CreateFederatedShareUseCase> { CreateFederatedShareUseCaseImpl(get()) }
    factory<GetOutgoingFederatedSharesUseCase> { GetOutgoingFederatedSharesUseCaseImpl(get()) }
    factory<GetIncomingFederatedSharesUseCase> { GetIncomingFederatedSharesUseCaseImpl(get()) }
    factory<AcceptFederatedShareUseCase> { AcceptFederatedShareUseCaseImpl(get()) }
    factory<DeclineFederatedShareUseCase> { DeclineFederatedShareUseCaseImpl(get()) }
    factory<RevokeFederatedShareUseCase> { RevokeFederatedShareUseCaseImpl(get()) }
    factory<LinkIdentityUseCase> { LinkIdentityUseCaseImpl(get()) }
    factory<GetFederatedIdentitiesUseCase> { GetFederatedIdentitiesUseCaseImpl(get()) }
    factory<UnlinkIdentityUseCase> { UnlinkIdentityUseCaseImpl(get()) }
    factory<GetFederatedActivitiesUseCase> { GetFederatedActivitiesUseCaseImpl(get()) }
}
