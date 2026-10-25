package com.vaultstadio.app.feature.activity.di

import com.vaultstadio.app.feature.activity.ActivityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureActivityModule = module {
    viewModel { ActivityViewModel(get()) }
}
