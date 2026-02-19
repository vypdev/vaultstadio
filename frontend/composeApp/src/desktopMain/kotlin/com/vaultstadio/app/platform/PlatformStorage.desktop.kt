/**
 * VaultStadio Desktop Platform Storage
 *
 * Uses Java Preferences API for persistent storage.
 */

package com.vaultstadio.app.platform

import java.util.prefs.Preferences

/**
 * Desktop implementation using Java Preferences.
 */
actual object PlatformStorage {
    private val prefs: Preferences = Preferences.userNodeForPackage(PlatformStorage::class.java)

    /**
     * Save a string value.
     */
    actual fun setString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    /**
     * Get a string value.
     */
    actual fun getString(key: String): String? {
        return prefs.get(key, null)
    }

    /**
     * Remove a value.
     */
    actual fun remove(key: String) {
        prefs.remove(key)
        prefs.flush()
    }

    /**
     * Clear all stored values.
     */
    actual fun clear() {
        prefs.keys().forEach { prefs.remove(it) }
        prefs.flush()
    }
}
