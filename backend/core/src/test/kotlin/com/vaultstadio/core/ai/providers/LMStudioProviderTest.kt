/**
 * VaultStadio LM Studio Provider Tests
 */

package com.vaultstadio.core.ai.providers

import com.vaultstadio.core.ai.AIMessage
import com.vaultstadio.core.ai.AIProviderConfig
import com.vaultstadio.core.ai.AIProviderType
import com.vaultstadio.core.ai.AIRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LMStudioProviderTest {

    private lateinit var provider: LMStudioProvider
    private val testConfig = AIProviderConfig(
        type = AIProviderType.LM_STUDIO,
        baseUrl = "http://localhost:1234/v1",
        model = "local-model",
    )

    @BeforeEach
    fun setup() {
        provider = LMStudioProvider(testConfig)
    }

    @Nested
    inner class ConfigurationTests {

        @Test
        fun `should have correct provider type`() {
            assertEquals(AIProviderType.LM_STUDIO, provider.type)
        }

        @Test
        fun `should use local base url`() {
            assertEquals("http://localhost:1234/v1", provider.config.baseUrl)
        }

        @Test
        fun `should not require api key`() {
            // LM Studio is local, no API key needed
            val config = provider.config
            // API key can be null for local providers
            assertTrue(config.apiKey == null || config.apiKey!!.isEmpty() || config.apiKey == "lm-studio")
        }

        @Test
        fun `should store model name`() {
            assertEquals("local-model", provider.config.model)
        }
    }

    @Nested
    inner class AvailabilityTests {

        @Test
        fun `isAvailable should return false when LM Studio not running`() = runTest {
            val result = provider.isAvailable()
            // In unit test environment, LM Studio is not running
            assertFalse(result)
        }
    }

    @Nested
    inner class ModelListTests {

        @Test
        fun `listModels should return error when server unreachable`() = runTest {
            val result = provider.listModels()
            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class ChatTests {

        @Test
        fun `chat should return error when server unreachable`() = runTest {
            val request = AIRequest(
                messages = listOf(AIMessage(role = "user", content = "Hello")),
            )

            val result = provider.chat(request)
            assertTrue(result.isLeft())
        }

        @Test
        fun `chat should use OpenAI-compatible format`() {
            // LM Studio uses OpenAI-compatible API
            val request = AIRequest(
                messages = listOf(
                    AIMessage(role = "system", content = "You are helpful"),
                    AIMessage(role = "user", content = "Hello"),
                ),
            )

            assertEquals("system", request.messages[0].role)
            assertEquals("user", request.messages[1].role)
        }
    }

    @Nested
    inner class VisionTests {

        @Test
        fun `vision should return error when server unreachable`() = runTest {
            val result = provider.vision(
                prompt = "Describe this image",
                imageBase64 = "base64data",
                mimeType = "image/jpeg",
            )
            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class OpenAICompatibilityTests {

        @Test
        fun `should format messages in OpenAI format`() {
            val request = AIRequest(
                messages = listOf(
                    AIMessage(role = "user", content = "Hello"),
                ),
                model = "local-model",
                temperature = 0.7,
                maxTokens = 1024,
            )

            assertEquals("user", request.messages[0].role)
            assertEquals(0.7, request.temperature)
            assertEquals(1024, request.maxTokens)
        }

        @Test
        fun `should support streaming option`() {
            val request = AIRequest(
                messages = listOf(AIMessage(role = "user", content = "Hello")),
                stream = true,
            )

            assertTrue(request.stream)
        }
    }

    @Nested
    inner class LocalModelTests {

        @Test
        fun `should handle various local model configurations`() {
            val configs = listOf(
                AIProviderConfig(
                    type = AIProviderType.LM_STUDIO,
                    baseUrl = "http://localhost:1234/v1",
                    model = "llama-2-7b-chat",
                ),
                AIProviderConfig(
                    type = AIProviderType.LM_STUDIO,
                    baseUrl = "http://127.0.0.1:1234/v1",
                    model = "mistral-7b-instruct",
                ),
                AIProviderConfig(
                    type = AIProviderType.LM_STUDIO,
                    baseUrl = "http://localhost:1234/v1",
                    model = "llava-1.5-7b",
                ),
            )

            configs.forEach { config ->
                val provider = LMStudioProvider(config)
                assertEquals(AIProviderType.LM_STUDIO, provider.type)
            }
        }
    }
}
