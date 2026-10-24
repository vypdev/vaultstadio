package com.vaultstadio.app.feature.files

/**
 * Persistence for files screen view preferences (view mode, sort field, sort order).
 * Implemented by the app/platform layer; the feature module does not depend on platform storage.
 */
interface FilesViewPreferences {
    fun getViewMode(): String?
    fun setViewMode(value: String)
    fun getSortField(): String?
    fun setSortField(value: String)
    fun getSortOrder(): String?
    fun setSortOrder(value: String)
}
