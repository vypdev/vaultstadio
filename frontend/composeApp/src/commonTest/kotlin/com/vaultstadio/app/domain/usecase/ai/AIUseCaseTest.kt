/**
 * Unit tests for AI use cases (GetAIProviders).
 * Uses a fake AIRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIChatMessage
import com.vaultstadio.app.domain.model.AIChatResponse
import com.vaultstadio.app.domain.model.AIModel
import com.vaultstadio.app.domain.model.AIProviderInfo
import com.vaultstadio.app.domain.model.AIProviderType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun testAIProviderInfo(
    type: AIProviderType = AIProviderType.OLLAMA,
    baseUrl: String = "http://localhost:11434",
    isActive: Boolean = false,
) = AIProviderInfo(
    type = type,
    baseUrl = baseUrl,
    model = null,
    hasApiKey = false,
    timeout = 30000L,
    maxTokens = 2048,
    temperature = 0.7,
    enabled = true,
    isActive = isActive,
)

private fun <T> stubResult(): Result<T> = Result.error("TEST", "Not implemented in fake")

private fun testAIModel(
    id: String = "model-1",
    name: String = "llama2",
    provider: AIProviderType = AIProviderType.OLLAMA,
) = AIModel(
    id = id,
    name = name,
    provider = provider,
    supportsVision = false,
    contextLength = null,
    description = null,
)

private class FakeAIRepository(
    var getProvidersResult: Result<List<AIProviderInfo>> = Result.success(emptyList()),
    var getModelsResult: Result<List<AIModel>> = Result.success(emptyList()),
) : AIRepository {

    override suspend fun getProviders(): Result<List<AIProviderInfo>> = getProvidersResult

    override suspend fun getModels(): Result<List<AIModel>> = getModelsResult

    override suspend fun configureProvider(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String?,
        model: String?,
    ): Result<String> = stubResult()

    override suspend fun setActiveProvider(type: AIProviderType): Result<String> = stubResult()

    override suspend fun deleteProvider(type: AIProviderType): Result<Unit> = stubResult()

    override suspend fun getProviderStatus(type: AIProviderType): Result<Map<String, Boolean>> = stubResult()

    override suspend fun getProviderModels(type: AIProviderType): Result<List<AIModel>> = getModelsResult

    override suspend fun chat(
        messages: List<AIChatMessage>,
        model: String?,
        maxTokens: Int?,
        temperature: Double?,
    ): Result<AIChatResponse> = stubResult()

    override suspend fun describeImage(imageBase64: String, mimeType: String): Result<String> = stubResult()

    override suspend fun tagImage(imageBase64: String, mimeType: String): Result<List<String>> = stubResult()

    override suspend fun classify(content: String, categories: List<String>): Result<String> = stubResult()

    override suspend fun summarize(text: String, maxLength: Int): Result<String> = stubResult()
}

class GetAIProvidersUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetProvidersResult() = runTest {
        val providers = listOf(
            testAIProviderInfo(AIProviderType.OLLAMA, "http://localhost:11434"),
            testAIProviderInfo(AIProviderType.LM_STUDIO, "http://localhost:1234", isActive = true),
        )
        val repo = FakeAIRepository(getProvidersResult = Result.success(providers))
        val useCase = GetAIProvidersUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals(AIProviderType.OLLAMA, result.getOrNull()?.get(0)?.type)
        assertTrue(result.getOrNull()?.get(1)?.isActive == true)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAIRepository(getProvidersResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetAIProvidersUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetAIModelsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetModelsResult() = runTest {
        val models = listOf(
            testAIModel("m1", "llama2", AIProviderType.OLLAMA),
            testAIModel("m2", "codellama", AIProviderType.OLLAMA),
        )
        val repo = FakeAIRepository(getModelsResult = Result.success(models))
        val useCase = GetAIModelsUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("llama2", result.getOrNull()?.get(0)?.name)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAIRepository(getModelsResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetAIModelsUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}
