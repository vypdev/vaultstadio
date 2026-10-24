/**
 * Link identity request DTO.
 */

package com.vaultstadio.app.data.federation.dto

import kotlinx.serialization.Serializable

@Serializable
data class LinkIdentityRequestDTO(
    val remoteUserId: String,
    val remoteInstance: String,
    val displayName: String,
)
