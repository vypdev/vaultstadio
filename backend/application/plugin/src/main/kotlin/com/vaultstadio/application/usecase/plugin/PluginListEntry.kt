/**
 * Result type for list plugins use case.
 */

package com.vaultstadio.application.usecase.plugin

import com.vaultstadio.plugins.api.Plugin
import com.vaultstadio.plugins.api.PluginState

/**
 * Plugin with its runtime state for listing.
 */
data class PluginListEntry(
    val plugin: Plugin,
    val isEnabled: Boolean,
    val state: PluginState,
)
