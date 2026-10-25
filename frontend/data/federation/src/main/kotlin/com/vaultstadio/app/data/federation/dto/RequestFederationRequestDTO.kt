/**
 * Request federation request DTO.
 */

package com.vaultstadio.app.data.federation.dto

import kotlinx.serialization.Serializable

@Serializable
data class RequestFederationRequestDTO(
    val targetDomain: String,
    val message: String? = null,
)
