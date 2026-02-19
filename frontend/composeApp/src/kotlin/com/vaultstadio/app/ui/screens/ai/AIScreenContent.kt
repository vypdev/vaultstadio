/**
 * AI Screen content composables: mode selector, provider info, content area, input, providers sheet.
 * Extracted from AIScreen.kt to keep the main screen file under the line limit.
 */

package com.vaultstadio.app.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.AIChatMessage
import com.vaultstadio.app.domain.model.AIModel
import com.vaultstadio.app.domain.model.AIProviderInfo
import com.vaultstadio.app.domain.model.AIProviderType
import com.vaultstadio.app.domain.model.ChatRole
import com.vaultstadio.app.i18n.StringResources

@Composable
fun AIModeSelector(
    selectedMode: AIMode,
    onModeSelected: (AIMode) -> Unit,
    strings: StringResources,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedMode == AIMode.CHAT,
            onClick = { onModeSelected(AIMode.CHAT) },
            label = { Text(strings.aiChat) },
            leadingIcon = { Icon(Icons.Default.SmartToy, null, Modifier.size(18.dp)) },
        )
        FilterChip(
            selected = selectedMode == AIMode.DESCRIBE,
            onClick = { onModeSelected(AIMode.DESCRIBE) },
            label = { Text(strings.aiDescribe) },
            leadingIcon = { Icon(Icons.Default.Image, null, Modifier.size(18.dp)) },
        )
        FilterChip(
            selected = selectedMode == AIMode.TAG,
            onClick = { onModeSelected(AIMode.TAG) },
            label = { Text(strings.aiTag) },
            leadingIcon = { Icon(Icons.Default.Tag, null, Modifier.size(18.dp)) },
        )
        FilterChip(
            selected = selectedMode == AIMode.SUMMARIZE,
            onClick = { onModeSelected(AIMode.SUMMARIZE) },
            label = { Text(strings.aiSummarize) },
            leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)) },
        )
    }
}

@Composable
fun ActiveProviderInfo(
    activeProvider: AIProviderInfo,
    models: List<AIModel>,
    selectedModel: AIModel?,
    showModelSelector: Boolean,
    onShowModelSelector: () -> Unit,
    onHideModelSelector: () -> Unit,
    onModelSelected: (AIModel) -> Unit,
    strings: StringResources,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "${strings.aiProvider}: ${activeProvider.type.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (models.isNotEmpty()) {
            Spacer(Modifier.width(16.dp))
            Box {
                OutlinedButton(
                    onClick = onShowModelSelector,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        selectedModel?.name ?: strings.aiSelectModel,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                DropdownMenu(
                    expanded = showModelSelector,
                    onDismissRequest = onHideModelSelector,
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.name) },
                            onClick = {
                                onModelSelected(model)
                                onHideModelSelector()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoProviderCard(
    isAdmin: Boolean,
    onConfigureProvider: () -> Unit,
    strings: StringResources,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                strings.aiNoProviderConfigured,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onConfigureProvider) {
                    Text(strings.aiConfigureProvider)
                }
            }
        }
    }
}

