/**
 * VaultStadio Platform Drag & Drop
 *
 * Expect/actual declarations for platform-specific drag and drop handling.
 */

package com.vaultstadio.app.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Drag and drop events.
 */
sealed class DragDropEvent {
    data object DragEnter : DragDropEvent()
    data object DragLeave : DragDropEvent()
    data class Drop(val files: List<SelectedFile>) : DragDropEvent()
}

/**
 * Global drag and drop state for the application.
 */
object DragDropState {
    private val _isDragging = MutableStateFlow(false)
    val isDragging: Flow<Boolean> = _isDragging

    private val _events = MutableStateFlow<DragDropEvent?>(null)
    val events: Flow<DragDropEvent?> = _events

    fun setDragging(dragging: Boolean) {
        _isDragging.value = dragging
    }

    fun emitEvent(event: DragDropEvent) {
        _events.value = event
    }

    fun clearEvent() {
        _events.value = null
    }
}

/**
 * Initialize drag and drop handling for the platform.
 */
expect fun initializeDragDrop(onFilesDropped: (List<SelectedFile>) -> Unit)
