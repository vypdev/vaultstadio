/**
 * Plugin Domain Model
 */

package com.vaultstadio.app.domain.model

data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val isEnabled: Boolean,
    val state: String,
)
