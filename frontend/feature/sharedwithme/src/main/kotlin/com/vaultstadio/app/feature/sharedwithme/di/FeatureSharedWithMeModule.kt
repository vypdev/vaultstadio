package com.vaultstadio.app.feature.sharedwithme.di

import com.vaultstadio.app.feature.sharedwithme.SharedWithMeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureSharedWithMeModule = module {
    viewModel { SharedWithMeViewModel(get(), get(), get(), get()) }
}
