/**
 * Token Storage Interface
 *
 * Abstraction for storing authentication tokens.
 */

package com.vaultstadio.app.data.repository

/**
 * Token storage interface for managing authentication tokens.
 */
interface TokenStorage {
    /**
     * Gets the current access token.
     */
    fun getAccessToken(): String?

    /**
     * Sets the access token.
     */
    fun setAccessToken(token: String?)

    /**
     * Gets the current refresh token.
     */
    fun getRefreshToken(): String?

    /**
     * Sets the refresh token.
     */
    fun setRefreshToken(token: String?)

    /**
     * Clears all stored tokens.
     */
    fun clear()
}
