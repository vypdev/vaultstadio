/**
 * VaultStadio Persistent Token Storage
 *
 * Token storage implementation that persists tokens using PlatformStorage.
 */

package com.vaultstadio.app.platform

import com.vaultstadio.app.data.network.TokenStorage

/**
 * Persistent token storage using platform-specific storage.
 */
class PersistentTokenStorage : TokenStorage {

    override fun getAccessToken(): String? {
        return PlatformStorage.getString(StorageKeys.AUTH_TOKEN)
    }

    override fun setAccessToken(token: String?) {
        if (token != null) {
            PlatformStorage.setString(StorageKeys.AUTH_TOKEN, token)
        } else {
            PlatformStorage.remove(StorageKeys.AUTH_TOKEN)
        }
    }

    override fun getRefreshToken(): String? {
        return PlatformStorage.getString(StorageKeys.REFRESH_TOKEN)
    }

    override fun setRefreshToken(token: String?) {
        if (token != null) {
            PlatformStorage.setString(StorageKeys.REFRESH_TOKEN, token)
        } else {
            PlatformStorage.remove(StorageKeys.REFRESH_TOKEN)
        }
    }

    override fun clear() {
        PlatformStorage.remove(StorageKeys.AUTH_TOKEN)
        PlatformStorage.remove(StorageKeys.REFRESH_TOKEN)
    }
}
