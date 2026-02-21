package com.vaultstadio.app.feature.federation.di

import com.vaultstadio.app.domain.federation.usecase.AcceptFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.BlockInstanceUseCase
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
import com.vaultstadio.app.feature.federation.FederationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureFederationModule = module {
    viewModel {
        FederationViewModel(
            get<GetFederatedInstancesUseCase>(),
            get<GetFederatedInstanceUseCase>(),
            get<RequestFederationUseCase>(),
            get<BlockInstanceUseCase>(),
            get<RemoveInstanceUseCase>(),
            get<GetOutgoingFederatedSharesUseCase>(),
            get<GetIncomingFederatedSharesUseCase>(),
            get<AcceptFederatedShareUseCase>(),
            get<DeclineFederatedShareUseCase>(),
            get<RevokeFederatedShareUseCase>(),
            get<GetFederatedIdentitiesUseCase>(),
            get<LinkIdentityUseCase>(),
            get<UnlinkIdentityUseCase>(),
            get<GetFederatedActivitiesUseCase>(),
        )
    }
}
