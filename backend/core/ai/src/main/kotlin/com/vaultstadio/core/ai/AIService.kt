/**
 * VaultStadio AI Service
 *
 * Central service for managing AI providers and executing AI tasks.
 */

package com.vaultstadio.core.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.ai.providers.LMStudioProvider
import com.vaultstadio.core.ai.providers.OllamaProvider
import com.vaultstadio.core.ai.providers.OpenRouterProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * AI Service interface.
 */
interface AIService {
    /**
     * Get all configured providers.
     */
    fun getProviders(): List<AIProviderConfig>

    /**
     * Get the active provider configuration.
     */
    fun getActiveProvider(): AIProviderConfig?

    /**
     * Set the active provider.
     */
    suspend fun setActiveProvider(type: AIProviderType): Either<AIError, Unit>

    /**
     * Configure a provider.
     */
    suspend fun configureProvider(config: AIProviderConfig): Either<AIError, Unit>

    /**
     * Remove a provider configuration.
     */
    suspend fun removeProvider(type: AIProviderType): Either<AIError, Unit>

    /**
     * Check if a provider is available.
     */
    suspend fun isProviderAvailable(type: AIProviderType): Boolean

    /**
     * List models from the active provider.
     */
    suspend fun listModels(): Either<AIError, List<AIModel>>

    /**
     * List models from a specific provider.
     */
    suspend fun listModels(type: AIProviderType): Either<AIError, List<AIModel>>

    /**
     * Send a chat request to the active provider.
     */
    suspend fun chat(request: AIRequest): Either<AIError, AIResponse>

    /**
     * Send a vision request to the active provider.
     */
    suspend fun vision(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
    ): Either<AIError, AIResponse>

    /**
     * Generate a description for an image.
     */
    suspend fun describeImage(imageBase64: String): Either<AIError, String>

    /**
     * Generate tags for an image.
     */
    suspend fun tagImage(imageBase64: String): Either<AIError, List<String>>

    /**
     * Classify content.
     */
    suspend fun classify(
        content: String,
        categories: List<String>,
    ): Either<AIError, String>

    /**
     * Summarize text.
     */
    suspend fun summarize(text: String, maxLength: Int = 200): Either<AIError, String>
}

/**
 * AI Service implementation.
 */
class AIServiceImpl : AIService {

    private val providers = ConcurrentHashMap<AIProviderType, AIProviderConfig>()
    private val providerInstances = ConcurrentHashMap<AIProviderType, AIProvider>()
    private var activeProviderType: AIProviderType? = null
    private val mutex = Mutex()

    override fun getProviders(): List<AIProviderConfig> = providers.values.toList()

    override fun getActiveProvider(): AIProviderConfig? =
        activeProviderType?.let { providers[it] }

    override suspend fun setActiveProvider(type: AIProviderType): Either<AIError, Unit> {
        if (!providers.containsKey(type)) {
            return AIError.ProviderError("Provider $type not configured").left()
        }

        mutex.withLock {
            activeProviderType = type
        }

        logger.info { "Active AI provider set to: $type" }
        return Unit.right()
    }

    override suspend fun configureProvider(config: AIProviderConfig): Either<AIError, Unit> {
        mutex.withLock {
            // Close existing instance if any
            providerInstances.remove(config.type)?.let { provider ->
                closeProvider(provider)
            }

            // Store configuration
            providers[config.type] = config

            // Create and cache provider instance
            val provider = createProvider(config)
            providerInstances[config.type] = provider

            // If no active provider, set this one
            if (activeProviderType == null && config.enabled) {
                activeProviderType = config.type
            }
        }

        logger.info { "Configured AI provider: ${config.type}" }
        return Unit.right()
    }

    override suspend fun removeProvider(type: AIProviderType): Either<AIError, Unit> {
        mutex.withLock {
            providerInstances.remove(type)?.let { provider ->
                closeProvider(provider)
            }
            providers.remove(type)

            if (activeProviderType == type) {
                activeProviderType = providers.keys.firstOrNull()
            }
        }

        logger.info { "Removed AI provider: $type" }
        return Unit.right()
    }

    override suspend fun isProviderAvailable(type: AIProviderType): Boolean {
        return getProviderInstance(type)?.isAvailable() ?: false
    }

    override suspend fun listModels(): Either<AIError, List<AIModel>> {
        val provider = getActiveProviderInstance()
            ?: return AIError.ProviderError("No active AI provider").left()
        return provider.listModels()
    }

    override suspend fun listModels(type: AIProviderType): Either<AIError, List<AIModel>> {
        val provider = getProviderInstance(type)
            ?: return AIError.ProviderError("Provider $type not configured").left()
        return provider.listModels()
    }

