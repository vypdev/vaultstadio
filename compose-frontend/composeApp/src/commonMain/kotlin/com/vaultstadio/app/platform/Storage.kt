/**
 * VaultStadio Platform Storage
 *
 * Expect/actual declarations for platform-specific persistent storage.
 */

package com.vaultstadio.app.platform

/**
 * Persistent key-value storage interface.
 */
expect object PlatformStorage {
    /**
     * Save a string value.
     */
    fun setString(key: String, value: String)

    /**
     * Get a string value.
     */
    fun getString(key: String): String?

    /**
     * Remove a value.
     */
    fun remove(key: String)

    /**
     * Clear all stored values.
     */
    fun clear()
}

/**
 * Storage keys for the application.
 */
object StorageKeys {
    const val AUTH_TOKEN = "auth_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val THEME_MODE = "theme_mode"
    const val LANGUAGE = "language"
    const val VIEW_MODE = "view_mode"
    const val FILES_SORT_FIELD = "files_sort_field"
    const val FILES_SORT_ORDER = "files_sort_order"
}
