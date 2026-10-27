@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.storage.transfer.DownloadPayload
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Real DOM/Blob/URL adapter with the final anchor click intercepted; not rendered end-to-end evidence. */
class Browser78DownloadTest {
    @Test fun transfer78BrowserBuildsBinaryBlobWithExactBytesAndMime() = runTest {
        withSaver { saver ->
            assertEquals(SaveOutcome.HANDED_TO_BROWSER, saver.save(payload(byteArrayOf(0, -1, 127))))
            assertEquals("[0,255,127]", capturedBytes().await<JsString>().toString())
            assertEquals("application/octet-stream", capturedMime())
            assertEquals(1, clicks())
        }
    }

    @Test fun transfer78BrowserHandsOffAnActualEmptyBlob() = runTest {
        withSaver { saver ->
            assertEquals(SaveOutcome.HANDED_TO_BROWSER, saver.save(payload(byteArrayOf())))
            assertEquals("[]", capturedBytes().await<JsString>().toString())
        }
    }

    @Test fun transfer78BrowserPreservesUnicodeFilenameOnAnchor() = runTest {
        withSaver { saver ->
            saver.save(DownloadPayload("música ñ 日本語.bin", byteArrayOf(1), "test/binary"))
            assertEquals("música ñ 日本語.bin", capturedName())
        }
    }

    @Test fun transfer78BrowserRemovesTemporaryAnchorImmediately() = runTest {
        val before = document.querySelectorAll("a").length
        withSaver { saver ->
            saver.save(payload())
            assertEquals(before, document.querySelectorAll("a").length)
            assertEquals(0, revocations())
        }
    }

    @Test fun transfer78BrowserCloseRevokesEveryPendingUrlOnce() = runTest {
        withSaver { saver ->
            saver.save(payload())
            saver.save(payload())
            assertEquals(0, revocations())
            saver.close()
            saver.close()
            assertEquals(2, revocations())
        }
    }

    @Test fun transfer78BrowserGraceTimerRevokesUrlAndCloseDoesNotRevokeTwice() = runTest {
        withSaver { saver ->
            saver.save(payload())
            suspendCoroutine { continuation ->
                window.setTimeout({ continuation.resume(Unit); null }, 1200)
            }
            assertEquals(1, revocations())
            saver.close()
            assertEquals(1, revocations())
        }
    }

    @Test fun transfer78BrowserFailedClickRemovesAnchorAndRevokesUrl() = runTest {
        val before = document.querySelectorAll("a").length
        withSaver { saver ->
            failClick()
            assertFailsWith<Throwable> { saver.save(payload()) }
            assertEquals(before, document.querySelectorAll("a").length)
            assertEquals(1, revocations())
        }
    }

    @Test fun transfer78BrowserInvalidNameCreatesNoUrlAndNoClick() = runTest {
        withSaver { saver ->
            assertFailsWith<IllegalArgumentException> {
                saver.save(DownloadPayload("../escape", byteArrayOf(1), "test/binary"))
            }
            assertEquals(0, creations())
            assertEquals(0, clicks())
        }
    }

    @Test fun transfer78BrowserClosedSaverCreatesNoUrl() = runTest {
        withSaver { saver ->
            saver.close()
            assertFailsWith<IllegalStateException> { saver.save(payload()) }
            assertEquals(0, creations())
        }
    }

    private suspend fun withSaver(block: suspend (BrowserDownloadSaver) -> Unit) {
        installCapture()
        val saver = BrowserDownloadSaver()
        try {
            block(saver)
        } finally {
            saver.close()
            restoreCapture()
        }
    }

    private fun payload(bytes: ByteArray = byteArrayOf(1)) =
        DownloadPayload("file.bin", bytes, "application/octet-stream")
}

private fun installCapture(): Unit = js("""{
    const state = {create: URL.createObjectURL, revoke: URL.revokeObjectURL,
        click: HTMLAnchorElement.prototype.click, urls: [], revoked: 0, clicks: 0, fail: false};
    globalThis.__vaultTransfer78 = state;
    URL.createObjectURL = function(blob) {
        state.blob = blob; const url = state.create.call(URL, blob); state.urls.push(url); return url;
    };
    URL.revokeObjectURL = function(url) { state.revoked++; state.revoke.call(URL, url); };
    HTMLAnchorElement.prototype.click = function() {
        state.clicks++; state.name = this.download;
        if (state.fail) throw new Error('owned click failure fixture');
    };
}""")
private fun restoreCapture(): Unit = js("""{
    const state = globalThis.__vaultTransfer78;
    URL.createObjectURL = state.create; URL.revokeObjectURL = state.revoke;
    HTMLAnchorElement.prototype.click = state.click;
    for (const url of state.urls) state.revoke.call(URL, url);
    delete globalThis.__vaultTransfer78;
}""")
private fun capturedBytes(): Promise<JsString> = js("""(async function() {
    return JSON.stringify(Array.from(new Uint8Array(await globalThis.__vaultTransfer78.blob.arrayBuffer())));
})()""")
private fun capturedMime(): String = js("globalThis.__vaultTransfer78.blob.type")
private fun capturedName(): String = js("globalThis.__vaultTransfer78.name")
private fun clicks(): Int = js("globalThis.__vaultTransfer78.clicks")
private fun creations(): Int = js("globalThis.__vaultTransfer78.urls.length")
private fun revocations(): Int = js("globalThis.__vaultTransfer78.revoked")
private fun failClick(): Unit = js("{ globalThis.__vaultTransfer78.fail = true; }")
