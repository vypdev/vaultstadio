@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Real File/FileReader/DOM regression; the final chooser activation is controlled, not OS evidence. */
class Browser121DirectoryRegressionTest {
    @Test
    fun folder121UsesDirectoryInputInsteadOfOrdinaryMultiplePicker() = runTest {
        withSelection {
            openFolderPicker()
            assertTrue(capturedDirectoryMode())
        }
    }

    @Test
    fun folder121PreservesSelectedRootAndNestedUnicodePath() = runTest {
        withSelection {
            val selected = openFolderPicker().single()
            assertEquals("tree121/nested/niño 日本語.bin", selected.relativePath)
            assertEquals("niño 日本語.bin", selected.name)
        }
    }

    @Test
    fun folder121FixtureRetainsActualBrowserBinaryBytes() = runTest {
        withSelection {
            val selected = openFolderPicker().single()
            assertContentEquals(byteArrayOf(0, -1, 127, -128), selected.data)
            assertEquals(4L, selected.size)
            assertEquals("application/octet-stream", selected.mimeType)
        }
    }

    @Test
    fun folder121FixtureRetainsLegitimateEmptyFile() = runTest {
        withSelection(empty = true) {
            val selected = openFolderPicker().single()
            assertEquals(0L, selected.size)
            assertContentEquals(byteArrayOf(), selected.data)
        }
    }

    private suspend fun withSelection(empty: Boolean = false, block: suspend () -> Unit) {
        installDirectoryFixture(empty)
        try {
            block()
        } finally {
            restoreDirectoryFixture()
        }
    }
}

private fun installDirectoryFixture(empty: Boolean): Unit = js(
    """{
    const state = {click: HTMLInputElement.prototype.click, directory: false};
    globalThis.__vaultFolder121 = state;
    HTMLInputElement.prototype.click = function() {
        state.directory = this.type === 'file' && this.hasAttribute('webkitdirectory');
        const bytes = empty ? new Uint8Array(0) : new Uint8Array([0,255,127,128]);
        const file = new File([bytes], 'niño 日本語.bin', {type:'application/octet-stream'});
        Object.defineProperty(file, 'webkitRelativePath', {value:'tree121/nested/niño 日本語.bin'});
        const transfer = new DataTransfer(); transfer.items.add(file);
        this.files = transfer.files;
        this.dispatchEvent(new Event('change'));
    };
}""",
)

private fun capturedDirectoryMode(): Boolean = js("globalThis.__vaultFolder121.directory")

private fun restoreDirectoryFixture(): Unit = js(
    """{
    HTMLInputElement.prototype.click = globalThis.__vaultFolder121.click;
    delete globalThis.__vaultFolder121;
}""",
)
