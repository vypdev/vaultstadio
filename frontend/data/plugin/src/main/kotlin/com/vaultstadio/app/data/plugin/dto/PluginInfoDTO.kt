/**
 * Plugin Data Transfer Objects
 */

package com.vaultstadio.app.data.plugin.dto

import kotlinx.serialization.Serializable

@Serializable
data class PluginInfoDTO(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val isEnabled: Boolean,
    val state: String,
)
