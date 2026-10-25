/**
 * Cursor Position DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.serialization.Serializable

@Serializable
data class CursorPositionDTO(val line: Int, val column: Int, val offset: Int)
