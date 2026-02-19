package com.vaultstadio.app.feature.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.AIChatMessage
import com.vaultstadio.app.domain.model.AIModel
import com.vaultstadio.app.domain.model.AIProviderInfo
import com.vaultstadio.app.domain.model.AIProviderType
import com.vaultstadio.app.domain.usecase.ai.AIChatUseCase
import com.vaultstadio.app.domain.usecase.ai.ClassifyContentUseCase
import com.vaultstadio.app.domain.usecase.ai.DeleteAIProviderUseCase
import com.vaultstadio.app.domain.usecase.ai.DescribeImageUseCase
import com.vaultstadio.app.domain.usecase.ai.GetAIModelsUseCase
import com.vaultstadio.app.domain.usecase.ai.GetAIProviderStatusUseCase
import com.vaultstadio.app.domain.usecase.ai.GetAIProvidersUseCase
import com.vaultstadio.app.domain.usecase.ai.GetProviderModelsUseCase
import com.vaultstadio.app.domain.usecase.ai.SummarizeTextUseCase
import com.vaultstadio.app.domain.usecase.ai.TagImageUseCase
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for AI features.
 */
@KoinViewModel
class AIViewModel(
    private val getAIProvidersUseCase: GetAIProvidersUseCase,
    private val getAIModelsUseCase: GetAIModelsUseCase,
    private val getProviderModelsUseCase: GetProviderModelsUseCase,
    private val getProviderStatusUseCase: GetAIProviderStatusUseCase,
    private val deleteAIProviderUseCase: DeleteAIProviderUseCase,
    private val aiChatUseCase: AIChatUseCase,
    private val describeImageUseCase: DescribeImageUseCase,
    private val tagImageUseCase: TagImageUseCase,
    private val classifyContentUseCase: ClassifyContentUseCase,
    private val summarizeTextUseCase: SummarizeTextUseCase,
) : ViewModel() {

    var providers by mutableStateOf<List<AIProviderInfo>>(emptyList())
        private set
    var models by mutableStateOf<List<AIModel>>(emptyList())
        private set
    var activeProvider by mutableStateOf<AIProviderInfo?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var showProviderConfig by mutableStateOf(false)
        private set
    var providerStatus by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set
    var selectedProviderModels by mutableStateOf<List<AIModel>>(emptyList())
        private set

    init {
        loadProviders()
    }

    fun loadProviders() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = getAIProvidersUseCase()) {
                is ApiResult.Success -> {
                    providers = result.data
                    activeProvider = result.data.find { it.isActive }
                    if (activeProvider != null) {
                        loadModels()
                    }
                }
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun loadModels() {
        viewModelScope.launch {
            error = null
            when (val result = getAIModelsUseCase()) {
                is ApiResult.Success -> models = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun chat(messages: List<AIChatMessage>, modelId: String?, callback: (String?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = aiChatUseCase(messages, modelId)) {
                is ApiResult.Success -> callback(result.data.content)
                is ApiResult.Error -> {
                    error = result.message
                    callback(null)
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    callback(null)
                }
            }
            isLoading = false
        }
    }

    fun describeImage(imageBase64: String, mimeType: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = describeImageUseCase(imageBase64, mimeType)) {
                is ApiResult.Success -> callback(result.data)
                is ApiResult.Error -> {
                    error = result.message
                    callback(null)
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    callback(null)
                }
            }
            isLoading = false
        }
    }

    fun tagImage(imageBase64: String, mimeType: String, callback: (List<String>?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = tagImageUseCase(imageBase64, mimeType)) {
                is ApiResult.Success -> callback(result.data)
                is ApiResult.Error -> {
                    error = result.message
                    callback(null)
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    callback(null)
                }
            }
            isLoading = false
        }
    }

    fun classifyContent(content: String, categories: List<String>, callback: (String?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = classifyContentUseCase(content, categories)) {
                is ApiResult.Success -> callback(result.data)
                is ApiResult.Error -> {
                    error = result.message
                    callback(null)
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    callback(null)
                }
            }
            isLoading = false
        }
    }

    fun summarize(text: String, maxLength: Int, callback: (String?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = summarizeTextUseCase(text, maxLength)) {
                is ApiResult.Success -> callback(result.data)
                is ApiResult.Error -> {
                    error = result.message
                    callback(null)
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    callback(null)
                }
            }
            isLoading = false
        }
    }

    fun configureProvider() {
        showProviderConfig = true
    }

    fun hideProviderConfig() {
        showProviderConfig = false
    }

    fun deleteProvider(type: AIProviderType) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = deleteAIProviderUseCase(type)) {
                is ApiResult.Success -> loadProviders()
                is ApiResult.Error -> {
                    error = result.message
                    isLoading = false
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun checkProviderStatus(type: AIProviderType) {
        viewModelScope.launch {
            error = null
            when (val result = getProviderStatusUseCase(type)) {
                is ApiResult.Success -> providerStatus = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun loadProviderModels(type: AIProviderType) {
        viewModelScope.launch {
            error = null
            when (val result = getProviderModelsUseCase(type)) {
                is ApiResult.Success -> selectedProviderModels = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun clearProviderStatus() {
        providerStatus = emptyMap()
    }

    fun clearProviderModels() {
        selectedProviderModels = emptyList()
    }

    fun clearError() {
        error = null
    }
}
