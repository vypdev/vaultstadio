package com.vaultstadio.app.feature.security.di

import com.vaultstadio.app.feature.security.SecurityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureSecurityModule = module {
    viewModel { SecurityViewModel(get(), get(), get(), get()) }
}
