/**
 * Repository interface for accessing application configuration.
 */

package com.vaultstadio.app.domain.config

/**
 * Provides access to application configuration values (e.g. API base URL).
 */
interface ConfigRepository {
    /**
     * Returns the base URL for API calls.
     */
    fun getApiBaseUrl(): String
}
