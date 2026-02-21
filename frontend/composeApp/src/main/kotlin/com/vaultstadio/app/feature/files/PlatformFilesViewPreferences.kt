package com.vaultstadio.app.feature.files

import com.vaultstadio.app.platform.PlatformStorage
import com.vaultstadio.app.platform.StorageKeys

/**
 * Platform-backed implementation of [FilesViewPreferences] for the files screen.
 * Registered in appModule; :feature:files only depends on the interface.
 */
class PlatformFilesViewPreferences : FilesViewPreferences {
    override fun getViewMode(): String? = PlatformStorage.getString(StorageKeys.VIEW_MODE)
    override fun setViewMode(value: String) = PlatformStorage.setString(StorageKeys.VIEW_MODE, value)
    override fun getSortField(): String? = PlatformStorage.getString(StorageKeys.FILES_SORT_FIELD)
    override fun setSortField(value: String) = PlatformStorage.setString(StorageKeys.FILES_SORT_FIELD, value)
    override fun getSortOrder(): String? = PlatformStorage.getString(StorageKeys.FILES_SORT_ORDER)
    override fun setSortOrder(value: String) = PlatformStorage.setString(StorageKeys.FILES_SORT_ORDER, value)
}
