/**
 * Koin module for storage (API, service, repository, use cases).
 * Uses classic DSL to avoid Koin compiler codegen issues on Kotlin/WASM.
 * ComposeApp loads it via modules(runtimeModules(url) + authModule + storageModule).
 */

package com.vaultstadio.app.data.storage.di

import com.vaultstadio.app.data.storage.api.StorageApi
import com.vaultstadio.app.data.storage.repository.StorageRepositoryImpl
import com.vaultstadio.app.data.storage.service.StorageService
import com.vaultstadio.app.data.storage.usecase.BatchCopyUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.BatchDeleteUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.BatchMoveUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.BatchStarUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.CopyItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.CreateFolderUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.DeleteItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.DownloadFileUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.EmptyTrashUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetBreadcrumbsUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetFolderItemsUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetRecentUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetStarredUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetTrashUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.MoveItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.RenameItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.RestoreItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.SearchUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.TrashItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.ToggleStarUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.UploadFileUseCaseImpl
import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.BatchCopyUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchDeleteUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchMoveUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchStarUseCase
import com.vaultstadio.app.domain.storage.usecase.CopyItemUseCase
import com.vaultstadio.app.domain.storage.usecase.CreateFolderUseCase
import com.vaultstadio.app.domain.storage.usecase.DeleteItemUseCase
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import com.vaultstadio.app.domain.storage.usecase.EmptyTrashUseCase
import com.vaultstadio.app.domain.storage.usecase.GetBreadcrumbsUseCase
import com.vaultstadio.app.domain.storage.usecase.GetFolderItemsUseCase
import com.vaultstadio.app.domain.storage.usecase.GetItemUseCase
import com.vaultstadio.app.domain.storage.usecase.GetRecentUseCase
import com.vaultstadio.app.domain.storage.usecase.GetStarredUseCase
import com.vaultstadio.app.domain.storage.usecase.GetTrashUseCase
import com.vaultstadio.app.domain.storage.usecase.MoveItemUseCase
import com.vaultstadio.app.domain.storage.usecase.RenameItemUseCase
import com.vaultstadio.app.domain.storage.usecase.RestoreItemUseCase
import com.vaultstadio.app.domain.storage.usecase.SearchUseCase
import com.vaultstadio.app.domain.storage.usecase.TrashItemUseCase
import com.vaultstadio.app.domain.storage.usecase.ToggleStarUseCase
import com.vaultstadio.app.domain.storage.usecase.UploadFileUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val storageModule = module {
    single { StorageApi(get<HttpClient>()) }
    single { StorageService(get()) }
    single<StorageRepository> { StorageRepositoryImpl(get(), get(), get()) }

    factory<GetFolderItemsUseCase> { GetFolderItemsUseCaseImpl(get()) }
    factory<GetItemUseCase> { GetItemUseCaseImpl(get()) }
    factory<CreateFolderUseCase> { CreateFolderUseCaseImpl(get()) }
    factory<GetBreadcrumbsUseCase> { GetBreadcrumbsUseCaseImpl(get()) }
    factory<RenameItemUseCase> { RenameItemUseCaseImpl(get()) }
    factory<MoveItemUseCase> { MoveItemUseCaseImpl(get()) }
    factory<CopyItemUseCase> { CopyItemUseCaseImpl(get()) }
    factory<ToggleStarUseCase> { ToggleStarUseCaseImpl(get()) }
    factory<TrashItemUseCase> { TrashItemUseCaseImpl(get()) }
    factory<DeleteItemUseCase> { DeleteItemUseCaseImpl(get()) }
    factory<RestoreItemUseCase> { RestoreItemUseCaseImpl(get()) }
    factory<GetTrashUseCase> { GetTrashUseCaseImpl(get()) }
    factory<EmptyTrashUseCase> { EmptyTrashUseCaseImpl(get()) }
    factory<GetStarredUseCase> { GetStarredUseCaseImpl(get()) }
    factory<GetRecentUseCase> { GetRecentUseCaseImpl(get()) }
    factory<SearchUseCase> { SearchUseCaseImpl(get()) }
    factory<BatchDeleteUseCase> { BatchDeleteUseCaseImpl(get()) }
    factory<BatchMoveUseCase> { BatchMoveUseCaseImpl(get()) }
    factory<BatchCopyUseCase> { BatchCopyUseCaseImpl(get()) }
    factory<BatchStarUseCase> { BatchStarUseCaseImpl(get()) }
    factory<UploadFileUseCase> { UploadFileUseCaseImpl(get()) }
    factory<DownloadFileUseCase> { DownloadFileUseCaseImpl(get()) }
}
