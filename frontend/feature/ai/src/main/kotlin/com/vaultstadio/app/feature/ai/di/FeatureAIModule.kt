package com.vaultstadio.app.feature.ai.di

import com.vaultstadio.app.domain.ai.usecase.AIChatUseCase
import com.vaultstadio.app.domain.ai.usecase.ClassifyContentUseCase
import com.vaultstadio.app.domain.ai.usecase.DeleteAIProviderUseCase
import com.vaultstadio.app.domain.ai.usecase.DescribeImageUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProviderStatusUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProvidersUseCase
import com.vaultstadio.app.domain.ai.usecase.GetProviderModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.SummarizeTextUseCase
import com.vaultstadio.app.domain.ai.usecase.TagImageUseCase
import com.vaultstadio.app.feature.ai.AIViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureAIModule = module {
    viewModel {
        AIViewModel(
            get<GetAIProvidersUseCase>(),
            get<GetAIModelsUseCase>(),
            get<GetProviderModelsUseCase>(),
            get<GetAIProviderStatusUseCase>(),
            get<DeleteAIProviderUseCase>(),
            get<AIChatUseCase>(),
            get<DescribeImageUseCase>(),
            get<TagImageUseCase>(),
            get<ClassifyContentUseCase>(),
            get<SummarizeTextUseCase>(),
        )
    }
}
