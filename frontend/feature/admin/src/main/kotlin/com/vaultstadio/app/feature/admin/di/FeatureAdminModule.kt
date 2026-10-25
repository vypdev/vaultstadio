package com.vaultstadio.app.feature.admin.di

import com.vaultstadio.app.feature.admin.AdminViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureAdminModule = module {
    viewModel { AdminViewModel(get(), get(), get(), get()) }
}
