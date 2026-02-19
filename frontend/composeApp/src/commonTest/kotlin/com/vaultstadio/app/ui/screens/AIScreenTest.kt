/**
 * VaultStadio AI Screen Tests
 */

package com.vaultstadio.app.ui.screens

import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.ai.model.ChatRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIScreenTest {

    @Test
    fun testChatMessageCreation() {
        val userMessage = AIChatMessage(ChatRole.USER, "Hello")
        val assistantMessage = AIChatMessage(ChatRole.ASSISTANT, "Hi there!")
        val loadingMessage = AIChatMessage(ChatRole.ASSISTANT, "")

        assertEquals(ChatRole.USER, userMessage.role)
        assertEquals("Hello", userMessage.content)
        assertEquals(null, userMessage.images)

        assertEquals(ChatRole.ASSISTANT, assistantMessage.role)
        assertEquals("Hi there!", assistantMessage.content)
    }

    @Test
    fun testAIProviderInfoModel() {
        val provider = AIProviderInfo(
            type = AIProviderType.OLLAMA,
            baseUrl = "http://localhost:11434",
            model = "llama2",
            hasApiKey = false,
            enabled = true,
            isActive = true,
        )

        assertEquals(AIProviderType.OLLAMA, provider.type)
        assertEquals("http://localhost:11434", provider.baseUrl)
        assertTrue(provider.isActive)
    }

    @Test
    fun testAIModelModel() {
        val model = AIModel(
            id = "llava:latest",
            name = "LLaVA",
            provider = AIProviderType.OLLAMA,
            supportsVision = true,
            contextLength = 4096,
        )

        assertEquals("llava:latest", model.id)
        assertTrue(model.supportsVision)
    }

    @Test
    fun testAIProviderTypeEnumValues() {
        val types = AIProviderType.entries
        assertEquals(3, types.size)
        assertTrue(types.contains(AIProviderType.OLLAMA))
        assertTrue(types.contains(AIProviderType.LM_STUDIO))
        assertTrue(types.contains(AIProviderType.OPENROUTER))
    }

    @Test
    fun testChatRoleEnumValues() {
        val roles = ChatRole.entries
        assertEquals(3, roles.size)
        assertTrue(roles.contains(ChatRole.SYSTEM))
        assertTrue(roles.contains(ChatRole.USER))
        assertTrue(roles.contains(ChatRole.ASSISTANT))
    }
}
