package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.storage.CopyItemUseCase
import com.vaultstadio.application.usecase.storage.CopyItemUseCaseImpl
import com.vaultstadio.application.usecase.storage.CreateFolderUseCase
import com.vaultstadio.application.usecase.storage.CreateFolderUseCaseImpl
import com.vaultstadio.application.usecase.storage.DeleteItemUseCase
import com.vaultstadio.application.usecase.storage.DeleteItemUseCaseImpl
import com.vaultstadio.application.usecase.storage.DownloadFileUseCase
import com.vaultstadio.application.usecase.storage.DownloadFileUseCaseImpl
import com.vaultstadio.application.usecase.storage.GetBreadcrumbsUseCase
import com.vaultstadio.application.usecase.storage.GetBreadcrumbsUseCaseImpl
import com.vaultstadio.application.usecase.storage.GetItemUseCase
import com.vaultstadio.application.usecase.storage.GetItemUseCaseImpl
import com.vaultstadio.application.usecase.storage.GetOrCreateFolderUseCase
import com.vaultstadio.application.usecase.storage.GetOrCreateFolderUseCaseImpl
import com.vaultstadio.application.usecase.storage.GetRecentItemsUseCase
import com.vaultstadio.application.usecase.storage.GetRecentItemsUseCaseImpl
import com.vaultstadio.application.usecase.storage.GetStarredItemsUseCase
import com.vaultstadio.application.usecase.storage.GetStarredItemsUseCaseImpl
import com.vaultstadio.application.usecase.storage.GetTrashItemsUseCase
import com.vaultstadio.application.usecase.storage.GetTrashItemsUseCaseImpl
import com.vaultstadio.application.usecase.storage.ListFolderUseCase
import com.vaultstadio.application.usecase.storage.ListFolderUseCaseImpl
import com.vaultstadio.application.usecase.storage.MoveItemUseCase
import com.vaultstadio.application.usecase.storage.MoveItemUseCaseImpl
import com.vaultstadio.application.usecase.storage.RenameItemUseCase
import com.vaultstadio.application.usecase.storage.RenameItemUseCaseImpl
import com.vaultstadio.application.usecase.storage.RestoreItemUseCase
import com.vaultstadio.application.usecase.storage.RestoreItemUseCaseImpl
import com.vaultstadio.application.usecase.storage.SearchUseCase
import com.vaultstadio.application.usecase.storage.SearchUseCaseImpl
import com.vaultstadio.application.usecase.storage.SetStarUseCase
import com.vaultstadio.application.usecase.storage.SetStarUseCaseImpl
import com.vaultstadio.application.usecase.storage.ToggleStarUseCase
import com.vaultstadio.application.usecase.storage.ToggleStarUseCaseImpl
import com.vaultstadio.application.usecase.storage.TrashItemUseCase
import com.vaultstadio.application.usecase.storage.TrashItemUseCaseImpl
import com.vaultstadio.application.usecase.storage.UploadFileUseCase
import com.vaultstadio.application.usecase.storage.UploadFileUseCaseImpl
import org.koin.dsl.module

fun applicationStorageModule() = module {
    single<ListFolderUseCase> { ListFolderUseCaseImpl(get()) }
    single<GetItemUseCase> { GetItemUseCaseImpl(get()) }
    single<CreateFolderUseCase> { CreateFolderUseCaseImpl(get()) }
    single<UploadFileUseCase> { UploadFileUseCaseImpl(get()) }
    single<DownloadFileUseCase> { DownloadFileUseCaseImpl(get()) }
    single<RenameItemUseCase> { RenameItemUseCaseImpl(get()) }
    single<MoveItemUseCase> { MoveItemUseCaseImpl(get()) }
    single<CopyItemUseCase> { CopyItemUseCaseImpl(get()) }
    single<ToggleStarUseCase> { ToggleStarUseCaseImpl(get()) }
    single<TrashItemUseCase> { TrashItemUseCaseImpl(get()) }
    single<RestoreItemUseCase> { RestoreItemUseCaseImpl(get()) }
    single<DeleteItemUseCase> { DeleteItemUseCaseImpl(get()) }
    single<GetTrashItemsUseCase> { GetTrashItemsUseCaseImpl(get()) }
    single<GetStarredItemsUseCase> { GetStarredItemsUseCaseImpl(get()) }
    single<GetRecentItemsUseCase> { GetRecentItemsUseCaseImpl(get()) }
    single<GetBreadcrumbsUseCase> { GetBreadcrumbsUseCaseImpl(get()) }
    single<SetStarUseCase> { SetStarUseCaseImpl(get()) }
    single<SearchUseCase> { SearchUseCaseImpl(get()) }
    single<GetOrCreateFolderUseCase> { GetOrCreateFolderUseCaseImpl(get()) }
}
