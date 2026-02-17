/**
 * Plugin Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.plugin.PluginInfoDTO
import com.vaultstadio.app.domain.model.PluginInfo

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
