/**
 * AI Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.ai.AIChatMessageDTO
import com.vaultstadio.app.data.dto.ai.AIChatResponseDTO
import com.vaultstadio.app.data.dto.ai.AIModelDTO
import com.vaultstadio.app.data.dto.ai.AIProviderInfoDTO
import com.vaultstadio.app.domain.model.AIChatMessage
import com.vaultstadio.app.domain.model.AIChatResponse
import com.vaultstadio.app.domain.model.AIModel
import com.vaultstadio.app.domain.model.AIProviderInfo
import com.vaultstadio.app.domain.model.AIProviderType

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
