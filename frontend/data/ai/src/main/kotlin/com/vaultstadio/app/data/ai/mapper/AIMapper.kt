/**
 * AI Mappers
 */

package com.vaultstadio.app.data.ai.mapper

import com.vaultstadio.app.data.ai.dto.AIChatMessageDTO
import com.vaultstadio.app.data.ai.dto.AIChatResponseDTO
import com.vaultstadio.app.data.ai.dto.AIModelDTO
import com.vaultstadio.app.data.ai.dto.AIProviderInfoDTO
import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIChatResponse
import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.ai.model.AIProviderType

fun AIProviderInfoDTO.toDomain(): AIProviderInfo = AIProviderInfo(
    type = try {
        AIProviderType.valueOf(type.uppercase())
    } catch (e: Exception) {
        AIProviderType.OLLAMA
    },
    baseUrl = baseUrl,
    model = model,
    hasApiKey = hasApiKey,
    timeout = timeout,
    maxTokens = maxTokens,
    temperature = temperature,
    enabled = enabled,
    isActive = isActive,
)

fun AIModelDTO.toDomain(): AIModel = AIModel(
    id = id,
    name = name,
    provider = try {
        AIProviderType.valueOf(provider.uppercase())
    } catch (e: Exception) {
        AIProviderType.OLLAMA
    },
    supportsVision = supportsVision,
    contextLength = contextLength,
    description = description,
)

fun AIChatResponseDTO.toDomain(): AIChatResponse = AIChatResponse(
    content = content,
    model = model,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
)

fun AIChatMessage.toDTO(): AIChatMessageDTO = AIChatMessageDTO(
    role = role.name,
    content = content,
    images = images,
)

fun List<AIProviderInfoDTO>.toProviderList(): List<AIProviderInfo> = map { it.toDomain() }
fun List<AIModelDTO>.toModelList(): List<AIModel> = map { it.toDomain() }
