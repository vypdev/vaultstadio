/**
 * Text Selection DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.serialization.Serializable

@Serializable
data class TextSelectionDTO(val start: CursorPositionDTO, val end: CursorPositionDTO)
