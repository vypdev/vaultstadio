/**
 * VaultStadio AI Classification Plugin
 *
 * Provides AI-powered image classification, object detection, and auto-tagging.
 * Uses the centralized AIService through the plugin's AIApi for consistent
 * provider management and configuration.
 */

package com.vaultstadio.plugins.ai

import com.vaultstadio.core.domain.event.EventHandlerResult
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.plugins.api.AbstractPlugin
import com.vaultstadio.plugins.api.PluginMetadata
import com.vaultstadio.plugins.api.PluginPermission
import com.vaultstadio.plugins.config.FieldType
import com.vaultstadio.plugins.config.pluginConfiguration
import com.vaultstadio.plugins.context.PluginContext
import com.vaultstadio.plugins.hooks.ClassificationHook
import com.vaultstadio.plugins.hooks.ClassificationLabel
import com.vaultstadio.plugins.hooks.ClassificationResult
import com.vaultstadio.plugins.metadata.MetadataKeys
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

private val logger = KotlinLogging.logger {}

/**
 * AI Classification plugin using the centralized AIService.
 *
 * Features:
 * - Automatic image classification on upload
 * - Object detection and labeling
 * - Scene recognition
 * - Custom prompt-based classification
 * - Uses the centralized AI provider configuration
 */
class AIClassificationPlugin : AbstractPlugin(), ClassificationHook {