@Composable
fun AIContentArea(
    selectedMode: AIMode,
    chatMessages: List<ChatMessage>,
    listState: LazyListState,
    descriptionResult: String?,
    tagsResult: List<String>?,
    summaryResult: String?,
    onClearDescription: () -> Unit,
    onClearTags: () -> Unit,
    onClearSummary: () -> Unit,
    strings: StringResources,
    modifier: Modifier = Modifier,
) {
    when (selectedMode) {
        AIMode.CHAT -> {
            LazyColumn(
                state = listState,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        EmptyAIState(
                            icon = Icons.Default.SmartToy,
                            title = strings.aiStartConversation,
                            description = strings.aiStartConversationDesc,
                        )
                    }
                }
                items(chatMessages) { message ->
                    ChatBubble(message = message)
                }
            }
        }
        AIMode.DESCRIBE, AIMode.TAG, AIMode.CLASSIFY, AIMode.SUMMARIZE -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                when (selectedMode) {
                    AIMode.DESCRIBE -> {
                        if (descriptionResult != null) {
                            ResultCard(
                                title = strings.aiDescribeImage,
                                content = descriptionResult,
                                onClear = onClearDescription,
                            )
                        } else {
                            EmptyAIState(
                                icon = Icons.Default.Image,
                                title = strings.aiDescribeImage,
                                description = strings.aiDescribeImageDesc,
                            )
                        }
                    }
                    AIMode.TAG -> {
                        if (tagsResult != null) {
                            TagsResultCard(
                                tags = tagsResult,
                                onClear = onClearTags,
                                strings = strings,
                            )
                        } else {
                            EmptyAIState(
                                icon = Icons.Default.Tag,
                                title = strings.aiAutoTagContent,
                                description = strings.aiAutoTagContentDesc,
                            )
                        }
                    }
                    AIMode.SUMMARIZE -> {
                        if (summaryResult != null) {
                            ResultCard(
                                title = strings.aiSummarize,
                                content = summaryResult,
                                onClear = onClearSummary,
                            )
                        } else {
                            EmptyAIState(
                                icon = Icons.Default.AutoAwesome,
                                title = strings.aiSummarizeText,
                                description = strings.aiSummarizeTextDesc,
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun AIInputArea(
    inputText: String,
    selectedMode: AIMode,
    isSending: Boolean,
    activeProvider: AIProviderInfo?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    strings: StringResources,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        when (selectedMode) {
                            AIMode.CHAT -> strings.aiTypePlaceholder
                            AIMode.DESCRIBE -> strings.aiImageUrlPlaceholder
                            AIMode.TAG -> strings.aiTagPlaceholder
                            AIMode.CLASSIFY -> strings.aiClassifyPlaceholder
                            AIMode.SUMMARIZE -> strings.aiSummarizePlaceholder
                        },
                    )
                },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isSending && activeProvider != null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isSending && activeProvider != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = strings.commonSend,
                        tint = if (inputText.isNotBlank() && activeProvider != null) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ProvidersSheetContent(
    providers: List<AIProviderInfo>,
    activeProvider: AIProviderInfo?,
    selectedProviderForModels: AIProviderInfo?,
    providerStatus: Map<String, Boolean>,
    selectedProviderModels: List<AIModel>,
    isAdmin: Boolean,
    onSelectProvider: (AIProviderInfo) -> Unit,
    onCheckStatus: (AIProviderType) -> Unit,
    onLoadModels: (AIProviderType) -> Unit,
    onDelete: (AIProviderInfo) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            "AI Providers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        providers.forEach { provider ->
            ProviderCard(
                provider = provider,
                isActive = provider.type == activeProvider?.type,
                providerStatus = if (selectedProviderForModels?.type == provider.type) {
                    providerStatus
                } else {
                    emptyMap()
                },
                providerModels = if (selectedProviderForModels?.type == provider.type) {
                    selectedProviderModels
                } else {
                    emptyList()
                },
                onCheckStatus = {
                    onSelectProvider(provider)
                    onCheckStatus(provider.type)
                },
                onLoadModels = {
                    onSelectProvider(provider)
                    onLoadModels(provider.type)
                },
                onDelete = { onDelete(provider) },
                isAdmin = isAdmin,
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(32.dp))
    }
}

fun handleAISend(
    selectedMode: AIMode,
    inputText: String,
    chatMessages: MutableList<ChatMessage>,
    selectedModel: AIModel?,
    onChat: (List<AIChatMessage>, String?, (String?) -> Unit) -> Unit,
    onSummarize: (String, Int, (String?) -> Unit) -> Unit,
    onInputClear: () -> Unit,
    onComplete: () -> Unit,
    onSummaryResult: (String?) -> Unit,
    strings: StringResources,
) {
    when (selectedMode) {
        AIMode.CHAT -> {
            val userMessage = ChatMessage(ChatRole.USER, inputText)
            chatMessages.add(userMessage)
            chatMessages.add(ChatMessage(ChatRole.ASSISTANT, "", isLoading = true))
            val messages = chatMessages
                .filter { !it.isLoading }
                .map { AIChatMessage(it.role, it.content) }
            onInputClear()
            onChat(messages, selectedModel?.id) { response ->
                chatMessages.removeAt(chatMessages.size - 1)
                if (response != null) {
                    chatMessages.add(ChatMessage(ChatRole.ASSISTANT, response))
                } else {
                    chatMessages.add(ChatMessage(ChatRole.ASSISTANT, strings.aiCouldNotProcess))
                }
                onComplete()
            }
        }
        AIMode.SUMMARIZE -> {
            val textToSummarize = inputText
            onInputClear()
            onSummarize(textToSummarize, 200) { result ->
                onSummaryResult(result)
                onComplete()
            }
        }
        else -> {
            onComplete()
        }
    }
}
