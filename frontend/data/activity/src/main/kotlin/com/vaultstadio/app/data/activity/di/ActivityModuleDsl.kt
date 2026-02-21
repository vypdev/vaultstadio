/**
 * Koin module for activity (API, service, repository, use cases).
 * ComposeApp loads it via modules(... + activityModule).
 */

package com.vaultstadio.app.data.activity.di

import com.vaultstadio.app.data.activity.api.ActivityApi
import com.vaultstadio.app.data.activity.repository.ActivityRepositoryImpl
import com.vaultstadio.app.data.activity.service.ActivityService
import com.vaultstadio.app.data.activity.usecase.GetItemActivityUseCaseImpl
import com.vaultstadio.app.data.activity.usecase.GetRecentActivityUseCaseImpl
import com.vaultstadio.app.domain.activity.ActivityRepository
import com.vaultstadio.app.domain.activity.usecase.GetItemActivityUseCase
import com.vaultstadio.app.domain.activity.usecase.GetRecentActivityUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val activityModule = module {
    single { ActivityApi(get<HttpClient>()) }
    single { ActivityService(get()) }
    single<ActivityRepository> { ActivityRepositoryImpl(get()) }

    factory<GetRecentActivityUseCase> { GetRecentActivityUseCaseImpl(get()) }
    factory<GetItemActivityUseCase> { GetItemActivityUseCaseImpl(get()) }
}
