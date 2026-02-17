/**
 * VaultStadio WASM Platform Storage
 *
 * Uses browser localStorage for persistent storage.
 */

package com.vaultstadio.app.platform

import kotlinx.browser.window

/**
 * WASM implementation using localStorage.
 */
actual object PlatformStorage {
    private val storage = window.localStorage

    /**
     * Save a string value to localStorage.
     */
    actual fun setString(key: String, value: String) {
        storage.setItem(key, value)
    }

    /**
     * Get a string value from localStorage.
     */
    actual fun getString(key: String): String? {
        return storage.getItem(key)
    }

    /**
     * Remove a value from localStorage.
     */
    actual fun remove(key: String) {
        storage.removeItem(key)
    }

    /**
     * Clear all stored values.
     */
    actual fun clear() {
        storage.clear()
    }
}
