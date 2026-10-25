/**
 * VaultStadio Plugin Manager implementation.
 */

package com.vaultstadio.api.plugins

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.ai.AIService
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.domain.auth.repository.UserRepository
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.PluginException
import com.vaultstadio.domain.common.exception.PluginLoadException
import com.vaultstadio.domain.common.exception.PluginNotFoundException
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.plugins.api.Plugin
import com.vaultstadio.plugins.api.PluginManager
import com.vaultstadio.plugins.api.PluginState
import com.vaultstadio.plugins.context.AIApi
import com.vaultstadio.plugins.context.AIResult
import com.vaultstadio.plugins.context.ConfigStore
import com.vaultstadio.plugins.context.EndpointRequest
import com.vaultstadio.plugins.context.EndpointResponse
import com.vaultstadio.plugins.context.HttpClientApi
import com.vaultstadio.plugins.context.HttpResponse
import com.vaultstadio.plugins.context.MetadataApi
import com.vaultstadio.plugins.context.PluginContext
import com.vaultstadio.plugins.context.PluginLogger
import com.vaultstadio.plugins.context.StorageApi
import com.vaultstadio.plugins.context.UserApi
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Plugin manager implementation.
 */
class PluginManagerImpl(
    private val eventBus: EventBus,
    private val storageItemRepository: StorageItemRepository,
    private val metadataRepository: MetadataRepository,
    private val userRepository: UserRepository,
    private val storageBackend: StorageBackend? = null,
    private val aiService: AIService? = null,
) : PluginManager {

    private val plugins = ConcurrentHashMap<String, Plugin>()
    private val pluginStates = ConcurrentHashMap<String, PluginState>()
    private val pluginContexts = ConcurrentHashMap<String, PluginContextImpl>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun listPlugins(): List<Plugin> = plugins.values.toList()

    override fun getPlugin(pluginId: String): Plugin? = plugins[pluginId]

    override fun isPluginEnabled(pluginId: String): Boolean =
        pluginStates[pluginId] == PluginState.RUNNING

    override fun getPluginState(pluginId: String): PluginState =
        pluginStates[pluginId] ?: PluginState.REGISTERED

    override suspend fun enablePlugin(pluginId: String): Either<PluginException, Unit> {
        val plugin = plugins[pluginId]
            ?: return PluginNotFoundException(pluginId).left()

        val currentState = pluginStates[pluginId]
        if (currentState == PluginState.RUNNING) {
            return Unit.right()
        }

        return try {
            pluginStates[pluginId] = PluginState.INITIALIZING

            val context = pluginContexts.getOrPut(pluginId) {
                createPluginContext(plugin)
            }

            plugin.onEnable(context)
            plugin.onInitialize(context)

            pluginStates[pluginId] = PluginState.RUNNING
            logger.info { "Plugin enabled: $pluginId" }

            Unit.right()
        } catch (e: Exception) {
            logger.error(e) { "Failed to enable plugin: $pluginId" }
            pluginStates[pluginId] = PluginState.FAILED
            PluginLoadException(pluginId, cause = e).left()
        }
    }

    override suspend fun disablePlugin(pluginId: String): Either<PluginException, Unit> {
        val plugin = plugins[pluginId]
            ?: return PluginNotFoundException(pluginId).left()

        val currentState = pluginStates[pluginId]
        if (currentState != PluginState.RUNNING) {
            return Unit.right()
        }

        return try {
            pluginStates[pluginId] = PluginState.STOPPING

            plugin.onDisable()

            // Cancel plugin context scope
            pluginContexts[pluginId]?.cancel()

            pluginStates[pluginId] = PluginState.DISABLED
            logger.info { "Plugin disabled: $pluginId" }

            Unit.right()
        } catch (e: Exception) {
            logger.error(e) { "Failed to disable plugin: $pluginId" }
            PluginLoadException(pluginId, cause = e).left()
        }
    }

    override suspend fun loadPlugins() {
        logger.info { "Loading plugins..." }

        // Use ServiceLoader to discover plugins
        val serviceLoader = ServiceLoader.load(Plugin::class.java)

        for (plugin in serviceLoader) {
            try {
                val pluginId = plugin.metadata.id
                logger.info { "Discovered plugin: $pluginId (${plugin.metadata.name})" }

                plugins[pluginId] = plugin
                pluginStates[pluginId] = PluginState.REGISTERED

                // Create context and load
                val context = createPluginContext(plugin)
                pluginContexts[pluginId] = context

                pluginStates[pluginId] = PluginState.LOADING
                plugin.onLoad(context)
                pluginStates[pluginId] = PluginState.LOADED

                // Initialize
                pluginStates[pluginId] = PluginState.INITIALIZING
                plugin.onInitialize(context)
                pluginStates[pluginId] = PluginState.RUNNING

                logger.info { "Plugin loaded and running: $pluginId" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to load plugin: ${plugin.metadata.id}" }
                pluginStates[plugin.metadata.id] = PluginState.FAILED
            }
        }

        logger.info { "Loaded ${plugins.size} plugins" }
    }

    override suspend fun shutdown() {
        logger.info { "Shutting down plugins..." }

        for ((pluginId, plugin) in plugins) {
            try {
                if (pluginStates[pluginId] == PluginState.RUNNING) {
                    pluginStates[pluginId] = PluginState.STOPPING
                    plugin.onShutdown()
                    plugin.onUnload()
                    pluginStates[pluginId] = PluginState.STOPPED
                }

                pluginContexts[pluginId]?.cancel()
            } catch (e: Exception) {
                logger.error(e) { "Error shutting down plugin: $pluginId" }
            }
        }

        scope.cancel()
        logger.info { "Plugins shut down" }
    }

    override suspend fun handlePluginEndpoint(
        pluginId: String,
        method: String,
        path: String,
        request: EndpointRequest,
    ): EndpointResponse? {
        val context = pluginContexts[pluginId] ?: return null
        if (pluginStates[pluginId] != PluginState.RUNNING) return null
        return context.handleEndpoint(method, path, request)
    }

    override fun getPluginEndpoints(pluginId: String): Set<String> {
        val context = pluginContexts[pluginId] ?: return emptySet()
        return context.getRegisteredEndpoints()
    }

    private fun createPluginContext(plugin: Plugin): PluginContextImpl {
        val pluginId = plugin.metadata.id
        val pluginContext = scope.coroutineContext + SupervisorJob()
        val pluginScope = CoroutineScope(pluginContext)

        return PluginContextImpl(
            pluginId = pluginId,
            pluginScope = pluginScope,
            eventBus = eventBus,
            storageItemRepository = storageItemRepository,
            metadataRepository = metadataRepository,
            userRepository = userRepository,
            storageBackend = storageBackend,
            aiService = aiService,
            tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "vaultstadio", "plugins", pluginId, "temp"),
            dataDirectory = Paths.get(System.getenv("VAULTSTADIO_DATA_PATH") ?: "/data/plugins", pluginId),
        )
    }
}

