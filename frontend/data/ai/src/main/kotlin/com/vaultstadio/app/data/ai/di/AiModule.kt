/**
 * Koin module for AI (API, service, repository, use cases).
 */

package com.vaultstadio.app.data.ai.di

import com.vaultstadio.app.data.ai.api.AIApi
import com.vaultstadio.app.data.ai.repository.AIRepositoryImpl
import com.vaultstadio.app.data.ai.service.AIService
import com.vaultstadio.app.data.ai.usecase.AIChatUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.ClassifyContentUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.ConfigureAIProviderUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.DeleteAIProviderUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.DescribeImageUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.GetAIModelsUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.GetAIProviderStatusUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.GetAIProvidersUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.GetProviderModelsUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.SetActiveAIProviderUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.SummarizeTextUseCaseImpl
import com.vaultstadio.app.data.ai.usecase.TagImageUseCaseImpl
import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.usecase.AIChatUseCase
import com.vaultstadio.app.domain.ai.usecase.ClassifyContentUseCase
import com.vaultstadio.app.domain.ai.usecase.ConfigureAIProviderUseCase
import com.vaultstadio.app.domain.ai.usecase.DeleteAIProviderUseCase
import com.vaultstadio.app.domain.ai.usecase.DescribeImageUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProviderStatusUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProvidersUseCase
import com.vaultstadio.app.domain.ai.usecase.GetProviderModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.SetActiveAIProviderUseCase
import com.vaultstadio.app.domain.ai.usecase.SummarizeTextUseCase
import com.vaultstadio.app.domain.ai.usecase.TagImageUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val aiModule = module {
    single { AIApi(get<HttpClient>()) }
    single { AIService(get()) }
    single<AIRepository> { AIRepositoryImpl(get()) }

    factory<GetAIProvidersUseCase> { GetAIProvidersUseCaseImpl(get()) }
    factory<GetAIModelsUseCase> { GetAIModelsUseCaseImpl(get()) }
    factory<ConfigureAIProviderUseCase> { ConfigureAIProviderUseCaseImpl(get()) }
    factory<SetActiveAIProviderUseCase> { SetActiveAIProviderUseCaseImpl(get()) }
    factory<DeleteAIProviderUseCase> { DeleteAIProviderUseCaseImpl(get()) }
    factory<GetAIProviderStatusUseCase> { GetAIProviderStatusUseCaseImpl(get()) }
    factory<GetProviderModelsUseCase> { GetProviderModelsUseCaseImpl(get()) }
    factory<AIChatUseCase> { AIChatUseCaseImpl(get()) }
    factory<DescribeImageUseCase> { DescribeImageUseCaseImpl(get()) }
    factory<TagImageUseCase> { TagImageUseCaseImpl(get()) }
    factory<ClassifyContentUseCase> { ClassifyContentUseCaseImpl(get()) }
    factory<SummarizeTextUseCase> { SummarizeTextUseCaseImpl(get()) }
}
