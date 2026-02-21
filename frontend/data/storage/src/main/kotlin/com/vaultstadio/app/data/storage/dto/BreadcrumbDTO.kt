/**
 * Breadcrumb DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class BreadcrumbDTO(
    val id: String?,
    val name: String,
    val path: String,
)
