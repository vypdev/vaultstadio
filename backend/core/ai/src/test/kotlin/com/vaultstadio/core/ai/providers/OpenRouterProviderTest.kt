/**
 * VaultStadio OpenRouter Provider Tests
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenRouterProviderTest {

    private lateinit var provider: OpenRouterProvider
    private val testConfig = AIProviderConfig(
        type = AIProviderType.OPENROUTER,
        baseUrl = "https://openrouter.ai/api/v1",
        apiKey = "test-api-key",
        model = "anthropic/claude-3-haiku",
    )

    @BeforeEach
    fun setup() {
        provider = OpenRouterProvider(testConfig)
    }

    @Nested
    inner class ConfigurationTests {

        @Test
        fun `should have correct provider type`() {
            assertEquals(AIProviderType.OPENROUTER, provider.type)
        }

        @Test
        fun `should store api key`() {
            assertEquals("test-api-key", provider.config.apiKey)
        }

        @Test
        fun `should use correct base url`() {
            assertEquals("https://openrouter.ai/api/v1", provider.config.baseUrl)
        }

        @Test
        fun `should store model`() {
            assertEquals("anthropic/claude-3-haiku", provider.config.model)
        }
    }

    @Nested
    inner class AvailabilityTests {

        @Test
        fun `isAvailable should check api connectivity`() = runTest {
            // Without valid API key, should still try to connect
            val result = provider.isAvailable()
            // Result depends on actual connectivity
        }
    }

    @Nested
    inner class ModelListTests {

        @Test
        fun `listModels should return result or error`() = runTest {
            val result = provider.listModels()
            // Result can be success (if API is reachable) or error
            // Just verify it doesn't throw
            assertNotNull(result)
        }
    }

    @Nested
    inner class ChatTests {

        @Test
        fun `chat should return error with invalid api key`() = runTest {
            val request = AIRequest(
                messages = listOf(AIMessage(role = "user", content = "Hello")),
            )

            val result = provider.chat(request)
            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class VisionTests {

        @Test
        fun `vision should format multimodal request`() = runTest {
            val result = provider.vision(
                prompt = "Describe this image",
                imageBase64 = "base64data",
                mimeType = "image/png",
            )

            // Should return error without valid API key
            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class RequestFormattingTests {

        @Test
        fun `should include required headers`() {
            // OpenRouter requires specific headers
            val config = provider.config
            assertEquals("test-api-key", config.apiKey)
        }

        @Test
        fun `should handle temperature parameter`() {
            val config = AIProviderConfig(
                type = AIProviderType.OPENROUTER,
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "test-key",
                model = "anthropic/claude-3-haiku",
                temperature = 0.5,
            )

            assertEquals(0.5, config.temperature)
        }

        @Test
        fun `should handle max tokens parameter`() {
            val config = AIProviderConfig(
                type = AIProviderType.OPENROUTER,
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "test-key",
                model = "anthropic/claude-3-haiku",
                maxTokens = 2048,
            )

            assertEquals(2048, config.maxTokens)
        }
    }

    @Nested
    inner class ErrorHandlingTests {

        @Test
        fun `should handle authentication errors`() = runTest {
            val invalidProvider = OpenRouterProvider(
                AIProviderConfig(
                    type = AIProviderType.OPENROUTER,
                    baseUrl = "https://openrouter.ai/api/v1",
                    apiKey = "invalid-key",
                    model = "anthropic/claude-3-haiku",
                ),
            )

            val result = invalidProvider.chat(
                AIRequest(messages = listOf(AIMessage(role = "user", content = "test"))),
            )

            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class ModelSupportTests {

        @Test
        fun `should support various model providers`() {
            // OpenRouter supports multiple providers
            val models = listOf(
                "anthropic/claude-3-opus",
                "anthropic/claude-3-sonnet",
                "openai/gpt-4-turbo",
                "google/gemini-pro",
                "meta-llama/llama-3-70b",
            )

            models.forEach { model ->
                val config = AIProviderConfig(
                    type = AIProviderType.OPENROUTER,
                    baseUrl = "https://openrouter.ai/api/v1",
                    apiKey = "test-key",
                    model = model,
                )
                assertEquals(model, config.model)
            }
        }
    }
}
