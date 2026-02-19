/**
 * VaultStadio AI API Models Tests
 */

package com.vaultstadio.app.api

import com.vaultstadio.app.domain.model.AIChatMessage
import com.vaultstadio.app.domain.model.AIChatRequest
import com.vaultstadio.app.domain.model.AIClassifyRequest
import com.vaultstadio.app.domain.model.AIDescribeRequest
import com.vaultstadio.app.domain.model.AIProviderType
import com.vaultstadio.app.domain.model.AISummarizeRequest
import com.vaultstadio.app.domain.model.AITagRequest
import com.vaultstadio.app.domain.model.AIVisionRequest
import com.vaultstadio.app.domain.model.ChatRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIApiModelsTest {

    @Test
    fun testAIChatMessageCreation() {
        val message = AIChatMessage(
            role = ChatRole.USER,
            content = "Hello, AI!",
            images = listOf("base64image1", "base64image2"),
        )

        assertEquals(ChatRole.USER, message.role)
        assertEquals("Hello, AI!", message.content)
        assertEquals(2, message.images?.size)
    }

    @Test
    fun testAIChatMessageWithoutImages() {
        val message = AIChatMessage(
            role = ChatRole.ASSISTANT,
            content = "Hello, User!",
        )

        assertEquals(ChatRole.ASSISTANT, message.role)
        assertEquals(null, message.images)
    }

    @Test
    fun testAIChatRequestCreation() {
        val messages = listOf(
            AIChatMessage(ChatRole.SYSTEM, "You are a helpful assistant."),
            AIChatMessage(ChatRole.USER, "What is the capital of France?"),
        )

        val request = AIChatRequest(
            messages = messages,
            model = "llama2",
            maxTokens = 1000,
            temperature = 0.7,
        )

        assertEquals(2, request.messages.size)
        assertEquals("llama2", request.model)
        assertEquals(1000, request.maxTokens)
        assertEquals(0.7, request.temperature)
    }

    @Test
    fun testAIChatRequestDefaults() {
        val messages = listOf(AIChatMessage(ChatRole.USER, "Hello"))
        val request = AIChatRequest(messages = messages)

        assertEquals(null, request.model)
        assertEquals(null, request.maxTokens)
        assertEquals(null, request.temperature)
    }

    @Test
    fun testAIVisionRequestCreation() {
        val request = AIVisionRequest(
            prompt = "Describe this image",
            imageBase64 = "base64encodedimage",
            mimeType = "image/png",
        )

        assertEquals("Describe this image", request.prompt)
        assertEquals("image/png", request.mimeType)
    }

    @Test
    fun testAIVisionRequestDefaultMimeType() {
        val request = AIVisionRequest(
            prompt = "What's in this image?",
            imageBase64 = "base64data",
        )

        assertEquals("image/jpeg", request.mimeType)
    }

    @Test
    fun testAIDescribeRequestCreation() {
        val request = AIDescribeRequest(
            itemId = "item-1",
            prompt = "Describe this image",
        )

        assertEquals("item-1", request.itemId)
        assertEquals("Describe this image", request.prompt)
    }

    @Test
    fun testAITagRequestCreation() {
        val request = AITagRequest(
            itemIds = listOf("item-1"),
            tags = listOf("tag1", "tag2"),
        )

        assertEquals(1, request.itemIds.size)
        assertEquals(2, request.tags.size)
    }

    @Test
    fun testAIClassifyRequestCreation() {
        val request = AIClassifyRequest(
            itemId = "item-1",
            categories = listOf("Technology", "Finance", "Health", "Sports"),
        )

        assertEquals(4, request.categories?.size)
        assertTrue(request.categories!!.contains("Technology"))
    }

    @Test
    fun testAISummarizeRequestCreation() {
        val request = AISummarizeRequest(
            itemId = "item-1",
            maxLength = 100,
        )

        assertEquals("item-1", request.itemId)
        assertEquals(100, request.maxLength)
    }

    @Test
    fun testAISummarizeRequestDefaultMaxLength() {
        val request = AISummarizeRequest(itemId = "item-1")

        assertEquals(null, request.maxLength)
    }

    @Test
    fun testAIProviderTypeValues() {
        val types = AIProviderType.entries

        assertEquals(3, types.size)
        assertTrue(types.contains(AIProviderType.OLLAMA))
        assertTrue(types.contains(AIProviderType.LM_STUDIO))
        assertTrue(types.contains(AIProviderType.OPENROUTER))
    }
}
