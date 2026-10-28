@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Controlled chooser activation; actual inputs/FileList/File/FileReader, not real OS chooser evidence. */
class Browser121DialogLifecycleTest {
    @Test fun folder121DialogCancelRemovesOwnedInput() = runTest {
        withDialog("cancel") {
            val before = document.querySelectorAll("input[type=file]").length
            assertEquals(emptyList(), openFolderPicker())
            assertEquals(before, document.querySelectorAll("input[type=file]").length)
            assertTrue(capturedInputRemoved())
        }
    }

    @Test fun folder121DialogEmptyChangeDoesNotInventFiles() = runTest {
        withDialog("empty") {
            assertEquals(emptyList(), openFolderPicker())
            assertTrue(capturedInputRemoved())
        }
    }

    @Test fun folder121DialogDuplicateEventsSettleOnlyOnce() = runTest {
        withDialog("duplicate") {
            assertEquals("root/a.bin", openFolderPicker().single().relativePath)
            assertTrue(capturedInputRemoved())
        }
    }

    @Test fun folder121DialogCoroutineCancellationCleansInputAndLateEvents() = runTest {
        withDialog("hold") {
            val selected = async(start = CoroutineStart.UNDISPATCHED) { openFolderPicker() }
            assertFalse(capturedInputRemoved())
            selected.cancelAndJoin()
            assertTrue(capturedInputRemoved())
            sendLateDialogEvents()
            assertTrue(selected.isCancelled)
        }
    }

    @Test fun folder121DialogExpectedActivationRefusalIsBounded() = runTest {
        withDialog("refused") {
            assertEquals(
                FolderSelectionProblem.UNSUPPORTED,
                assertFailsWith<FolderSelectionException> {
                    openFolderPicker()
                }.problem,
            )
            assertTrue(capturedInputRemoved())
        }
    }

    @Test fun folder121DialogUnexpectedFatalActivationIsNotMappedToCapability() = runTest {
        withDialog("fatal") {
            val error = assertFailsWith<Throwable> { openFolderPicker() }
            assertFalse(error is FolderSelectionException)
            assertTrue(error.toString().contains("owned fatal activation"))
            assertTrue(capturedInputRemoved())
        }
    }

    @Test fun folder121DialogUnsupportedCapabilityNeverFallsBackToFiles() = runTest {
        withDialog("unsupported") {
            assertFalse(isFolderPickerAvailable())
            assertEquals(
                FolderSelectionProblem.UNSUPPORTED,
                assertFailsWith<FolderSelectionException> {
                    openFolderPicker()
                }.problem,
            )
            assertEquals(0, dialogClicks())
        }
    }

    @Test fun folder121DialogRejectsExcessFileCountBeforeAnyRead() = runTest {
        withDialog("too-many") {
            assertEquals(
                FolderSelectionProblem.TOO_MANY_ENTRIES,
                assertFailsWith<FolderSelectionException> {
                    openFolderPicker()
                }.problem,
            )
            assertEquals(0, dialogReads())
            assertTrue(capturedInputRemoved())
        }
    }

    @Test fun folder121DialogFreshSelectionAfterCancelStillReads() = runTest {
        withDialog("cancel") {
            assertTrue(openFolderPicker().isEmpty())
            setDialogMode("files")
            assertContentEquals(byteArrayOf(1, 2, 3), openFolderPicker().single().data)
            assertEquals(2, dialogClicks())
            assertTrue(capturedInputRemoved())
        }
    }

    @Test fun folder121OrdinarySinglePickerPreservesModeAcceptAndBytes() = runTest {
        withDialog("files") {
            val selected = openFilePicker(multiple = false, accept = "image/*").single()
            assertFalse(capturedMultiple())
            assertFalse(capturedDirectory())
            assertEquals("image/*", capturedAccept())
            assertContentEquals(byteArrayOf(1, 2, 3), selected.data)
        }
    }

    @Test fun folder121OrdinaryMultiplePickerPreservesOrderAndEmptyFile() = runTest {
        withDialog("multiple") {
            val selected = openFilePicker(multiple = true)
            assertTrue(capturedMultiple())
            assertEquals(listOf("a.bin", "empty.txt"), selected.map { it.name })
            assertContentEquals(byteArrayOf(), selected.last().data)
        }
    }

    @Test fun folder121OrdinaryOversizedMetadataStillRequiresLargeFilePath() = runTest {
        withDialog("oversize") {
            val selected = openFilePicker().single()
            assertEquals(LARGE_FILE_THRESHOLD + 1, selected.size)
            assertEquals(0, selected.data.size)
            assertEquals(0, dialogReads())
        }
    }

