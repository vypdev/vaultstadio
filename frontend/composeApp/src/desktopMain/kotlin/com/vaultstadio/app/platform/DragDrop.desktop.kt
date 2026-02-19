/**
 * VaultStadio Desktop Drag & Drop Implementation
 *
 * Desktop drag and drop is handled by Compose Desktop's built-in support.
 */

package com.vaultstadio.app.platform

/**
 * Initialize drag and drop handling for desktop.
 * Note: Desktop drag and drop is typically handled at the window level.
 */
actual fun initializeDragDrop(onFilesDropped: (List<SelectedFile>) -> Unit) {
    // Desktop drag and drop is handled by Compose Desktop's window-level APIs
    // This is a no-op as we would configure it at the window creation level
}
