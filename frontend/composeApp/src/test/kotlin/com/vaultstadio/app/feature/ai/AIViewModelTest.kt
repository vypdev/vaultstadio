/**
 * Unit tests for AIViewModel: loadProviders, loadModels, chat, describeImage, tagImage,
 * classifyContent, summarize, configureProvider, hideProviderConfig, deleteProvider,
 * checkProviderStatus, loadProviderModels, clearProviderStatus, clearProviderModels, clearError.
 */

package com.vaultstadio.app.feature.ai

import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIChatResponse
import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.ai.usecase.AIChatUseCase
import com.vaultstadio.app.domain.ai.usecase.ClassifyContentUseCase
import com.vaultstadio.app.domain.ai.usecase.DeleteAIProviderUseCase
import com.vaultstadio.app.domain.ai.usecase.DescribeImageUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProviderStatusUseCase
import com.vaultstadio.app.domain.ai.usecase.GetAIProvidersUseCase
import com.vaultstadio.app.domain.ai.usecase.GetProviderModelsUseCase
import com.vaultstadio.app.domain.ai.usecase.SummarizeTextUseCase
import com.vaultstadio.app.domain.ai.usecase.TagImageUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun testProvider(
    type: AIProviderType = AIProviderType.OLLAMA,
    isActive: Boolean = false,
) = AIProviderInfo(
    type = type,
    baseUrl = "http://localhost",
    model = null,
    hasApiKey = false,
    timeout = 30000L,
    maxTokens = 2048,
    temperature = 0.7,
    enabled = true,
    isActive = isActive,
)

private fun testModel(id: String = "m1") = AIModel(
    id = id,
    name = "Test Model",
    provider = AIProviderType.OLLAMA,
    supportsVision = false,
    contextLength = null,
)

private class FakeGetAIProvidersUseCase(
    var result: Result<List<AIProviderInfo>> = Result.success(emptyList()),
) : GetAIProvidersUseCase {
    override suspend fun invoke(): Result<List<AIProviderInfo>> = result
}

private class FakeGetAIModelsUseCase(
    var result: Result<List<AIModel>> = Result.success(emptyList()),
) : GetAIModelsUseCase {
    override suspend fun invoke(): Result<List<AIModel>> = result
}

private class FakeGetProviderModelsUseCase(
    var result: Result<List<AIModel>> = Result.success(emptyList()),
) : GetProviderModelsUseCase {
    override suspend fun invoke(type: AIProviderType): Result<List<AIModel>> = result
}

private class FakeGetAIProviderStatusUseCase(
    var result: Result<Map<String, Boolean>> = Result.success(emptyMap()),
) : GetAIProviderStatusUseCase {
    override suspend fun invoke(type: AIProviderType): Result<Map<String, Boolean>> = result
}

private class FakeDeleteAIProviderUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : DeleteAIProviderUseCase {
    override suspend fun invoke(type: AIProviderType): Result<Unit> = result
}

private class FakeAIChatUseCase(
    var result: Result<AIChatResponse> = Result.success(AIChatResponse(content = "Hi", model = "m1")),
) : AIChatUseCase {
    override suspend fun invoke(
        messages: List<AIChatMessage>,
        model: String?,
        maxTokens: Int?,
        temperature: Double?,
    ): Result<AIChatResponse> = result
}

private class FakeDescribeImageUseCase(
    var result: Result<String> = Result.success("Description"),
) : DescribeImageUseCase {
    override suspend fun invoke(imageBase64: String, mimeType: String): Result<String> = result
}

private class FakeTagImageUseCase(
    var result: Result<List<String>> = Result.success(listOf("tag1", "tag2")),
) : TagImageUseCase {
    override suspend fun invoke(imageBase64: String, mimeType: String): Result<List<String>> = result
}

private class FakeClassifyContentUseCase(
    var result: Result<String> = Result.success("category"),
) : ClassifyContentUseCase {
    override suspend fun invoke(content: String, categories: List<String>): Result<String> = result
}

private class FakeSummarizeTextUseCase(
    var result: Result<String> = Result.success("Summary"),
) : SummarizeTextUseCase {
    override suspend fun invoke(text: String, maxLength: Int): Result<String> = result
}

