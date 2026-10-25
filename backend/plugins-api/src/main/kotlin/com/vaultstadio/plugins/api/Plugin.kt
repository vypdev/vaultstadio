/**
 * VaultStadio Plugin API
 *
 * Core plugin interface that all plugins must implement.
 */

package com.vaultstadio.plugins.api

import com.vaultstadio.plugins.config.PluginConfiguration
import com.vaultstadio.plugins.context.PluginContext
import com.vaultstadio.plugins.lifecycle.PluginLifecycle
import kotlinx.serialization.Serializable

/**
 * Plugin metadata describing the plugin.
 *
 * @property id Unique identifier for the plugin (e.g., "com.example.myplugin")
 * @property name Human-readable name
 * @property version Semantic version (e.g., "1.0.0")
 * @property description Brief description of what the plugin does
 * @property author Plugin author
 * @property website Plugin website or documentation URL
 * @property minCoreVersion Minimum VaultStadio core version required
 * @property permissions Permissions this plugin requires
 * @property dependencies List of other plugins this plugin depends on
 * @property tags Tags for categorization
 * @property supportedMimeTypes MIME types this plugin can process
 */
@Serializable
data class PluginMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val website: String? = null,
    val minCoreVersion: String = "1.0.0",
    val permissions: Set<PluginPermission> = emptySet(),
    val dependencies: List<PluginDependency> = emptyList(),
    val tags: List<String> = emptyList(),
    val supportedMimeTypes: Set<String> = emptySet(),
)

/**
 * Plugin permission types.
 */
@Serializable
enum class PluginPermission {
    /** Read file content */
    READ_FILES,

    /** Write/modify files */
    WRITE_FILES,

    /** Delete files */
    DELETE_FILES,

    /** Read file metadata */
    READ_METADATA,

    /** Write file metadata */
    WRITE_METADATA,

    /** Access user information */
    READ_USERS,

    /** Make network requests */
    NETWORK,

    /** Execute background tasks */
    BACKGROUND_TASKS,

    /** Access system configuration */
    READ_CONFIG,

    /** Register API endpoints */
    REGISTER_ENDPOINTS,

    /** Access storage backend directly */
    STORAGE_BACKEND,

    /** Execute system commands */
    EXECUTE_COMMANDS,
}

/**
 * Plugin dependency specification.
 *
 * @property pluginId ID of the required plugin
 * @property minVersion Minimum version required
 * @property optional Whether this dependency is optional
 */
@Serializable
data class PluginDependency(
    val pluginId: String,
    val minVersion: String,
    val optional: Boolean = false,
)

/**
 * Plugin state.
 */
enum class PluginState {
    /** Plugin is registered but not loaded */
    REGISTERED,

    /** Plugin is being loaded */
    LOADING,

    /** Plugin is loaded and ready */
    LOADED,

    /** Plugin is being initialized */
    INITIALIZING,

    /** Plugin is initialized and running */
    RUNNING,

    /** Plugin is being stopped */
    STOPPING,

    /** Plugin is stopped */
    STOPPED,

    /** Plugin failed to load or run */
    FAILED,

    /** Plugin is disabled */
    DISABLED,
}

/**
 * Main plugin interface that all VaultStadio plugins must implement.
 *
 * Plugins are the primary extension mechanism for VaultStadio.
 * They can:
 * - React to storage events (file uploads, downloads, deletions, etc.)
 * - Extract and attach metadata to files
 * - Provide custom API endpoints
 * - Add UI components (via the frontend plugin system)
 * - Integrate with external services
 *
 * Example plugin implementation:
 * ```kotlin
 * class MyPlugin : Plugin {
 *     override val metadata = PluginMetadata(
 *         id = "com.example.myplugin",
 *         name = "My Plugin",
 *         version = "1.0.0",
 *         description = "Does something cool",
 *         author = "Example Author",
 *         permissions = listOf(PluginPermission.READ_FILES, PluginPermission.WRITE_METADATA)
 *     )
 *
 *     override suspend fun onInitialize(context: PluginContext) {
 *         // Subscribe to events, register endpoints, etc.
 *         context.eventBus.subscribe<FileEvent.Uploaded>("myplugin") { event ->
 *             // Process uploaded file
 *             EventHandlerResult.Success
 *         }
 *     }
 * }
 * ```
 */
interface Plugin : PluginLifecycle {

    /**
     * Plugin metadata describing this plugin.
     */
    val metadata: PluginMetadata

    /**
     * Returns the plugin's configuration schema.
     * Override to provide custom configuration options.
     */
    fun getConfigurationSchema(): PluginConfiguration? = null

    /**
     * Returns the current configuration values.
     */
    fun getConfiguration(): Map<String, Any?> = emptyMap()

    /**
     * Updates the plugin configuration.
     *
     * @param config New configuration values
     * @return True if configuration was applied successfully
     */
    suspend fun updateConfiguration(config: Map<String, Any?>): Boolean = true

    /**
     * Returns health status of the plugin.
     */
    suspend fun healthCheck(): PluginHealthStatus = PluginHealthStatus.Healthy

    /**
     * Returns plugin statistics/metrics.
     */
    suspend fun getStatistics(): Map<String, Any> = emptyMap()
}

/**
 * Plugin health status.
 */
sealed class PluginHealthStatus {
    data object Healthy : PluginHealthStatus()
    data class Degraded(val reason: String) : PluginHealthStatus()
    data class Unhealthy(val reason: String, val error: Throwable? = null) : PluginHealthStatus()
}

/**
 * Abstract base class for plugins providing common functionality.
 */
abstract class AbstractPlugin : Plugin {

    protected var context: PluginContext? = null
        private set

    protected var state: PluginState = PluginState.REGISTERED
        private set

    override suspend fun onLoad(context: PluginContext) {
        this.context = context
        state = PluginState.LOADED
    }

    override suspend fun onInitialize(context: PluginContext) {
        state = PluginState.RUNNING
    }

    override suspend fun onShutdown() {
        state = PluginState.STOPPED
        context = null
    }

    /**
     * Helper to require a loaded context.
     */
    protected fun requireContext(): PluginContext {
        return context ?: throw IllegalStateException("Plugin not loaded")
    }
}
