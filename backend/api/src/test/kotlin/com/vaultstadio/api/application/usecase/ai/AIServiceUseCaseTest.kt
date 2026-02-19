/**
 * AIServiceUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.ai

import arrow.core.Either
import com.vaultstadio.core.ai.AIModel
import com.vaultstadio.core.ai.AIProviderConfig
import com.vaultstadio.core.ai.AIProviderType
import com.vaultstadio.core.ai.AIService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AIServiceUseCaseTest {

    private val aiService: AIService = mockk()
    private val useCase = AIServiceUseCaseImpl(aiService)

    @Test
    fun getProvidersDelegatesToAIService() {
        val config = AIProviderConfig(
            type = AIProviderType.OLLAMA,
            baseUrl = "http://localhost:11434",
            apiKey = null,
            model = "llama2",
        )
        every { aiService.getProviders() } returns listOf(config)

        val result = useCase.getProviders()

        assertEquals(1, result.size)
        assertEquals(AIProviderType.OLLAMA, result[0].type)
        assertEquals("http://localhost:11434", result[0].baseUrl)
    }

    @Test
    fun getActiveProviderReturnsNullWhenNone() {
        every { aiService.getActiveProvider() } returns null

        val result = useCase.getActiveProvider()

        assertNull(result)
    }

    @Test
    fun listModelsDelegatesToAIService() = runTest {
        val models = listOf(
            AIModel(id = "llama2", name = "Llama 2", provider = "ollama"),
        )
        coEvery { aiService.listModels() } returns Either.Right(models)

        val result = useCase.listModels()

        assertTrue(result.isRight())
        val list = (result as Either.Right).value
        assertEquals(1, list.size)
        assertEquals("llama2", list[0].id)
    }
}
