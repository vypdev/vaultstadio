/**
 * Plugin Mappers
 */

package com.vaultstadio.app.data.plugin.mapper

import com.vaultstadio.app.data.plugin.dto.PluginInfoDTO
import com.vaultstadio.app.domain.plugin.model.PluginInfo

fun PluginInfoDTO.toDomain(): PluginInfo = PluginInfo(
    id = id,
    name = name,
    version = version,
    description = description,
    author = author,
    isEnabled = isEnabled,
    state = state,
)

fun List<PluginInfoDTO>.toPluginList(): List<PluginInfo> = map { it.toDomain() }
