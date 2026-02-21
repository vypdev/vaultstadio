package com.vaultstadio.app.feature.versionhistory.di

import com.vaultstadio.app.feature.versionhistory.VersionHistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureVersionHistoryModule = module {
    viewModel { (itemId: String) ->
        VersionHistoryViewModel(
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
