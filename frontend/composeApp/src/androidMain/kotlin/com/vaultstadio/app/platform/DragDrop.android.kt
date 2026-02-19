/**
 * VaultStadio Android Drag & Drop Implementation
 *
 * Android uses content URIs for drag and drop.
 */

package com.vaultstadio.app.platform

/**
 * Initialize drag and drop handling.
 * Note: Android handles drag and drop differently through View modifiers.
 */
actual fun initializeDragDrop(onFilesDropped: (List<SelectedFile>) -> Unit) {
    // Android drag and drop is handled through Compose modifiers
    // This is a no-op for Android as it's handled at the View level
}
