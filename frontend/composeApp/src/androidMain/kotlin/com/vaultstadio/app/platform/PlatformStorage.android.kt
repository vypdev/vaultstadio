/**
 * VaultStadio Android Platform Storage Implementation
 *
 * Uses SharedPreferences for local storage.
 */

package com.vaultstadio.app.platform

/**
 * Platform-specific local storage.
 * Note: Android implementation would use SharedPreferences
 * injected via dependency injection.
 */
actual object PlatformStorage {
    private val storage = mutableMapOf<String, String>()

    actual fun getString(key: String): String? = storage[key]

    actual fun setString(key: String, value: String) {
        storage[key] = value
    }

    actual fun remove(key: String) {
        storage.remove(key)
    }

    actual fun clear() {
        storage.clear()
    }
}
