@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderSelectionException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.w3c.files.Blob
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Actual browser FileReader; public callback faults remain controlled adapter evidence. */
class Browser121FileReaderTest {
    @Test fun folder121BrowserReaderPreservesBytesAndDetachesCallbacks() = runTest {
        withReader("normal") {
            assertContentEquals(byteArrayOf(0, -1, 127, -128), readBrowserBlob(readerBlob(), 4))
            assertTrue(readerHandlersCleared())
        }
    }

    @Test fun folder121BrowserReaderAcceptsRealEmptyBlob() = runTest {
        withReader("normal") {
            assertContentEquals(byteArrayOf(), readBrowserBlob(emptyReaderBlob(), 0))
            assertTrue(readerHandlersCleared())
        }
    }

    @Test fun folder121BrowserReaderCancellationAbortsAndDetachesBeforeAbort() = runTest {
        withReader("normal") {
            val job = async(start = CoroutineStart.UNDISPATCHED) { readBrowserBlob(readerBlob(), 4) }
            job.cancelAndJoin()
            assertEquals(1, readerAborts())
            assertTrue(readerHandlersCleared())
            assertTrue(detachedBeforeAbort())
            sendLateReaderEvents()
            assertTrue(job.isCancelled)
        }
    }

    @Test fun folder121BrowserReaderErrorNeverBecomesEmptySuccess() = runTest {
        withReader("error") {
            assertFailsWith<FolderSelectionException> { readBrowserBlob(readerBlob(), 4) }
            assertTrue(readerHandlersCleared())
        }
    }

    @Test fun folder121BrowserReaderNullResultIsRejected() = runTest {
        withReader("null") {
            assertFailsWith<FolderSelectionException> { readBrowserBlob(readerBlob(), 4) }
            assertTrue(readerHandlersCleared())
        }
    }

    @Test fun folder121BrowserReaderTextResultIsRejected() = runTest {
        withReader("text") {
            assertFailsWith<FolderSelectionException> { readBrowserBlob(readerBlob(), 4) }
            assertTrue(readerHandlersCleared())
        }
    }

    @Test fun folder121BrowserReaderRejectsOversizedBlobBeforeStartingIO() = runTest {
        withReader("normal") {
            assertFailsWith<FolderSelectionException> { readBrowserBlob(readerBlob(), 3) }
            assertEquals(0, readerStarts())
        }
    }

    @Test fun folder121BrowserReaderRejectsInvalidBudgetBeforeIO() = runTest {
        withReader("normal") {
            assertFailsWith<FolderSelectionException> { readBrowserBlob(readerBlob(), -1) }
            assertFailsWith<FolderSelectionException> { readBrowserBlob(readerBlob(), Int.MAX_VALUE.toLong() + 1) }
            assertEquals(0, readerStarts())
        }
    }

    @Test fun folder121BrowserReaderSynchronousDOMFailureCleansHandlers() = runTest {
        withReader("refused") {
            assertFailsWith<FolderSelectionException> { readBrowserBlob(readerBlob(), 4) }
            assertTrue(readerHandlersCleared())
        }
    }

    @Test fun folder121BrowserReaderSynchronousFatalFailureKeepsItsMeaning() = runTest {
        withReader("fatal") {
            val error = assertFailsWith<Throwable> { readBrowserBlob(readerBlob(), 4) }
            assertFalse(error is FolderSelectionException)
            assertTrue(error.toString().contains("owned fatal read"))
            assertTrue(readerHandlersCleared())
        }
    }

    @Test fun folder121BrowserReaderDuplicateCompletionSettlesOnlyOnce() = runTest {
        withReader("duplicate") {
            assertContentEquals(byteArrayOf(0, -1, 127, -128), readBrowserBlob(readerBlob(), 4))
            assertTrue(readerHandlersCleared())
        }
    }

    private suspend fun withReader(mode: String, block: suspend () -> Unit) {
        installReaderCapture(mode)
        try {
            block()
        } finally {
            restoreReaderCapture()
        }
    }
}

private fun readerBlob(): Blob = js("new Blob([new Uint8Array([0,255,127,128])],{type:'application/octet-stream'})")
private fun emptyReaderBlob(): Blob = js("new Blob([])")
private fun installReaderCapture(mode: String): Unit = js(
    """{
    const s={read:FileReader.prototype.readAsArrayBuffer,abort:FileReader.prototype.abort,mode,starts:0,aborts:0};
    globalThis.__folderReader121=s;
    FileReader.prototype.readAsArrayBuffer=function(blob) {
        s.reader=this;s.starts++;s.load=this.onload;s.error=this.onerror;s.onabort=this.onabort;
        if(s.mode==='refused') throw new DOMException('owned read refusal','NotReadableError');
        if(s.mode==='fatal') throw new Error('owned fatal read');
        if(s.mode==='error') {this.dispatchEvent(new ProgressEvent('error'));return;}
        if(s.mode==='null') {this.dispatchEvent(new ProgressEvent('load'));return;}
        if(s.mode==='text') {this.readAsText(blob);return;}
        if(s.mode==='duplicate') {const load=this.onload;this.onload=function(event){load.call(this,event);load.call(this,event);};}
        return s.read.call(this,blob);
    };
    FileReader.prototype.abort=function() {
        s.aborts++;s.detached=this.onload===null&&this.onerror===null&&this.onabort===null;
        return s.abort.call(this);
    };
}""",
)
private fun restoreReaderCapture(): Unit = js(
    """{
    const s=globalThis.__folderReader121;
    FileReader.prototype.readAsArrayBuffer=s.read;FileReader.prototype.abort=s.abort;
    if(s.reader&&s.reader.readyState===1) {s.reader.onload=null;s.reader.onerror=null;s.reader.onabort=null;s.abort.call(s.reader);}
    delete globalThis.__folderReader121;
}""",
)
private fun readerHandlersCleared(): Boolean = js(
    "!globalThis.__folderReader121.reader || (globalThis.__folderReader121.reader.onload===null&&globalThis.__folderReader121.reader.onerror===null&&globalThis.__folderReader121.reader.onabort===null)",
)
private fun readerStarts(): Int = js("globalThis.__folderReader121.starts")
private fun readerAborts(): Int = js("globalThis.__folderReader121.aborts")
private fun detachedBeforeAbort(): Boolean = js("globalThis.__folderReader121.detached===true")
private fun sendLateReaderEvents(): Unit = js(
    "{const s=globalThis.__folderReader121;s.load.call(s.reader,new ProgressEvent('load'));s.error.call(s.reader,new ProgressEvent('error'));s.onabort.call(s.reader,new ProgressEvent('abort'));}",
)
