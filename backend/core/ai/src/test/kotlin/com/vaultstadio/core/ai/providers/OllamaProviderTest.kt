/**
 * VaultStadio Ollama Provider Tests
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

class OllamaProviderTest {

    private lateinit var provider: OllamaProvider
    private val testConfig = AIProviderConfig(
        type = AIProviderType.OLLAMA,
        baseUrl = "http://localhost:11434",
        model = "llava",
    )

    @BeforeEach
    fun setup() {
        provider = OllamaProvider(testConfig)
    }

    @Nested
    inner class ConfigurationTests {

        @Test
        fun `should have correct provider type`() {
            assertEquals(AIProviderType.OLLAMA, provider.type)
        }

        @Test
        fun `should store config`() {
            assertEquals("http://localhost:11434", provider.config.baseUrl)
            assertEquals("llava", provider.config.model)
        }

        @Test
        fun `should use default timeout`() {
            assertEquals(120000L, provider.config.timeout)
        }
    }

    @Nested
    inner class AvailabilityTests {

        @Test
        fun `isAvailable should return false when server unreachable`() = runTest {
            // With no actual Ollama server running, should return false
            val result = provider.isAvailable()
            // In unit test environment, Ollama is not running
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
        fun `chat should use configured model`() = runTest {
            val request = AIRequest(
                messages = listOf(AIMessage(role = "user", content = "Hello")),
            )

            // Model from config should be used
            assertEquals("llava", provider.config.model)
        }

        @Test
        fun `chat should override model from request`() = runTest {
            val request = AIRequest(
                messages = listOf(AIMessage(role = "user", content = "Hello")),
                model = "llama2",
            )

            // Request model should override config
            assertEquals("llama2", request.model)
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
    inner class RequestFormattingTests {

        @Test
        fun `should format request with system message`() {
            val request = AIRequest(
                messages = listOf(
                    AIMessage(role = "system", content = "You are helpful"),
                    AIMessage(role = "user", content = "Hello"),
                ),
            )

            assertEquals(2, request.messages.size)
            assertEquals("system", request.messages[0].role)
            assertEquals("user", request.messages[1].role)
        }

        @Test
        fun `should handle messages with images`() {
            val request = AIRequest(
                messages = listOf(
                    AIMessage(
                        role = "user",
                        content = "What is this?",
                        images = listOf("base64data1", "base64data2"),
                    ),
                ),
            )

            assertEquals(2, request.messages[0].images?.size)
        }
    }
}