/**
 * Plugin context implementation.
 */
class PluginContextImpl(
    override val pluginId: String,
    private val pluginScope: CoroutineScope,
    override val eventBus: EventBus,
    private val storageItemRepository: StorageItemRepository,
    private val metadataRepository: MetadataRepository,
    private val userRepository: UserRepository,
    private val storageBackend: StorageBackend?,
    private val aiService: AIService?,
    override val tempDirectory: Path,
    override val dataDirectory: Path,
) : PluginContext {

    override val scope: CoroutineScope get() = pluginScope

    private val endpoints =
        mutableMapOf<
            String,
            suspend (
                EndpointRequest,
            ) -> EndpointResponse,
            >()
    private val tasks = mutableMapOf<String, Job>()

    override val storage = object : StorageApi {
        override suspend fun getItem(itemId: String) = storageItemRepository.findById(itemId)
        override suspend fun getItemByPath(
            path: String,
            ownerId: String,
        ) = storageItemRepository.findByPath(path, ownerId)
        override suspend fun listFolder(folderId: String?, ownerId: String, limit: Int, offset: Int) =
            storageItemRepository.query(
                StorageItemQuery(parentId = folderId, ownerId = ownerId, limit = limit, offset = offset),
            )

        override suspend fun readFile(
            itemId: String,
        ): Either<StorageException, InputStream> {
            if (storageBackend == null) {
                return StorageBackendException(
                    "plugin",
                    "Storage backend not available",
                ).left()
            }

            val itemResult = storageItemRepository.findById(itemId)
            return itemResult.fold(
                { error -> error.left() },
                { item ->
                    if (item == null) {
                        ItemNotFoundException(itemId).left()
                    } else {
                        when {
                            item.storageKey == null -> StorageBackendException(
                                "plugin",
                                "Item has no storage key",
                            ).left()
                            else -> storageBackend.retrieve(item.storageKey!!)
                        }
                    }
                },
            )
        }

        override suspend fun retrieve(
            storageKey: String,
        ): Either<StorageException, InputStream> {
            if (storageBackend == null) {
                return StorageBackendException(
                    "plugin",
                    "Storage backend not available",
                ).left()
            }
            return storageBackend.retrieve(storageKey)
        }

        override suspend fun getPresignedUrl(
            itemId: String,
            expirationSeconds: Long,
        ): Either<StorageException, String?> {
            if (storageBackend == null) {
                return StorageBackendException(
                    "plugin",
                    "Storage backend not available",
                ).left()
            }

            // Get the item first to get the storage key
            val itemResult = storageItemRepository.findById(itemId)
            return when {
                itemResult.isLeft() -> itemResult as Either<StorageException, String?>
                else -> {
                    val item = itemResult.getOrNull()
                    when {
                        item == null -> ItemNotFoundException(itemId).left()
                        item.storageKey == null -> (null as String?).right()
                        else -> storageBackend.getPresignedUrl(item.storageKey!!, expirationSeconds)
                    }
                }
            }
        }

        override suspend fun search(query: String, ownerId: String?, mimeTypePattern: String?, limit: Int) =
            storageItemRepository.query(
                StorageItemQuery(searchQuery = query, ownerId = ownerId, mimeType = mimeTypePattern, limit = limit),
            ).map {
                it.items
            }
        override suspend fun getItemsByMimeType(pattern: String, ownerId: String?, limit: Int) =
            storageItemRepository.query(StorageItemQuery(mimeType = pattern, ownerId = ownerId, limit = limit)).map {
                it.items
            }
    }

    override val metadata = object : MetadataApi {
        override suspend fun getMetadata(itemId: String) = metadataRepository.findByItemIdAndPluginId(itemId, pluginId)
        override suspend fun getValue(itemId: String, key: String) =
            metadataRepository.findByItemIdAndPluginIdAndKey(itemId, pluginId, key).map { it?.value }
        override suspend fun setValue(itemId: String, key: String, value: String) =
            metadataRepository.save(
                com.vaultstadio.core.domain.model.StorageItemMetadata(
                    itemId = itemId,
                    pluginId = pluginId,
                    key = key,
                    value = value,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
            )
        override suspend fun setValues(itemId: String, values: Map<String, String>) =
            metadataRepository.saveAll(
                values.map { (key, value) ->
                    com.vaultstadio.core.domain.model.StorageItemMetadata(
                        itemId = itemId,
                        pluginId = pluginId,
                        key = key,
                        value = value,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                },
            )
        override suspend fun saveAll(metadata: List<StorageItemMetadata>) =
            metadataRepository.saveAll(metadata)
        override suspend fun deleteValue(itemId: String, key: String) =
            metadataRepository.findByItemIdAndPluginIdAndKey(itemId, pluginId, key).map { metadata ->
                metadata?.let { metadataRepository.delete(it.id) }
                Unit
            }
        override suspend fun deleteAllForItem(
            itemId: String,
        ) = metadataRepository.deleteByItemIdAndPluginId(itemId, pluginId)
        override suspend fun searchByValue(key: String, valuePattern: String, limit: Int) =
            metadataRepository.searchByValue(pluginId, key, valuePattern, limit)
    }

    override val users = object : UserApi {
        override suspend fun getUserInfo(userId: String) =
            userRepository.findById(userId).map { it?.sanitized() }
        override suspend fun getCurrentUser() = null // Set from request context
    }

    override val logger = object : PluginLogger {
        private val log = KotlinLogging.logger("plugin.$pluginId")
        override fun trace(message: () -> String) = log.trace(message)
        override fun debug(message: () -> String) = log.debug(message)
        override fun info(message: () -> String) = log.info(message)
        override fun warn(message: () -> String) = log.warn(message)
        override fun error(message: () -> String) = log.error(message)
        override fun error(throwable: Throwable, message: () -> String) = log.error(throwable, message)
    }

    override val config = object : ConfigStore {
        private val store = mutableMapOf<String, Any?>()

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: String, defaultValue: T): T = store[key] as? T ?: defaultValue

        @Suppress("UNCHECKED_CAST")
        override fun <T> getOrNull(key: String): T? = store[key] as? T
        override suspend fun <T> set(key: String, value: T) {
            store[key] = value
        }
        override fun getAll(): Map<String, Any?> = store.toMap()
    }

    override val ai: AIApi? = if (aiService != null) {
        object : AIApi {
            override suspend fun isAvailable(): Boolean {
                return aiService.getActiveProvider() != null
            }

            override suspend fun describeImage(
                imageData: ByteArray,
                mimeType: String,
                prompt: String?,
            ): AIResult<String> {
                val base64 = java.util.Base64.getEncoder().encodeToString(imageData)
                return aiService.describeImage(base64).fold(
                    { error -> AIResult.Error(error.errorMessage) },
                    { result -> AIResult.Success(result) },
                )
            }

            override suspend fun tagImage(
                imageData: ByteArray,
                mimeType: String,
            ): AIResult<String> {
                val base64 = java.util.Base64.getEncoder().encodeToString(imageData)
                return aiService.tagImage(base64).fold(
                    { error -> AIResult.Error(error.errorMessage) },
                    { result -> AIResult.Success(result.joinToString(",")) },
                )
            }

            override suspend fun classify(
                text: String,
                categories: List<String>,
            ): AIResult<String> {
                return aiService.classify(text, categories).fold(
                    { error -> AIResult.Error(error.errorMessage) },
                    { result -> AIResult.Success(result) },
                )
            }

            override suspend fun analyzeImage(
                imageData: ByteArray,
                mimeType: String,
                prompt: String,
            ): AIResult<String> {
                val base64 = java.util.Base64.getEncoder().encodeToString(imageData)
                return aiService.vision(prompt, base64, mimeType).fold(
                    { error -> AIResult.Error(error.errorMessage) },
                    { result -> AIResult.Success(result.content) },
                )
            }

            override suspend fun chat(
                messages: List<Pair<String, String>>,
                model: String?,
            ): AIResult<String> {
                val request = com.vaultstadio.core.ai.AIRequest(
                    messages = messages.map { (role, content) ->
                        com.vaultstadio.core.ai.AIMessage(role = role, content = content)
                    },
                    model = model,
                )
                return aiService.chat(request).fold(
                    { error -> AIResult.Error(error.errorMessage) },
                    { result -> AIResult.Success(result.content) },
                )
            }
        }
    } else {
        null
    }

    override val httpClient: HttpClientApi =
        object : HttpClientApi {
            private val client = HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 10_000
                }
            }

            override suspend fun request(
                method: String,
                url: String,
                headers: Map<String, String>,
                body: String?,
            ): HttpResponse {
                return try {
                    val response = client.request(url) {
                        this.method = HttpMethod.parse(method.uppercase())
                        headers.forEach { (key, value) -> header(key, value) }
                        if (body != null) {
                            setBody(body)
                            contentType(ContentType.Application.Json)
                        }
                    }
                    HttpResponse(
                        statusCode = response.status.value,
                        headers = response.headers.entries().associate { it.key to it.value },
                        body = response.bodyAsText(),
                    )
                } catch (e: Exception) {
                    HttpResponse(
                        statusCode = 0,
                        headers = emptyMap(),
                        body = e.message ?: "Unknown error",
                    )
                }
            }
        }

    override fun registerEndpoint(
        method: String,
        path: String,
        handler: suspend (
            EndpointRequest,
        ) -> EndpointResponse,
    ) {
        endpoints["$method:$path"] = handler
    }

    override fun unregisterEndpoint(method: String, path: String) {
        endpoints.remove("$method:$path")
    }

    override suspend fun scheduleTask(
        name: String,
        cronExpression: String?,
        task: suspend () -> Unit,
    ): String {
        val taskId = "$pluginId:$name"
        val job = if (cronExpression == null) {
            // One-time task execution
            scope.launch { task() }
        } else {
            // Scheduled task with cron expression
            CronScheduler.schedule(scope, cronExpression, taskId, task)
                ?: scope.launch { task() } // Fallback to one-time if cron is invalid
        }
        tasks[taskId] = job
        return taskId
    }

    override suspend fun cancelTask(taskId: String) {
        tasks[taskId]?.cancel()
        tasks.remove(taskId)
    }

    fun cancel() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
        pluginScope.cancel()
    }

    /**
     * Handles a request to a registered endpoint.
     */
    suspend fun handleEndpoint(
        method: String,
        path: String,
        request: EndpointRequest,
    ): EndpointResponse? {
        val key = "${method.uppercase()}:$path"
        val handler = endpoints[key] ?: return null
        return try {
            handler(request)
        } catch (e: Exception) {
            logger.error(e) { "Plugin endpoint error" }
            EndpointResponse.error(e.message ?: "Internal error")
        }
    }

    /**
     * Gets all registered endpoint keys.
     */
    fun getRegisteredEndpoints(): Set<String> = endpoints.keys.toSet()
}
