@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.resume
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Controlled native promises exercise the production bridge without changing a real user clipboard. */
class BrowserClipboard116Test {
    @Test fun api116NativePromiseAcknowledgesTheExactConfiguredLink() = runTest {
        val writer = clipboardProbe116("copied")
        val result = BrowserShareLinkClipboard({
            writer
        }).write("https://viewer.invalid/gateway/api/v1/share/CaseToken/view")
        assertEquals(ClipboardWriteOutcome.COPIED, result)
        assertEquals(1, clipboardCalls116(writer))
        assertEquals("https://viewer.invalid/gateway/api/v1/share/CaseToken/view", clipboardText116(writer).toString())
    }

    @Test fun api116RefusalNeverRunsAPermissionBypassingFallback() = runTest {
        for (mode in listOf("denied", "security")) {
            var fallbacks = 0
            val writer = clipboardProbe116(mode)
            val result = BrowserShareLinkClipboard({ writer }, {
                fallbacks++
                ClipboardWriteOutcome.COPIED
            }).write("link")
            assertEquals(ClipboardWriteOutcome.DENIED, result)
            assertEquals(0, fallbacks)
        }
    }

    @Test fun api116RejectedAndSynchronousNativeFailureStayExplicit() = runTest {
        for (mode in listOf("failed", "throw")) {
            assertEquals(
                ClipboardWriteOutcome.FAILED,
                BrowserShareLinkClipboard({
                    clipboardProbe116(mode)
                }).write("link"),
            )
        }
    }

    @Test fun api116AbsentAsyncApiDelegatesExactlyOnceToTheSynchronousBoundary() = runTest {
        val seen = mutableListOf<String>()
        val writer = BrowserShareLinkClipboard({ null }, {
            seen += it
            ClipboardWriteOutcome.UNAVAILABLE
        })
        assertEquals(ClipboardWriteOutcome.UNAVAILABLE, writer.write("link"))
        assertEquals(listOf("link"), seen)
    }

    @Test fun api116CancellationBeforeTheNativeMicrotaskPreventsTheWrite() = runTest {
        val writer = clipboardProbe116("pending")
        val pending = async(start = CoroutineStart.UNDISPATCHED) { BrowserShareLinkClipboard({ writer }).write("link") }
        pending.cancelAndJoin()
        clipboardTurn116()
        assertEquals(0, clipboardCalls116(writer))
        assertTrue(pending.isCancelled)
    }

    @Test fun api116LateNativeCompletionCannotResumeACancelledRequest() = runTest {
        val writer = clipboardProbe116("pending")
        val pending = async(start = CoroutineStart.UNDISPATCHED) { BrowserShareLinkClipboard({ writer }).write("link") }
        clipboardTurn116()
        assertEquals(1, clipboardCalls116(writer))
        pending.cancelAndJoin()
        clipboardResolve116(writer)
        clipboardTurn116()
        assertTrue(pending.isCancelled)
    }
}

private fun clipboardProbe116(mode: String): JsAny = js(
    """(function() {
    const state={calls:0,text:null,resolve:null};
    const writer=function(text) {
        state.calls++;state.text=text;
        if(mode==='throw')throw Error('controlled native failure');
        if(mode==='copied')return Promise.resolve();
        if(mode==='denied')return Promise.reject(new DOMException('controlled refusal','NotAllowedError'));
        if(mode==='security')return Promise.reject(new DOMException('controlled refusal','SecurityError'));
        if(mode==='failed')return Promise.reject(Error('controlled native failure'));
        return new Promise(resolve=>{state.resolve=resolve;});
    };writer.state=state;return writer;
})()""",
)
private fun clipboardCalls116(writer: JsAny): Int = js("writer.state.calls")
private fun clipboardText116(writer: JsAny): JsString = js("writer.state.text")
private fun clipboardResolve116(writer: JsAny): Unit = js("writer.state.resolve()")
private suspend fun clipboardTurn116(): Unit = suspendCancellableCoroutine { continuation ->
    clipboardSchedule116 { if (continuation.isActive) continuation.resume(Unit) }
}
private fun clipboardSchedule116(done: () -> Unit): Unit = js("{setTimeout(done,0);}")
