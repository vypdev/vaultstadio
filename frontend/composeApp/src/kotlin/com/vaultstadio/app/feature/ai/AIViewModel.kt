package com.vaultstadio.app.feature.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.ai.usecase.AIChatUseCase
import com.vaultstadio.app.domain.ai.usecase.ClassifyContentUseCase
import com.vaultstadio.app.domain.ai.usecase.DeleteAIProviderUseCase
import com.vaultstadio.app.domain.ai.usecase.DescribeImageUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProviderStatusUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProvidersUseCase
import com.vaultstadio.app.domain.ai.usecase.GetProviderModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.SummarizeTextUseCase
import com.vaultstadio.app.domain.ai.usecase.TagImageUseCase
import kotlinx.coroutines.launch
/**
 * ViewModel for AI features.
 */
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
                is Result.Success -> {
                    providers = result.data
                    activeProvider = result.data.find { it.isActive }
                    if (activeProvider != null) {
                        loadModels()
                    }
                }
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun loadModels() {
        viewModelScope.launch {
            error = null
            when (val result = getAIModelsUseCase()) {
                is Result.Success -> models = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun chat(messages: List<AIChatMessage>, modelId: String?, callback: (String?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = aiChatUseCase(messages, modelId)) {
                is Result.Success -> callback(result.data.content)
                is Result.Error -> {
                    error = result.message
                    callback(null)
                }
                is Result.NetworkError -> {
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
                is Result.Success -> callback(result.data)
                is Result.Error -> {
                    error = result.message
                    callback(null)
                }
                is Result.NetworkError -> {
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
                is Result.Success -> callback(result.data)
                is Result.Error -> {
                    error = result.message
                    callback(null)
                }
                is Result.NetworkError -> {
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
                is Result.Success -> callback(result.data)
                is Result.Error -> {
                    error = result.message
                    callback(null)
                }
                is Result.NetworkError -> {
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
                is Result.Success -> callback(result.data)
                is Result.Error -> {
                    error = result.message
                    callback(null)
                }
                is Result.NetworkError -> {
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
                is Result.Success -> loadProviders()
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

    fun checkProviderStatus(type: AIProviderType) {
        viewModelScope.launch {
            error = null
            when (val result = getProviderStatusUseCase(type)) {
                is Result.Success -> providerStatus = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun loadProviderModels(type: AIProviderType) {
        viewModelScope.launch {
            error = null
            when (val result = getProviderModelsUseCase(type)) {
                is Result.Success -> selectedProviderModels = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
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
