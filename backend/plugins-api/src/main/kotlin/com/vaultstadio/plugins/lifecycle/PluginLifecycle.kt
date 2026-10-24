/**
 * VaultStadio Plugin Lifecycle
 *
 * Defines the lifecycle methods that plugins implement.
 */

package com.vaultstadio.plugins.lifecycle

import com.vaultstadio.plugins.context.PluginContext

/**
 * Plugin lifecycle interface.
 *
 * Plugins go through the following lifecycle:
 * 1. Load: Plugin class is loaded, dependencies resolved
 * 2. Initialize: Plugin starts, subscribes to events, registers endpoints
 * 3. Running: Plugin is active and processing events
 * 4. Shutdown: Plugin is being stopped, cleanup
 * 5. Unload: Plugin is completely removed
 */
interface PluginLifecycle {

    /**
     * Called when the plugin is loaded.
     *
     * This is called after the plugin class is instantiated and
     * before initialization. Use this for one-time setup like
     * registering configuration schema.
     *
     * @param context The plugin context providing access to core APIs
     */
    suspend fun onLoad(context: PluginContext) {}

    /**
     * Called when the plugin is initialized and should start running.
     *
     * Use this to:
     * - Subscribe to events
     * - Register API endpoints
     * - Start background tasks
     * - Load saved state
     *
     * @param context The plugin context providing access to core APIs
     */
    suspend fun onInitialize(context: PluginContext)

    /**
     * Called when the plugin should stop running.
     *
     * Use this to:
     * - Cancel background tasks
     * - Unsubscribe from events
     * - Save state
     * - Clean up resources
     */
    suspend fun onShutdown() {}

    /**
     * Called when the plugin is being unloaded completely.
     *
     * This is called after shutdown. Use this for final cleanup
     * like removing any persistent data if needed.
     */
    suspend fun onUnload() {}

    /**
     * Called when the plugin is being upgraded from a previous version.
     *
     * @param previousVersion The version being upgraded from
     * @param context The plugin context
     */
    suspend fun onUpgrade(previousVersion: String, context: PluginContext) {}

    /**
     * Called when the plugin is enabled after being disabled.
     *
     * @param context The plugin context
     */
    suspend fun onEnable(context: PluginContext) {}

    /**
     * Called when the plugin is disabled but not uninstalled.
     */
    suspend fun onDisable() {}
}
