package com.vaultstadio.app.feature.files.di

import com.vaultstadio.app.domain.activity.usecase.GetItemActivityUseCase
import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase
import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase
import com.vaultstadio.app.domain.metadata.usecase.GetSearchSuggestionsUseCase
import com.vaultstadio.app.domain.share.usecase.CreateShareUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchCopyUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchDeleteUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchMoveUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchStarUseCase
import com.vaultstadio.app.domain.storage.usecase.CreateFolderUseCase
import com.vaultstadio.app.domain.storage.usecase.DeleteItemUseCase
import com.vaultstadio.app.domain.storage.usecase.EmptyTrashUseCase
import com.vaultstadio.app.domain.storage.usecase.GetBreadcrumbsUseCase
import com.vaultstadio.app.domain.storage.usecase.GetFolderItemsUseCase
import com.vaultstadio.app.domain.storage.usecase.GetRecentUseCase
import com.vaultstadio.app.domain.storage.usecase.GetStarredUseCase
import com.vaultstadio.app.domain.storage.usecase.GetTrashUseCase
import com.vaultstadio.app.domain.storage.usecase.RenameItemUseCase
import com.vaultstadio.app.domain.storage.usecase.RestoreItemUseCase
import com.vaultstadio.app.domain.storage.usecase.SearchUseCase
import com.vaultstadio.app.domain.storage.usecase.ToggleStarUseCase
import com.vaultstadio.app.domain.storage.usecase.TrashItemUseCase
import com.vaultstadio.app.feature.files.FilesMode
import com.vaultstadio.app.feature.files.FilesViewModel
import com.vaultstadio.app.feature.files.FilesViewPreferences
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureFilesModule = module {
    viewModel { (mode: FilesMode) ->
        FilesViewModel(
            get<FilesViewPreferences>(),
            get<GetFolderItemsUseCase>(),
            get<GetRecentUseCase>(),
            get<GetStarredUseCase>(),
            get<GetTrashUseCase>(),
            get<GetBreadcrumbsUseCase>(),
            get<CreateFolderUseCase>(),
            get<RenameItemUseCase>(),
            get<ToggleStarUseCase>(),
            get<TrashItemUseCase>(),
            get<RestoreItemUseCase>(),
            get<DeleteItemUseCase>(),
            get<EmptyTrashUseCase>(),
            get<BatchDeleteUseCase>(),
            get<BatchMoveUseCase>(),
            get<BatchCopyUseCase>(),
            get<BatchStarUseCase>(),
            get<SearchUseCase>(),
            get<GetSearchSuggestionsUseCase>(),
            get<GetItemActivityUseCase>(),
            get<CreateShareUseCase>(),
            get<GetStorageUrlsUseCase>(),
            get<GetShareUrlUseCase>(),
            mode,
        )
    }
}
