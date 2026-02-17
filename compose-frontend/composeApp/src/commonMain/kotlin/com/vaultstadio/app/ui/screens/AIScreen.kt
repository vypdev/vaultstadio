/**
 * VaultStadio AI Screen
 *
 * Screen for AI features including chat, image analysis, tagging, and classification.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vaultstadio.app.domain.model.AIChatMessage
import com.vaultstadio.app.domain.model.AIModel
import com.vaultstadio.app.domain.model.AIProviderInfo
import com.vaultstadio.app.domain.model.AIProviderType
import com.vaultstadio.app.i18n.strings
import com.vaultstadio.app.ui.screens.ai.AIContentArea
import com.vaultstadio.app.ui.screens.ai.AIErrorDialog
import com.vaultstadio.app.ui.screens.ai.AIInputArea
import com.vaultstadio.app.ui.screens.ai.AIMode
import com.vaultstadio.app.ui.screens.ai.AIModeSelector
import com.vaultstadio.app.ui.screens.ai.ActiveProviderInfo
import com.vaultstadio.app.ui.screens.ai.ChatMessage
import com.vaultstadio.app.ui.screens.ai.DeleteProviderDialog
import com.vaultstadio.app.ui.screens.ai.NoProviderCard
import com.vaultstadio.app.ui.screens.ai.ProviderConfigDialog
import com.vaultstadio.app.ui.screens.ai.ProvidersSheetContent
import com.vaultstadio.app.ui.screens.ai.handleAISend
import com.vaultstadio.app.ui.theme.VaultStadioPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    providers: List<AIProviderInfo>,
    models: List<AIModel>,
    selectedProviderModels: List<AIModel>,
    providerStatus: Map<String, Boolean>,
    showProviderConfig: Boolean,
    activeProvider: AIProviderInfo?,
    isAdmin: Boolean,
    isLoading: Boolean,
    error: String?,
    onLoadProviders: () -> Unit,
    onLoadModels: () -> Unit,
    onLoadProviderModels: (AIProviderType) -> Unit,
    onCheckProviderStatus: (AIProviderType) -> Unit,
    onDeleteProvider: (AIProviderType) -> Unit,
    onChat: (List<AIChatMessage>, String?, (String?) -> Unit) -> Unit,
    onDescribe: (String, String, (String?) -> Unit) -> Unit,
    onTag: (String, String, (List<String>?) -> Unit) -> Unit,
    onClassify: (String, List<String>, (String?) -> Unit) -> Unit,
    onSummarize: (String, Int, (String?) -> Unit) -> Unit,
    onConfigureProvider: () -> Unit,
    onHideProviderConfig: () -> Unit,
    onClearProviderModels: () -> Unit,
    onClearProviderStatus: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = strings()

    var selectedMode by remember { mutableStateOf(AIMode.CHAT) }
    var inputText by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf<AIModel?>(null) }
    var showModelSelector by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    // Provider management state
    var showProvidersSheet by remember { mutableStateOf(false) }
    var providerToDelete by remember { mutableStateOf<AIProviderInfo?>(null) }
    var selectedProviderForModels by remember { mutableStateOf<AIProviderInfo?>(null) }
    val providersSheetState = rememberModalBottomSheetState()

    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    // Result states for non-chat modes
    var descriptionResult by remember { mutableStateOf<String?>(null) }
    var tagsResult by remember { mutableStateOf<List<String>?>(null) }
    var summaryResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onLoadProviders()
        onLoadModels()
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.aiAssistant) },
                actions = {
                    if (providers.isNotEmpty()) {
                        IconButton(onClick = { showProvidersSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Providers")
                        }
                    }
                    if (isAdmin) {
                        IconButton(onClick = onConfigureProvider) {
                            Icon(Icons.Default.Settings, contentDescription = strings.commonConfigure)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Mode selector
            AIModeSelector(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it },
                strings = strings,
            )

            // Provider info
            if (activeProvider != null) {
                ActiveProviderInfo(
                    activeProvider = activeProvider,
                    models = models,
                    selectedModel = selectedModel,
                    showModelSelector = showModelSelector,
                    onShowModelSelector = { showModelSelector = true },
                    onHideModelSelector = { showModelSelector = false },
                    onModelSelected = { selectedModel = it },
                    strings = strings,
                )
            } else if (!isLoading) {
                NoProviderCard(
                    isAdmin = isAdmin,
                    onConfigureProvider = onConfigureProvider,
                    strings = strings,
                )
            }

            // Main content area
            AIContentArea(
                selectedMode = selectedMode,
                chatMessages = chatMessages,
                listState = listState,
                descriptionResult = descriptionResult,
                tagsResult = tagsResult,
                summaryResult = summaryResult,
                onClearDescription = { descriptionResult = null },
                onClearTags = { tagsResult = null },
                onClearSummary = { summaryResult = null },
                strings = strings,
                modifier = Modifier.weight(1f),
            )

            // Input area
            AIInputArea(
                inputText = inputText,
                selectedMode = selectedMode,
                isSending = isSending,
                activeProvider = activeProvider,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() && !isSending) {
                        isSending = true
                        handleAISend(
                            selectedMode = selectedMode,
                            inputText = inputText,
                            chatMessages = chatMessages,
                            selectedModel = selectedModel,
                            onChat = onChat,
                            onSummarize = onSummarize,
                            onInputClear = { inputText = "" },
                            onComplete = { isSending = false },
                            onSummaryResult = { summaryResult = it },
                            strings = strings,
                        )
                    }
                },
                strings = strings,
            )
        }
    }

    // Providers Bottom Sheet
    if (showProvidersSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showProvidersSheet = false
                onClearProviderModels()
                onClearProviderStatus()
            },
            sheetState = providersSheetState,
        ) {
            ProvidersSheetContent(
                providers = providers,
                activeProvider = activeProvider,
                selectedProviderForModels = selectedProviderForModels,
                providerStatus = providerStatus,
                selectedProviderModels = selectedProviderModels,
                isAdmin = isAdmin,
                onSelectProvider = { selectedProviderForModels = it },
                onCheckStatus = onCheckProviderStatus,
                onLoadModels = onLoadProviderModels,
                onDelete = { providerToDelete = it },
            )
        }
    }

    // Delete Provider Confirmation Dialog
    providerToDelete?.let { provider ->
        DeleteProviderDialog(
            provider = provider,
            onConfirm = {
                onDeleteProvider(provider.type)
                providerToDelete = null
            },
            onDismiss = { providerToDelete = null },
        )
    }

    // Provider configuration dialog
    if (showProviderConfig) {
        ProviderConfigDialog(
            providersCount = providers.size,
            activeProviderName = activeProvider?.type?.name,
            onDismiss = onHideProviderConfig,
        )
    }

    // Error dialog
    error?.let { errorMessage ->
        AIErrorDialog(
            message = errorMessage,
            onDismiss = onClearError,
        )
    }
}

// region Previews

@Composable
internal fun AIScreenPreview() {
    VaultStadioPreview {
        AIScreen(
            providers = emptyList(),
            models = emptyList(),
            selectedProviderModels = emptyList(),
            providerStatus = emptyMap(),
            showProviderConfig = false,
            activeProvider = null,
            isAdmin = true,
            isLoading = false,
            error = null,
            onLoadProviders = {},
            onLoadModels = {},
            onLoadProviderModels = {},
            onCheckProviderStatus = {},
            onDeleteProvider = {},
            onChat = { _, _, _ -> },
            onDescribe = { _, _, _ -> },
            onTag = { _, _, _ -> },
            onClassify = { _, _, _ -> },
            onSummarize = { _, _, _ -> },
            onConfigureProvider = {},
            onHideProviderConfig = {},
            onClearProviderModels = {},
            onClearProviderStatus = {},
            onClearError = {},
        )
    }
}

// endregion
