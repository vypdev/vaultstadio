package com.vaultstadio.app.feature.profile.di

import com.vaultstadio.app.feature.profile.ProfileComponent
import com.vaultstadio.app.feature.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureProfileModule = module {
    viewModel { (component: ProfileComponent) ->
        ProfileViewModel(get(), get(), get(), get(), component)
    }
}