    @Test fun folder121LargePickerDefersReadsAndPreservesChunkRange() = runTest {
        withDialog("files") {
            val selected = openLargeFilePicker().single()
            assertEquals(0, dialogReads())
            assertContentEquals(byteArrayOf(2, 3), selected.readChunk(1, 3))
            assertEquals(1, dialogReads())
            assertEquals("application/octet-stream", selected.mimeType)
        }
    }

    @Test fun folder121LargePickerRejectsInvalidChunkRangeBeforeRead() = runTest {
        withDialog("files") {
            val selected = openLargeFilePicker().single()
            for ((start, end) in listOf(-1L to 1L, 2L to 1L, 0L to 4L)) {
                assertFailsWith<FolderSelectionException> { selected.readChunk(start, end) }
            }
            assertEquals(0, dialogReads())
        }
    }

    @Test fun folder121LargeQueueSourceRetainsDeferredBytePort() = runTest {
        withDialog("files") {
            val entry = pickLargeFilesForUpload().single() as UploadQueueEntry.Chunked
            assertEquals(0, dialogReads())
            assertContentEquals(byteArrayOf(1, 2, 3), entry.source.readChunk(0, 3))
        }
    }

    private suspend fun withDialog(mode: String, block: suspend () -> Unit) {
        installDialog(mode)
        try {
            block()
        } finally {
            restoreDialog()
        }
    }
}

private fun installDialog(mode: String): Unit = js(
    """{
    const state = {mode, click:HTMLInputElement.prototype.click, read:FileReader.prototype.readAsArrayBuffer,
        descriptor:Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'webkitdirectory'), clicks:0, reads:0};
    globalThis.__folderDialog121 = state;
    if (mode === 'unsupported') delete HTMLInputElement.prototype.webkitdirectory;
    FileReader.prototype.readAsArrayBuffer = function(blob) { state.reads++; return state.read.call(this,blob); };
    HTMLInputElement.prototype.click = function() {
        state.input=this; state.clicks++;
        if(state.mode==='hold') return;
        if(state.mode==='refused') throw new DOMException('owned activation refusal','NotAllowedError');
        if(state.mode==='fatal') throw new Error('owned fatal activation');
        if(state.mode==='cancel') {this.dispatchEvent(new Event('cancel'));return;}
        const transfer=new DataTransfer();
        if(state.mode!=='empty') {
            const count=state.mode==='too-many'?257:1;
            for(let i=0;i<count;i++) {
                const file=new File([new Uint8Array([1,2,3])],count===1?'a.bin':'a'+i+'.bin',{type:'application/octet-stream'});
                Object.defineProperty(file,'webkitRelativePath',{value:'root/'+file.name});
                if(state.mode==='oversize') Object.defineProperty(file,'size',{value:104857601});
                transfer.items.add(file);
            }
            if(state.mode==='multiple') transfer.items.add(new File([],'empty.txt',{type:'text/plain'}));
        }
        this.files=transfer.files;this.dispatchEvent(new Event('change'));
        if(state.mode==='duplicate') {this.dispatchEvent(new Event('change'));this.dispatchEvent(new Event('cancel'));}
    };
}""",
)

private fun restoreDialog(): Unit = js(
    """{
    const s=globalThis.__folderDialog121;
    HTMLInputElement.prototype.click=s.click;FileReader.prototype.readAsArrayBuffer=s.read;
    if(s.descriptor) Object.defineProperty(HTMLInputElement.prototype,'webkitdirectory',s.descriptor);
    if(s.input) s.input.remove();delete globalThis.__folderDialog121;
}""",
)
private fun setDialogMode(mode: String): Unit = js("{globalThis.__folderDialog121.mode=mode;}")
private fun sendLateDialogEvents(): Unit = js(
    "{const i=globalThis.__folderDialog121.input;i.dispatchEvent(new Event('change'));i.dispatchEvent(new Event('cancel'));}",
)
private fun capturedInputRemoved(): Boolean = js("!globalThis.__folderDialog121.input.isConnected")
private fun dialogClicks(): Int = js("globalThis.__folderDialog121.clicks")
private fun dialogReads(): Int = js("globalThis.__folderDialog121.reads")
private fun capturedMultiple(): Boolean = js("globalThis.__folderDialog121.input.multiple")
private fun capturedDirectory(): Boolean = js("globalThis.__folderDialog121.input.hasAttribute('webkitdirectory')")
private fun capturedAccept(): String = js("globalThis.__folderDialog121.input.accept")
