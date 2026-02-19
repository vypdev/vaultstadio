package com.vaultstadio.app.feature.plugins

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.plugin.model.PluginInfo
import com.vaultstadio.app.domain.plugin.usecase.DisablePluginUseCase
import com.vaultstadio.app.domain.plugin.usecase.EnablePluginUseCase
import com.vaultstadio.app.domain.plugin.usecase.GetPluginsUseCase
import kotlinx.coroutines.launch
/**
 * ViewModel for plugin management.
 */
class PluginsViewModel(
    private val getPluginsUseCase: GetPluginsUseCase,
    private val enablePluginUseCase: EnablePluginUseCase,
    private val disablePluginUseCase: DisablePluginUseCase,
) : ViewModel() {

    var plugins by mutableStateOf<List<PluginInfo>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadPlugins()
    }

    fun loadPlugins() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = getPluginsUseCase()) {
                is Result.Success -> plugins = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun enablePlugin(pluginId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = enablePluginUseCase(pluginId)) {
                is Result.Success -> loadPlugins()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun disablePlugin(pluginId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = disablePluginUseCase(pluginId)) {
                is Result.Success -> loadPlugins()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun clearError() {
        error = null
    }
}
