/**
 * In-Memory Token Storage Implementation
 *
 * Simple in-memory implementation of TokenStorage.
 */

package com.vaultstadio.app.data.repository

/**
 * In-memory token storage implementation.
 *
 * Note: This implementation loses tokens when the app is closed.
 * For production use, consider implementing a persistent token storage.
 */
class InMemoryTokenStorage : TokenStorage {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    override fun getAccessToken(): String? = accessToken

    override fun setAccessToken(token: String?) {
        accessToken = token
    }

    override fun getRefreshToken(): String? = refreshToken

    override fun setRefreshToken(token: String?) {
        refreshToken = token
    }

    override fun clear() {
        accessToken = null
        refreshToken = null
    }
}
