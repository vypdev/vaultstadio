package com.vaultstadio.app.feature.collaboration.di

import com.vaultstadio.app.feature.collaboration.CollaborationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureCollaborationModule = module {
    viewModel { (itemId: String) ->
        CollaborationViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            itemId,
        )
    }
}