    override val metadata = PluginMetadata(
        id = "com.vaultstadio.plugins.ai-classification",
        name = "AI Classification",
        version = "1.1.0",
        description = "AI-powered image classification and auto-tagging using centralized AI providers",
        author = "VaultStadio",
        website = "https://vaultstadio.io",
        permissions = setOf(
            PluginPermission.READ_FILES,
            PluginPermission.WRITE_METADATA,
            PluginPermission.BACKGROUND_TASKS,
        ),
        supportedMimeTypes = setOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
        ),
    )

    private val configSchema = pluginConfiguration {
        group("classification", "Classification Settings") {
            field("autoClassify", "Auto-classify on Upload", FieldType.BOOLEAN) {
                description = "Automatically classify images when uploaded"
                defaultValue = true
            }
            field("maxFileSize", "Max File Size (MB)", FieldType.NUMBER) {
                description = "Maximum file size to process (in MB)"
                defaultValue = 20
                validation = "value >= 1 && value <= 100"
            }
            field("prompt", "Classification Prompt", FieldType.STRING) {
                description = "Custom prompt for classification"
                defaultValue =
                    "Describe this image. List the main objects, scene type, colors, and any text visible. Format as JSON with keys: description, objects, scene, colors, text"
            }
            field("confidenceThreshold", "Confidence Threshold", FieldType.NUMBER) {
                description = "Minimum confidence for labels (0-100)"
                defaultValue = 50
                validation = "value >= 0 && value <= 100"
            }
        }
    }

    override fun getConfigurationSchema() = configSchema

    private lateinit var pluginContext: PluginContext

    // Configuration
    private var autoClassify = true
    private var maxFileSizeMb = 20
    private var prompt = ""
    private var confidenceThreshold = 50

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun onInitialize(context: PluginContext) {
        pluginContext = context
        loadConfiguration()

        // Subscribe to file upload events
        context.eventBus.subscribe<FileEvent.Uploaded>(metadata.id) { event ->
            if (autoClassify && context.ai != null) {
                handleFileUploaded(event)
            } else {
                EventHandlerResult.Success
            }
        }

        val aiAvailable = context.ai?.isAvailable() ?: false
        logger.info { "AI Classification Plugin initialized (AI available: $aiAvailable)" }
    }

    private fun loadConfiguration() {
        autoClassify = pluginContext.config.getBoolean("autoClassify") ?: true
        maxFileSizeMb = pluginContext.config.getInt("maxFileSize") ?: 20
        prompt = pluginContext.config.getString("prompt") ?: DEFAULT_PROMPT
        confidenceThreshold = pluginContext.config.getInt("confidenceThreshold") ?: 50
    }

    private suspend fun handleFileUploaded(event: FileEvent.Uploaded): EventHandlerResult {
        val item = event.item

        // Check if this is a supported image type
        if (!metadata.supportedMimeTypes.contains(item.mimeType)) {
            return EventHandlerResult.Success
        }

        // Check file size
        if (item.size > maxFileSizeMb * 1024 * 1024) {
            logger.info { "Skipping classification of ${item.name}: file too large" }
            return EventHandlerResult.Success
        }

        return try {
            val streamResult = pluginContext.storage.retrieve(
                item.storageKey ?: return EventHandlerResult.Success,
            )

            streamResult.fold(
                { error ->
                    logger.error { "Failed to retrieve file for classification: ${error.message}" }
                    EventHandlerResult.Error(error)
                },
                { stream ->
                    stream.use { classifyAndSave(item, it) }
                    EventHandlerResult.Success
                },
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to classify file ${item.id}" }
            EventHandlerResult.Error(e)
        }
    }

    private suspend fun classifyAndSave(item: StorageItem, stream: InputStream) {
        val result = classify(item, stream)

        if (result.labels.isNotEmpty()) {
            // Save labels as metadata
            val labels = result.labels
                .filter { it.confidence * 100 >= confidenceThreshold }
                .joinToString(",") { it.name }

            pluginContext.metadata.setValue(item.id, MetadataKeys.AI_TAGS, labels)

            // Save detailed classification
            result.labels.firstOrNull()?.let { primary ->
                pluginContext.metadata.setValue(item.id, MetadataKeys.CLASSIFICATION, primary.name)
                pluginContext.metadata.setValue(
                    item.id,
                    MetadataKeys.CONFIDENCE,
                    (primary.confidence * 100).toInt().toString(),
                )
            }

            // Save classification timestamp
            pluginContext.metadata.setValue(
                item.id,
                "classifiedAt",
                Clock.System.now().toString(),
            )

            logger.info { "Classified ${item.name}: ${result.labels.size} labels" }
        }
    }

    // ClassificationHook implementation
    override suspend fun classify(item: StorageItem, stream: InputStream): ClassificationResult {
        val aiApi = pluginContext.ai
        if (aiApi == null) {
            logger.warn { "AI API not available for classification" }
            return ClassificationResult(emptyList())
        }

        return withContext(Dispatchers.IO) {
            try {
                // Read image bytes
                val imageBytes = stream.readBytes()
                val mimeType = item.mimeType ?: "image/jpeg"

                // Use the centralized AI service through the plugin API
                val result = aiApi.analyzeImage(imageBytes, mimeType, prompt)

                result.fold(
                    onSuccess = { response -> parseClassificationResponse(response) },
                    onError = { error ->
                        logger.error { "AI classification failed for ${item.name}: ${error.message}" }
                        ClassificationResult(emptyList())
                    },
                )
            } catch (e: Exception) {
                logger.error(e) { "Classification failed for ${item.name}" }
                ClassificationResult(emptyList())
            }
        }
    }

    private fun parseClassificationResponse(response: String): ClassificationResult {
        // Try to parse as JSON first
        return try {
            val parsed = json.decodeFromString<AIClassificationOutput>(response)

            val labels = mutableListOf<ClassificationLabel>()

            // Add objects as labels
            parsed.objects?.forEach { obj ->
                labels.add(
                    ClassificationLabel(
                        name = obj,
                        confidence = 0.8,
                        category = "object",
                    ),
                )
            }

            // Add scene as label
            parsed.scene?.let { scene ->
                labels.add(
                    ClassificationLabel(
                        name = scene,
                        confidence = 0.9,
                        category = "scene",
                    ),
                )
            }

            // Add colors as labels
            parsed.colors?.forEach { color ->
                labels.add(
                    ClassificationLabel(
                        name = color,
                        confidence = 0.7,
                        category = "color",
                    ),
                )
            }

            ClassificationResult(labels, labels.maxOfOrNull { it.confidence } ?: 0.0)
        } catch (e: Exception) {
            // Fall back to simple text parsing
            val words = response
                .replace(Regex("[^a-zA-Z\\s]"), " ")
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.length > 3 }
                .distinct()
                .take(10)

            ClassificationResult(
                labels = words.map { word ->
                    ClassificationLabel(
                        name = word,
                        confidence = 0.5,
                        category = "extracted",
                    )
                },
            )
        }
    }

    override suspend fun getStatistics(): Map<String, Any> {
        val aiAvailable = pluginContext.ai?.isAvailable() ?: false
        return mapOf(
            "aiAvailable" to aiAvailable,
            "autoClassify" to autoClassify,
            "confidenceThreshold" to confidenceThreshold,
        )
    }

    override suspend fun onShutdown() {
        logger.info { "AI Classification Plugin shut down" }
    }

    companion object {
        private const val DEFAULT_PROMPT = """Analyze this image and provide:
1. A brief description
2. Main objects/subjects visible
3. Scene type (indoor, outdoor, nature, urban, etc.)
4. Dominant colors

Format your response as JSON with keys: description, objects (array), scene, colors (array)"""
    }
}

// AI output parsing model
@Serializable
data class AIClassificationOutput(
    val description: String? = null,
    val objects: List<String>? = null,
    val scene: String? = null,
    val colors: List<String>? = null,
    val text: String? = null,
)
