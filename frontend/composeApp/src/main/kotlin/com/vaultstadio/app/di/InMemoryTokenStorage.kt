/**
 * In-memory token storage (e.g. for desktop/dev).
 * Override with PersistentTokenStorage on platforms that need persistence.
 */

package com.vaultstadio.app.di

import com.vaultstadio.app.data.network.TokenStorage

class InMemoryTokenStorage : TokenStorage {

    private var accessToken: String? = null
    private var refreshToken: String? = null

    override fun getAccessToken(): String? = accessToken
    override fun setAccessToken(token: String?) { accessToken = token }
    override fun getRefreshToken(): String? = refreshToken
    override fun setRefreshToken(token: String?) { refreshToken = token }
    override fun clear() {
        accessToken = null
        refreshToken = null
    }
}
