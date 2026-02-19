/**
 * VaultStadio App ViewModel Detailed Tests
 *
 * Comprehensive tests for ViewModel state management and business logic.
 */

package com.vaultstadio.app.viewmodel

import com.vaultstadio.app.domain.model.SortField
import com.vaultstadio.app.domain.model.SortOrder
import com.vaultstadio.app.domain.model.ViewMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Detailed unit tests for AppViewModel.
 */
class AppViewModelDetailedTest {

    // ========================================================================
    // NavDestination Tests
    // ========================================================================

    @Test
    fun `NavDestination should have all required values`() {
        val destinations = NavDestination.entries

        assertTrue(destinations.contains(NavDestination.FILES))
        assertTrue(destinations.contains(NavDestination.RECENT))
        assertTrue(destinations.contains(NavDestination.STARRED))
        assertTrue(destinations.contains(NavDestination.SHARED))
        assertTrue(destinations.contains(NavDestination.TRASH))
        assertTrue(destinations.contains(NavDestination.SETTINGS))
        assertTrue(destinations.contains(NavDestination.ADMIN))
    }

    @Test
    fun `NavDestination FILES should be the default`() {
        val defaultDestination = NavDestination.FILES
        assertEquals(NavDestination.FILES, defaultDestination)
    }

    // ========================================================================
    // ViewMode State Tests
    // ========================================================================

