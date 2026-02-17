/**
 * VaultStadio Keyboard Shortcuts
 *
 * Keyboard shortcut handling for file operations.
 */

package com.vaultstadio.app.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Keyboard shortcut actions.
 */
data class KeyboardShortcutActions(
    val onSelectAll: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onCopy: () -> Unit = {},
    val onPaste: () -> Unit = {},
    val onCut: () -> Unit = {},
    val onRename: () -> Unit = {},
    val onNewFolder: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onEscape: () -> Unit = {},
    val onUpload: () -> Unit = {},
)

/**
 * Modifier extension for handling keyboard shortcuts.
 */
fun Modifier.keyboardShortcuts(
    actions: KeyboardShortcutActions,
): Modifier = this.onKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) {
        return@onKeyEvent false
    }

    val isModifierPressed = event.isCtrlPressed || event.isMetaPressed

    when {
        // Ctrl/Cmd+A - Select All
        isModifierPressed && event.key == Key.A -> {
            actions.onSelectAll()
            true
        }
        // Ctrl/Cmd+C - Copy
        isModifierPressed && event.key == Key.C -> {
            actions.onCopy()
            true
        }
        // Ctrl/Cmd+V - Paste
        isModifierPressed && event.key == Key.V -> {
            actions.onPaste()
            true
        }
        // Ctrl/Cmd+X - Cut
        isModifierPressed && event.key == Key.X -> {
            actions.onCut()
            true
        }
        // Ctrl/Cmd+Shift+N or Ctrl/Cmd+N - New Folder
        isModifierPressed && event.key == Key.N -> {
            actions.onNewFolder()
            true
        }
        // Ctrl/Cmd+R or F5 - Refresh
        (isModifierPressed && event.key == Key.R) || event.key == Key.F5 -> {
            actions.onRefresh()
            true
        }
        // Ctrl/Cmd+U - Upload
        isModifierPressed && event.key == Key.U -> {
            actions.onUpload()
            true
        }
        // Delete/Backspace - Delete
        event.key == Key.Delete || event.key == Key.Backspace -> {
            actions.onDelete()
            true
        }
        // F2 - Rename
        event.key == Key.F2 -> {
            actions.onRename()
            true
        }
        // Escape - Cancel/Deselect
        event.key == Key.Escape -> {
            actions.onEscape()
            true
        }
        else -> false
    }
}

/**
 * Keyboard shortcut help dialog content.
 */
@Composable
fun KeyboardShortcutsHelp(): List<ShortcutItem> {
    return listOf(
        ShortcutItem("Ctrl/⌘ + A", "Select all"),
        ShortcutItem("Ctrl/⌘ + C", "Copy selected"),
        ShortcutItem("Ctrl/⌘ + V", "Paste"),
        ShortcutItem("Ctrl/⌘ + X", "Cut selected"),
        ShortcutItem("Ctrl/⌘ + N", "New folder"),
        ShortcutItem("Ctrl/⌘ + U", "Upload files"),
        ShortcutItem("Ctrl/⌘ + R", "Refresh"),
        ShortcutItem("F2", "Rename"),
        ShortcutItem("Delete", "Move to trash"),
        ShortcutItem("Escape", "Clear selection"),
    )
}

data class ShortcutItem(
    val shortcut: String,
    val description: String,
)
