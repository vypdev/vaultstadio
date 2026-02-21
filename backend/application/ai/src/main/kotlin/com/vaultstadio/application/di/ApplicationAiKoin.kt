package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.ai.AIServiceUseCase
import com.vaultstadio.application.usecase.ai.AIServiceUseCaseImpl
import org.koin.dsl.module

fun applicationAiModule() = module {
    single<AIServiceUseCase> { AIServiceUseCaseImpl(get()) }
}