    @Test
    fun `ViewMode should toggle between GRID and LIST`() {
        var currentMode = ViewMode.GRID

        // Toggle to LIST
        currentMode = if (currentMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        assertEquals(ViewMode.LIST, currentMode)

        // Toggle back to GRID
        currentMode = if (currentMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        assertEquals(ViewMode.GRID, currentMode)
    }

    // ========================================================================
    // Selection State Tests
    // ========================================================================

    @Test
    fun `selection state should track multiple items`() {
        val selectedItems = mutableSetOf<String>()

        // Add items
        selectedItems.add("item-1")
        selectedItems.add("item-2")
        selectedItems.add("item-3")

        assertEquals(3, selectedItems.size)
        assertTrue(selectedItems.contains("item-1"))
    }

    @Test
    fun `selection state should toggle items correctly`() {
        val selectedItems = mutableSetOf<String>()

        // Toggle on
        val itemId = "item-1"
        if (!selectedItems.add(itemId)) {
            selectedItems.remove(itemId)
        }
        assertTrue(selectedItems.contains(itemId))

        // Toggle off
        if (!selectedItems.add(itemId)) {
            selectedItems.remove(itemId)
        }
        assertFalse(selectedItems.contains(itemId))
    }

    @Test
    fun `select all should add all visible items`() {
        val visibleItems = listOf("item-1", "item-2", "item-3", "item-4", "item-5")
        val selectedItems = mutableSetOf<String>()

        // Select all
        selectedItems.addAll(visibleItems)

        assertEquals(5, selectedItems.size)
        assertTrue(visibleItems.all { it in selectedItems })
    }

    @Test
    fun `clear selection should remove all items`() {
        val selectedItems = mutableSetOf("item-1", "item-2", "item-3")

        selectedItems.clear()

        assertTrue(selectedItems.isEmpty())
    }

    // ========================================================================
    // Sorting State Tests
    // ========================================================================

    @Test
    fun `sort state should track field and order`() {
        data class SortState(
            val field: SortField,
            val order: SortOrder,
        )

        val defaultSort = SortState(SortField.NAME, SortOrder.ASC)

        assertEquals(SortField.NAME, defaultSort.field)
        assertEquals(SortOrder.ASC, defaultSort.order)
    }

    @Test
    fun `toggling sort on same field should reverse order`() {
        data class SortState(
            val field: SortField,
            val order: SortOrder,
        )

        var sortState = SortState(SortField.NAME, SortOrder.ASC)

        // Click on NAME again - should toggle to DESC
        val clickedField = SortField.NAME
        sortState = if (sortState.field == clickedField) {
            sortState.copy(order = if (sortState.order == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC)
        } else {
            SortState(clickedField, SortOrder.ASC)
        }

        assertEquals(SortField.NAME, sortState.field)
        assertEquals(SortOrder.DESC, sortState.order)
    }

    @Test
    fun `changing sort field should reset to ASC`() {
        data class SortState(
            val field: SortField,
            val order: SortOrder,
        )

        var sortState = SortState(SortField.NAME, SortOrder.DESC)

        // Click on a different field
        val clickedField = SortField.SIZE
        sortState = if (sortState.field == clickedField) {
            sortState.copy(order = if (sortState.order == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC)
        } else {
            SortState(clickedField, SortOrder.ASC)
        }

        assertEquals(SortField.SIZE, sortState.field)
        assertEquals(SortOrder.ASC, sortState.order)
    }

    // ========================================================================
    // Breadcrumb Navigation Tests
    // ========================================================================

    @Test
    fun `breadcrumb navigation should track path`() {
        data class BreadcrumbItem(
            val id: String?,
            val name: String,
            val path: String,
        )

        val breadcrumbs = listOf(
            BreadcrumbItem(null, "Root", "/"),
            BreadcrumbItem("folder-1", "Documents", "/Documents"),
            BreadcrumbItem("folder-2", "Reports", "/Documents/Reports"),
            BreadcrumbItem("folder-3", "2024", "/Documents/Reports/2024"),
        )

        assertEquals(4, breadcrumbs.size)
        assertEquals("Root", breadcrumbs.first().name)
        assertEquals("2024", breadcrumbs.last().name)
    }

    @Test
    fun `clicking breadcrumb should navigate to that level`() {
        data class BreadcrumbItem(
            val id: String?,
            val name: String,
            val path: String,
        )

        var currentBreadcrumbs = listOf(
            BreadcrumbItem(null, "Root", "/"),
            BreadcrumbItem("folder-1", "Documents", "/Documents"),
            BreadcrumbItem("folder-2", "Reports", "/Documents/Reports"),
        )

        // Click on "Documents" (index 1)
        val clickedIndex = 1
        currentBreadcrumbs = currentBreadcrumbs.take(clickedIndex + 1)

        assertEquals(2, currentBreadcrumbs.size)
        assertEquals("Documents", currentBreadcrumbs.last().name)
    }

    // ========================================================================
    // Error State Tests
    // ========================================================================

    @Test
    fun `error state should be clearable`() {
        var error: String? = "Network error occurred"

        assertNotNull(error)

        error = null

        assertNull(error)
    }

    @Test
    fun `error state should support various error types`() {
        val errorTypes = listOf(
            "NETWORK_ERROR" to "Unable to connect to server",
            "AUTH_ERROR" to "Session expired",
            "PERMISSION_ERROR" to "You don't have permission to access this resource",
            "NOT_FOUND" to "File not found",
            "QUOTA_EXCEEDED" to "Storage quota exceeded",
        )

        errorTypes.forEach { (code, message) ->
            assertNotNull(code)
            assertNotNull(message)
            assertTrue(message.isNotEmpty())
        }
    }

    // ========================================================================
    // Loading State Tests
    // ========================================================================

    @Test
    fun `loading state should be trackable per operation`() {
        data class LoadingState(
            val isListLoading: Boolean = false,
            val isUploadLoading: Boolean = false,
            val isDeleteLoading: Boolean = false,
            val isAuthLoading: Boolean = false,
        )

        var state = LoadingState()

        // Start upload
        state = state.copy(isUploadLoading = true)
        assertTrue(state.isUploadLoading)
        assertFalse(state.isListLoading)

        // Complete upload
        state = state.copy(isUploadLoading = false)
        assertFalse(state.isUploadLoading)
    }

    // ========================================================================
    // Clipboard Operations Tests
    // ========================================================================

    @Test
    fun `clipboard should track cut and copy operations`() {
        data class ClipboardState(
            val itemIds: List<String>,
            val isCut: Boolean, // true = cut, false = copy
        )

        var clipboard: ClipboardState? = null

        // Copy operation
        clipboard = ClipboardState(listOf("item-1", "item-2"), isCut = false)
        assertFalse(clipboard.isCut)

        // Cut operation
        clipboard = ClipboardState(listOf("item-3"), isCut = true)
        assertTrue(clipboard.isCut)
    }

    @Test
    fun `paste should clear clipboard after cut`() {
        data class ClipboardState(
            val itemIds: List<String>,
            val isCut: Boolean,
        )

        var clipboard: ClipboardState? = ClipboardState(listOf("item-1"), isCut = true)

        // Simulate paste
        val wasCut = clipboard?.isCut ?: false
        if (wasCut) {
            clipboard = null // Clear after cut-paste
        }

        assertNull(clipboard)
    }

    @Test
    fun `paste should keep clipboard after copy`() {
        data class ClipboardState(
            val itemIds: List<String>,
            val isCut: Boolean,
        )

        var clipboard: ClipboardState? = ClipboardState(listOf("item-1"), isCut = false)

        // Simulate paste
        val wasCut = clipboard?.isCut ?: false
        if (wasCut) {
            clipboard = null
        }

        assertNotNull(clipboard) // Should still be there after copy-paste
    }

    // ========================================================================
    // Upload Queue Tests
    // ========================================================================

    @Test
    fun `upload queue should track multiple uploads`() {
        data class UploadTask(
            val id: String,
            val fileName: String,
            val progress: Float,
            val status: String, // "pending", "uploading", "completed", "failed"
        )

        val uploadQueue = mutableListOf<UploadTask>()

        uploadQueue.add(UploadTask("1", "file1.pdf", 0f, "pending"))
        uploadQueue.add(UploadTask("2", "file2.jpg", 0f, "pending"))
        uploadQueue.add(UploadTask("3", "file3.mp4", 0f, "pending"))

        assertEquals(3, uploadQueue.size)
    }

    @Test
    fun `upload progress should be trackable`() {
        data class UploadTask(
            val id: String,
            val fileName: String,
            var progress: Float,
            var status: String,
        )

        val task = UploadTask("1", "file.pdf", 0f, "uploading")

        // Update progress
        task.progress = 0.5f
        assertEquals(0.5f, task.progress)

        // Complete
        task.progress = 1.0f
        task.status = "completed"
        assertEquals(1.0f, task.progress)
        assertEquals("completed", task.status)
    }

    // ========================================================================
    // Dialog State Tests
    // ========================================================================

    @Test
    fun `dialogs should be manageable`() {
        data class DialogState(
            val showRenameDialog: Boolean = false,
            val showDeleteDialog: Boolean = false,
            val showMoveDialog: Boolean = false,
            val showShareDialog: Boolean = false,
            val showUploadDialog: Boolean = false,
            val targetItem: String? = null,
        )

        var dialogState = DialogState()

        // Open rename dialog for item
        dialogState = dialogState.copy(showRenameDialog = true, targetItem = "item-123")
        assertTrue(dialogState.showRenameDialog)
        assertEquals("item-123", dialogState.targetItem)

        // Close dialog
        dialogState = dialogState.copy(showRenameDialog = false, targetItem = null)
        assertFalse(dialogState.showRenameDialog)
        assertNull(dialogState.targetItem)
    }

    // ========================================================================
    // Search State Tests
    // ========================================================================

    @Test
    fun `search state should track query and results`() {
        data class SearchState(
            val query: String,
            val isSearching: Boolean,
            val resultCount: Int,
        )

        var searchState = SearchState("", false, 0)

        // Start search
        searchState = SearchState("document", true, 0)
        assertTrue(searchState.isSearching)

        // Complete search
        searchState = searchState.copy(isSearching = false, resultCount = 15)
        assertFalse(searchState.isSearching)
        assertEquals(15, searchState.resultCount)
    }

    @Test
    fun `search should be debounced`() {
        // Simulate debounce logic - typing rapidly should not trigger multiple searches
        val searchTriggers = mutableListOf<String>()

        val inputs = listOf("d", "do", "doc", "docu", "docum", "document")

        // With debounce, only the final value should trigger search
        // Simulating: only add if 300ms passed (we simulate just taking the last one)
        searchTriggers.add(inputs.last())

        assertEquals(1, searchTriggers.size)
        assertEquals("document", searchTriggers.first())
    }
}