class AIViewModelTest {

    private fun createViewModel(
        getProvidersResult: Result<List<AIProviderInfo>> = Result.success(emptyList()),
        getModelsResult: Result<List<AIModel>> = Result.success(emptyList()),
        getProviderModelsResult: Result<List<AIModel>> = Result.success(emptyList()),
        getProviderStatusResult: Result<Map<String, Boolean>> = Result.success(emptyMap()),
        deleteProviderResult: Result<Unit> = Result.success(Unit),
        chatResult: Result<AIChatResponse> = Result.success(AIChatResponse(content = "Hi", model = "m1")),
        describeImageResult: Result<String> = Result.success("Desc"),
        tagImageResult: Result<List<String>> = Result.success(emptyList()),
        classifyResult: Result<String> = Result.success("cat"),
        summarizeResult: Result<String> = Result.success("Sum"),
    ): AIViewModel = AIViewModel(
        getAIProvidersUseCase = FakeGetAIProvidersUseCase(getProvidersResult),
        getAIModelsUseCase = FakeGetAIModelsUseCase(getModelsResult),
        getProviderModelsUseCase = FakeGetProviderModelsUseCase(getProviderModelsResult),
        getProviderStatusUseCase = FakeGetAIProviderStatusUseCase(getProviderStatusResult),
        deleteAIProviderUseCase = FakeDeleteAIProviderUseCase(deleteProviderResult),
        aiChatUseCase = FakeAIChatUseCase(chatResult),
        describeImageUseCase = FakeDescribeImageUseCase(describeImageResult),
        tagImageUseCase = FakeTagImageUseCase(tagImageResult),
        classifyContentUseCase = FakeClassifyContentUseCase(classifyResult),
        summarizeTextUseCase = FakeSummarizeTextUseCase(summarizeResult),
    )

    @Test
    fun loadProviders_success_setsProvidersAndActiveProvider() = ViewModelTestBase.runTestWithMain {
        val providers = listOf(
            testProvider(AIProviderType.OLLAMA, isActive = false),
            testProvider(AIProviderType.LM_STUDIO, isActive = true),
        )
        val vm = createViewModel(getProvidersResult = Result.success(providers))
        vm.loadProviders()
        assertEquals(providers, vm.providers)
        assertEquals(providers[1], vm.activeProvider)
    }

