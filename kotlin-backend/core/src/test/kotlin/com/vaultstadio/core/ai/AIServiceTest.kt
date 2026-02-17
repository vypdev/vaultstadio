/**
 * VaultStadio AI Service Tests
 */

package com.vaultstadio.core.ai

import arrow.core.Either
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AIServiceTest {

    private lateinit var aiService: AIServiceImpl

    @BeforeEach
    fun setup() {
        aiService = AIServiceImpl()
    }

    @Nested
    inner class ProviderConfigurationTests {

        @Test
        fun `should configure provider successfully`() = runTest {
            // Given
            val config = AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llava",
            )

            // When
            val result = aiService.configureProvider(config)

            // Then
            assertTrue(result.isRight())
            assertEquals(1, aiService.getProviders().size)
            assertEquals(AIProviderType.OLLAMA, aiService.getProviders().first().type)
        }

        @Test
        fun `should set first configured provider as active`() = runTest {
            // Given
            val config = AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llava",
            )

            // When
            aiService.configureProvider(config)

            // Then
            val active = aiService.getActiveProvider()
            assertEquals(AIProviderType.OLLAMA, active?.type)
        }

        @Test
        fun `should configure multiple providers`() = runTest {
            // Given
            val ollamaConfig = AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llava",
            )
            val openRouterConfig = AIProviderConfig(
                type = AIProviderType.OPENROUTER,
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "test-key",
                model = "anthropic/claude-3-haiku",
            )

            // When
            aiService.configureProvider(ollamaConfig)
            aiService.configureProvider(openRouterConfig)

            // Then
            assertEquals(2, aiService.getProviders().size)
        }

        @Test
        fun `should update existing provider configuration`() = runTest {
            // Given
            val initialConfig = AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llava",
            )
            val updatedConfig = AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llava:34b",
            )

            // When
            aiService.configureProvider(initialConfig)
            aiService.configureProvider(updatedConfig)

            // Then
            assertEquals(1, aiService.getProviders().size)
            assertEquals("llava:34b", aiService.getProviders().first().model)
        }

        @Test
        fun `should remove provider`() = runTest {
            // Given
            val config = AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llava",
            )
            aiService.configureProvider(config)

            // When
            val result = aiService.removeProvider(AIProviderType.OLLAMA)

            // Then
            assertTrue(result.isRight())
            assertTrue(aiService.getProviders().isEmpty())
        }

        @Test
        fun `should clear active provider when removed`() = runTest {
            // Given
            val config = AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llava",
            )
            aiService.configureProvider(config)

            // When
            aiService.removeProvider(AIProviderType.OLLAMA)

            // Then
            assertNull(aiService.getActiveProvider())
        }
    }

    @Nested
    inner class SetActiveProviderTests {

        @Test
        fun `should set active provider`() = runTest {
            // Given
            val ollamaConfig = AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llava",
            )
            val openRouterConfig = AIProviderConfig(
                type = AIProviderType.OPENROUTER,
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "test-key",
                model = "anthropic/claude-3-haiku",
            )
            aiService.configureProvider(ollamaConfig)
            aiService.configureProvider(openRouterConfig)

            // When
            val result = aiService.setActiveProvider(AIProviderType.OPENROUTER)

            // Then
            assertTrue(result.isRight())
            assertEquals(AIProviderType.OPENROUTER, aiService.getActiveProvider()?.type)
        }

        @Test
        fun `should fail to set unconfigured provider as active`() = runTest {
            // When
            val result = aiService.setActiveProvider(AIProviderType.OLLAMA)

            // Then
            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class ChatWithoutProviderTests {

        @Test
        fun `should return error when no provider configured`() = runTest {
            // When
            val result = aiService.chat(
                AIRequest(
                    messages = listOf(AIMessage(role = "user", content = "Hello")),
                ),
            )

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AIError.ProviderError)
        }

        @Test
        fun `should return error for vision without provider`() = runTest {
            // When
            val result = aiService.vision("Describe this", "base64image", "image/jpeg")

            // Then
            assertTrue(result.isLeft())
        }

        @Test
        fun `should return error for describe without provider`() = runTest {
            // When
            val result = aiService.describeImage("base64image")

            // Then
            assertTrue(result.isLeft())
        }

        @Test
        fun `should return error for tag without provider`() = runTest {
            // When
            val result = aiService.tagImage("base64image")

            // Then
            assertTrue(result.isLeft())
        }

        @Test
        fun `should return error for classify without provider`() = runTest {
            // When
            val result = aiService.classify("content", listOf("cat1", "cat2"))

            // Then
            assertTrue(result.isLeft())
        }

        @Test
        fun `should return error for summarize without provider`() = runTest {
            // When
            val result = aiService.summarize("long text", 100)

            // Then
            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class ListModelsTests {

        @Test
        fun `should return error when listing models without provider`() = runTest {
            // When
            val result = aiService.listModels()

            // Then
            assertTrue(result.isLeft())
        }

        @Test
        fun `should return error for unconfigured provider type`() = runTest {
            // When
            val result = aiService.listModels(AIProviderType.OPENROUTER)

            // Then
            assertTrue(result.isLeft())
        }
    }
}

class AIProviderConfigTest {

    @Test
    fun `should create config with defaults`() {
        val config = AIProviderConfig(
            type = AIProviderType.OLLAMA,
            baseUrl = "http://localhost:11434",
            model = "llava",
        )

        assertEquals(120000L, config.timeout)
        assertEquals(1024, config.maxTokens)
        assertEquals(0.7, config.temperature)
        assertTrue(config.enabled)
        assertNull(config.apiKey)
    }

    @Test
    fun `should create config with custom values`() {
        val config = AIProviderConfig(
            type = AIProviderType.OPENROUTER,
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = "test-key",
            model = "anthropic/claude-3-opus",
            timeout = 60000,
            maxTokens = 2048,
            temperature = 0.3,
            enabled = false,
        )

        assertEquals(60000L, config.timeout)
        assertEquals(2048, config.maxTokens)
        assertEquals(0.3, config.temperature)
        assertFalse(config.enabled)
        assertEquals("test-key", config.apiKey)
    }
}

class AIMessageTest {

    @Test
    fun `should create message without images`() {
        val message = AIMessage(
            role = "user",
            content = "Hello",
        )

        assertEquals("user", message.role)
        assertEquals("Hello", message.content)
        assertNull(message.images)
    }

    @Test
    fun `should create message with images`() {
        val message = AIMessage(
            role = "user",
            content = "Describe this image",
            images = listOf("base64data1", "base64data2"),
        )

        assertEquals(2, message.images?.size)
    }
}

class AIRequestTest {

    @Test
    fun `should create request with defaults`() {
        val request = AIRequest(
            messages = listOf(AIMessage(role = "user", content = "Hello")),
        )

        assertEquals(1, request.messages.size)
        assertNull(request.model)
        assertNull(request.maxTokens)
        assertNull(request.temperature)
        assertFalse(request.stream)
    }

    @Test
    fun `should create request with custom values`() {
        val request = AIRequest(
            messages = listOf(
                AIMessage(role = "system", content = "You are helpful"),
                AIMessage(role = "user", content = "Hello"),
            ),
            model = "custom-model",
            maxTokens = 500,
            temperature = 0.5,
            stream = true,
        )

        assertEquals(2, request.messages.size)
        assertEquals("custom-model", request.model)
        assertEquals(500, request.maxTokens)
        assertEquals(0.5, request.temperature)
        assertTrue(request.stream)
    }
}

class AIResponseTest {

    @Test
    fun `should create response with all fields`() {
        val response = AIResponse(
            content = "Hello! How can I help?",
            model = "llava",
            promptTokens = 10,
            completionTokens = 5,
            totalTokens = 15,
            finishReason = "stop",
        )

        assertEquals("Hello! How can I help?", response.content)
        assertEquals("llava", response.model)
        assertEquals(15, response.totalTokens)
        assertEquals("stop", response.finishReason)
    }
}

class AIErrorTest {

    @Test
    fun `should create connection error`() {
        val error = AIError.ConnectionError("Failed to connect")
        assertEquals("Failed to connect", error.errorMessage)
    }

    @Test
    fun `should create authentication error`() {
        val error = AIError.AuthenticationError("Invalid API key")
        assertEquals("Invalid API key", error.errorMessage)
    }

    @Test
    fun `should create rate limit error`() {
        val error = AIError.RateLimitError("Too many requests")
        assertEquals("Too many requests", error.errorMessage)
    }

    @Test
    fun `should create model not found error`() {
        val error = AIError.ModelNotFoundError("Model xyz not found")
        assertEquals("Model xyz not found", error.errorMessage)
    }

    @Test
    fun `should create provider error`() {
        val error = AIError.ProviderError("Something went wrong")
        assertEquals("Something went wrong", error.errorMessage)
    }
}

class AIModelTest {

    @Test
    fun `should create model with defaults`() {
        val model = AIModel(
            id = "llava",
            name = "LLaVA",
            provider = "ollama",
        )

        assertFalse(model.supportsVision)
        assertEquals(4096, model.contextLength)
        assertNull(model.description)
    }

    @Test
    fun `should create vision model`() {
        val model = AIModel(
            id = "gpt-4-vision",
            name = "GPT-4 Vision",
            provider = "openai",
            supportsVision = true,
            contextLength = 128000,
            description = "Vision-capable GPT-4",
        )

        assertTrue(model.supportsVision)
        assertEquals(128000, model.contextLength)
    }
}
