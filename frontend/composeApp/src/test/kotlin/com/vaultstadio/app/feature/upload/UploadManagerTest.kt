/**
 * Unit tests for UploadManager: upload destination and minimized state.
 * Repository is not exercised; only state setters and getters are tested.
 */

package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadManagerDestinationTest {

    @Test
    fun `setUploadDestination and getCurrentDestinationFolderId update destination`() =
        ViewModelTestBase.withMainDispatcher {
            val manager = UploadManager(StubStorageRepository())
            assertEquals(null, manager.getCurrentDestinationFolderId())
            manager.setUploadDestination("folder-1")
            assertEquals("folder-1", manager.getCurrentDestinationFolderId())
            manager.setUploadDestination(null)
            assertEquals(null, manager.getCurrentDestinationFolderId())
        }
}

class UploadManagerMinimizedTest {

    @Test
    fun `setMinimized updates isMinimized state`() = ViewModelTestBase.runTestWithMain {
        val manager = UploadManager(StubStorageRepository())
        assertFalse(manager.isMinimized.first())
        manager.setMinimized(true)
        assertTrue(manager.isMinimized.first())
        manager.setMinimized(false)
        assertFalse(manager.isMinimized.first())
    }
}