    @Test
    fun loadProviders_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getProvidersResult = Result.error("ERR", "Load failed"))
        vm.loadProviders()
        assertTrue(vm.providers.isEmpty())
        assertEquals("Load failed", vm.error)
    }

    @Test
    fun loadModels_success_setsModels() = ViewModelTestBase.runTestWithMain {
        val models = listOf(testModel("m1"), testModel("m2"))
        val vm = createViewModel(getModelsResult = Result.success(models))
        vm.loadModels()
        assertEquals(models, vm.models)
    }

    @Test
    fun chat_success_invokesCallbackWithContent() = ViewModelTestBase.runTestWithMain {
        var callbackContent: String? = null
        val vm = createViewModel(chatResult = Result.success(AIChatResponse(content = "Hello", model = "m1")))
        vm.chat(emptyList(), null) { callbackContent = it }
        assertEquals("Hello", callbackContent)
        assertNull(vm.error)
    }

    @Test
    fun chat_error_setsErrorAndCallbackNull() = ViewModelTestBase.runTestWithMain {
        var callbackContent: String? = "initial"
        val vm = createViewModel(chatResult = Result.error("ERR", "Chat failed"))
        vm.chat(emptyList(), null) { callbackContent = it }
        assertEquals("Chat failed", vm.error)
        assertNull(callbackContent)
    }

    @Test
    fun describeImage_success_invokesCallback() = ViewModelTestBase.runTestWithMain {
        var received: String? = null
        val vm = createViewModel(describeImageResult = Result.success("A red car"))
        vm.describeImage("base64", "image/png") { received = it }
        assertEquals("A red car", received)
    }

    @Test
    fun tagImage_success_invokesCallback() = ViewModelTestBase.runTestWithMain {
        var received: List<String>? = null
        val tags = listOf("outdoor", "nature")
        val vm = createViewModel(tagImageResult = Result.success(tags))
        vm.tagImage("base64", "image/jpeg") { received = it }
        assertEquals(tags, received)
    }

    @Test
    fun classifyContent_success_invokesCallback() = ViewModelTestBase.runTestWithMain {
        var received: String? = null
        val vm = createViewModel(classifyResult = Result.success("sport"))
        vm.classifyContent("text", listOf("sport", "news")) { received = it }
        assertEquals("sport", received)
    }

    @Test
    fun summarize_success_invokesCallback() = ViewModelTestBase.runTestWithMain {
        var received: String? = null
        val vm = createViewModel(summarizeResult = Result.success("Short summary"))
        vm.summarize("long text", 100) { received = it }
        assertEquals("Short summary", received)
    }

    @Test
    fun configureProvider_setsShowProviderConfigTrue() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.configureProvider()
        assertTrue(vm.showProviderConfig)
    }

    @Test
    fun hideProviderConfig_setsShowProviderConfigFalse() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.configureProvider()
        assertTrue(vm.showProviderConfig)
        vm.hideProviderConfig()
        assertFalse(vm.showProviderConfig)
    }

    @Test
    fun deleteProvider_success_reloadsProviders() = ViewModelTestBase.runTestWithMain {
        val providers = listOf(testProvider(AIProviderType.OLLAMA))
        val getProviders = FakeGetAIProvidersUseCase(Result.success(providers))
        val vm = AIViewModel(
            getAIProvidersUseCase = getProviders,
            getAIModelsUseCase = FakeGetAIModelsUseCase(),
            getProviderModelsUseCase = FakeGetProviderModelsUseCase(),
            getProviderStatusUseCase = FakeGetAIProviderStatusUseCase(),
            deleteAIProviderUseCase = FakeDeleteAIProviderUseCase(Result.success(Unit)),
            aiChatUseCase = FakeAIChatUseCase(),
            describeImageUseCase = FakeDescribeImageUseCase(),
            tagImageUseCase = FakeTagImageUseCase(),
            classifyContentUseCase = FakeClassifyContentUseCase(),
            summarizeTextUseCase = FakeSummarizeTextUseCase(),
        )
        vm.deleteProvider(AIProviderType.OLLAMA)
        assertEquals(providers, vm.providers)
    }

    @Test
    fun checkProviderStatus_success_setsProviderStatus() = ViewModelTestBase.runTestWithMain {
        val status = mapOf("ok" to true)
        val vm = createViewModel(getProviderStatusResult = Result.success(status))
        vm.checkProviderStatus(AIProviderType.OLLAMA)
        assertEquals(status, vm.providerStatus)
    }

    @Test
    fun loadProviderModels_success_setsSelectedProviderModels() = ViewModelTestBase.runTestWithMain {
        val models = listOf(testModel("m1"))
        val vm = createViewModel(getProviderModelsResult = Result.success(models))
        vm.loadProviderModels(AIProviderType.OLLAMA)
        assertEquals(models, vm.selectedProviderModels)
    }

    @Test
    fun clearProviderStatus_clearsMap() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getProviderStatusResult = Result.success(mapOf("x" to true)))
        vm.checkProviderStatus(AIProviderType.OLLAMA)
        assertTrue(vm.providerStatus.isNotEmpty())
        vm.clearProviderStatus()
        assertTrue(vm.providerStatus.isEmpty())
    }

    @Test
    fun clearProviderModels_clearsList() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getProviderModelsResult = Result.success(listOf(testModel())))
        vm.loadProviderModels(AIProviderType.OLLAMA)
        assertTrue(vm.selectedProviderModels.isNotEmpty())
        vm.clearProviderModels()
        assertTrue(vm.selectedProviderModels.isEmpty())
    }

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getProvidersResult = Result.error("ERR", "Oops"))
        vm.loadProviders()
        assertEquals("Oops", vm.error)
        vm.clearError()
        assertNull(vm.error)
    }
}
