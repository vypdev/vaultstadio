/**
 * Text Selection
 */

package com.vaultstadio.app.domain.collaboration.model

data class TextSelection(val start: CursorPosition, val end: CursorPosition) {
    val length: Int get() = kotlin.math.abs(end.offset - start.offset)
    val isEmpty: Boolean get() = length == 0
}
