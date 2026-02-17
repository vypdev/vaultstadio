/**
 * VaultStadio iOS Platform Storage
 *
 * In-memory implementation for now. Production should use NSUserDefaults
 * via platform interop.
 */

package com.vaultstadio.app.platform

/**
 * iOS implementation using in-memory map.
 * TODO: Replace with NSUserDefaults via Kotlin/Native interop.
 */
actual object PlatformStorage {
    private val map = mutableMapOf<String, String>()

    actual fun setString(key: String, value: String) {
        map[key] = value
    }

    actual fun getString(key: String): String? = map[key]

    actual fun remove(key: String) {
        map.remove(key)
    }

    actual fun clear() {
        map.clear()
    }
}
