package com.vaultstadio.app.feature.shares.di

import com.vaultstadio.app.feature.shares.SharesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureSharesModule = module {
    viewModel { SharesViewModel(get(), get(), get()) }
}
