package com.vaultstadio.app.feature.changepassword.di

import com.vaultstadio.app.feature.changepassword.ChangePasswordViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureChangePasswordModule = module {
    viewModel { ChangePasswordViewModel(get()) }
}
