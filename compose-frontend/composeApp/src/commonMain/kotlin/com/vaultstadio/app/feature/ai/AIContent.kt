package com.vaultstadio.app.feature.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.AIScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * AI feature content - delegates to AIScreen with ViewModel data.
 */
@Composable
fun AIContent(
    component: AIComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: AIViewModel = koinViewModel()

    AIScreen(
        providers = viewModel.providers,
        models = viewModel.models,
        selectedProviderModels = viewModel.selectedProviderModels,
        providerStatus = viewModel.providerStatus,
        showProviderConfig = viewModel.showProviderConfig,
        activeProvider = viewModel.activeProvider,
        isAdmin = component.isAdmin,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onLoadProviders = viewModel::loadProviders,
        onLoadModels = viewModel::loadModels,
        onLoadProviderModels = viewModel::loadProviderModels,
        onCheckProviderStatus = viewModel::checkProviderStatus,
        onDeleteProvider = viewModel::deleteProvider,
        onChat = { messages, modelId, callback ->
            viewModel.chat(messages, modelId, callback)
        },
        onDescribe = { imageBase64, mimeType, callback ->
            viewModel.describeImage(imageBase64, mimeType, callback)
        },
        onTag = { imageBase64, mimeType, callback ->
            viewModel.tagImage(imageBase64, mimeType, callback)
        },
        onClassify = { content, categories, callback ->
            viewModel.classifyContent(content, categories, callback)
        },
        onSummarize = { text, maxLength, callback ->
            viewModel.summarize(text, maxLength, callback)
        },
        onConfigureProvider = viewModel::configureProvider,
        onHideProviderConfig = viewModel::hideProviderConfig,
        onClearProviderModels = viewModel::clearProviderModels,
        onClearProviderStatus = viewModel::clearProviderStatus,
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}
