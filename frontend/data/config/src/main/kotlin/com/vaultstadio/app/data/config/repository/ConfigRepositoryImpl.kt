/**
 * Config repository implementation using ApiClientConfig.
 */

package com.vaultstadio.app.data.config.repository

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.domain.config.ConfigRepository

class ConfigRepositoryImpl(
    private val apiClientConfig: ApiClientConfig,
) : ConfigRepository {
    override fun getApiBaseUrl(): String = apiClientConfig.baseUrl
}
