package com.vaultstadio.app.feature.sync.di

import com.vaultstadio.app.feature.sync.SyncViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureSyncModule = module {
    viewModel { SyncViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
