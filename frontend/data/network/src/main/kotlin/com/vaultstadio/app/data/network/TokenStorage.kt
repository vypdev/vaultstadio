/**
 * Token storage interface for authentication tokens.
 * Placed in data:network so all modules (auth, composeApp) can use it without depending on data:auth.
 */

package com.vaultstadio.app.data.network

interface TokenStorage {
    fun getAccessToken(): String?
    fun setAccessToken(token: String?)
    fun getRefreshToken(): String?
    fun setRefreshToken(token: String?)
    fun clear()
}
