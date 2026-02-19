/**
 * Config Repository
 *
 * Provides access to application configuration values.
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiClientConfig
import org.koin.core.annotation.Single

/**
 * Repository interface for accessing application configuration.
 */
interface ConfigRepository {
    /**
     * Returns the base URL for API calls.
     */
    fun getApiBaseUrl(): String
}

@Single(binds = [ConfigRepository::class])
class ConfigRepositoryImpl(
    private val apiClientConfig: ApiClientConfig,
) : ConfigRepository {
    override fun getApiBaseUrl(): String = apiClientConfig.baseUrl
}