    override suspend fun chat(request: AIRequest): Either<AIError, AIResponse> {
        val provider = getActiveProviderInstance()
            ?: return AIError.ProviderError("No active AI provider").left()
        return provider.chat(request)
    }

    override suspend fun vision(
        prompt: String,
        imageBase64: String,
        mimeType: String,
    ): Either<AIError, AIResponse> {
        val provider = getActiveProviderInstance()
            ?: return AIError.ProviderError("No active AI provider").left()
        return provider.vision(prompt, imageBase64, mimeType)
    }

    override suspend fun describeImage(imageBase64: String): Either<AIError, String> {
        val prompt = """Describe this image in detail. Include:
            |1. Main subject or scene
            |2. Colors and lighting
            |3. Any text visible
            |4. Mood or atmosphere
            |Keep the description concise but informative.
        """.trimMargin()

        return vision(prompt, imageBase64).map { it.content }
    }

    override suspend fun tagImage(imageBase64: String): Either<AIError, List<String>> {
        val prompt = """Analyze this image and provide relevant tags.
            |Return ONLY a comma-separated list of tags, nothing else.
            |Include: objects, scene type, colors, mood, style.
            |Example: sunset, beach, ocean, orange, peaceful, landscape
        """.trimMargin()

        return vision(prompt, imageBase64).map { response ->
            response.content
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() && it.length > 2 }
                .distinct()
        }
    }

    override suspend fun classify(
        content: String,
        categories: List<String>,
    ): Either<AIError, String> {
        val prompt = """Classify the following content into one of these categories: ${categories.joinToString(", ")}
            |
            |Content: $content
            |
            |Reply with ONLY the category name, nothing else.
        """.trimMargin()

        return chat(
            AIRequest(
                messages = listOf(AIMessage(role = "user", content = prompt)),
            ),
        ).map { response ->
            val result = response.content.trim().lowercase()
            categories.find { it.lowercase() == result } ?: categories.first()
        }
    }

    override suspend fun summarize(text: String, maxLength: Int): Either<AIError, String> {
        val prompt = """Summarize the following text in $maxLength characters or less:
            |
            |$text
            |
            |Provide only the summary, no introduction.
        """.trimMargin()

        return chat(
            AIRequest(
                messages = listOf(AIMessage(role = "user", content = prompt)),
            ),
        ).map { it.content.take(maxLength) }
    }

    private fun getActiveProviderInstance(): AIProvider? {
        return activeProviderType?.let { getProviderInstance(it) }
    }

    private fun getProviderInstance(type: AIProviderType): AIProvider? {
        return providerInstances[type] ?: providers[type]?.let { config ->
            createProvider(config).also { providerInstances[type] = it }
        }
    }

    private fun createProvider(config: AIProviderConfig): AIProvider {
        return when (config.type) {
            AIProviderType.OLLAMA -> OllamaProvider(config)
            AIProviderType.LM_STUDIO -> LMStudioProvider(config)
            AIProviderType.OPENROUTER -> OpenRouterProvider(config)
            AIProviderType.OPENAI -> OpenRouterProvider(
                config.copy(baseUrl = "https://api.openai.com/v1"),
            )
            AIProviderType.CUSTOM -> OpenRouterProvider(config) // Use OpenAI-compatible format
        }
    }

    private fun closeProvider(provider: AIProvider) {
        when (provider) {
            is OllamaProvider -> provider.close()
            is LMStudioProvider -> provider.close()
            is OpenRouterProvider -> provider.close()
        }
    }
}

/**
 * Factory for creating AIService instances.
 */
object AIServiceFactory {

    /**
     * Create an AIService with default local configuration.
     */
    fun createDefault(): AIService {
        return AIServiceImpl().apply {
            // No default configuration - user must configure
        }
    }

    /**
     * Create an AIService with Ollama configured.
     */
    suspend fun createWithOllama(
        url: String = "http://localhost:11434",
        model: String = "llava",
    ): AIService {
        val service = AIServiceImpl()
        service.configureProvider(
            AIProviderConfig(
                type = AIProviderType.OLLAMA,
                baseUrl = url,
                model = model,
            ),
        )
        return service
    }

    /**
     * Create an AIService with OpenRouter configured.
     */
    suspend fun createWithOpenRouter(
        apiKey: String,
        model: String = "anthropic/claude-3-haiku",
    ): AIService {
        val service = AIServiceImpl()
        service.configureProvider(
            AIProviderConfig(
                type = AIProviderType.OPENROUTER,
                baseUrl = OpenRouterProvider.DEFAULT_BASE_URL,
                apiKey = apiKey,
                model = model,
            ),
        )
        return service
    }
}
