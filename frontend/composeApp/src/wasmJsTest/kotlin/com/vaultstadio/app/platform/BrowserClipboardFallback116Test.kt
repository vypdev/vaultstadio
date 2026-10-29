@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import kotlin.js.JsString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Actual temporary DOM/selection restoration with a controlled execCommand, not a system clipboard write. */
class BrowserClipboardFallback116Test {
    @Test fun api116SynchronousFallbackRestoresFocusSelectionAndItsOwnedDom() {
        installClipboardFallback116("copied")
        try {
            assertEquals(
                ClipboardWriteOutcome.COPIED,
                browserClipboardFallback("https://viewer.invalid/api/v1/share/Case/view"),
            )
            assertEquals("https://viewer.invalid/api/v1/share/Case/view", fallbackText116().toString())
            assertEquals(1, fallbackCalls116())
            assertTrue(fallbackRestored116(), fallbackStatus116().toString())
        } finally {
            restoreClipboardFallback116()
        }
    }

    @Test fun api116EveryRefusedFallbackCleansUpAndKeepsItsOutcome() {
        val cases = mapOf(
            "denied" to ClipboardWriteOutcome.DENIED,
            "false" to ClipboardWriteOutcome.FAILED,
            "throw" to ClipboardWriteOutcome.FAILED,
            "unfocused" to ClipboardWriteOutcome.DENIED,
            "missing" to ClipboardWriteOutcome.UNAVAILABLE,
        )
        for ((mode, expected) in cases) {
            installClipboardFallback116(mode)
            try {
                assertEquals(expected, browserClipboardFallback("link"))
                assertTrue(fallbackRestored116(), fallbackStatus116().toString())
                assertEquals(if (mode in listOf("unfocused", "missing")) 0 else 1, fallbackCalls116())
            } finally {
                restoreClipboardFallback116()
            }
        }
    }
}

private fun installClipboardFallback116(mode: String): Unit = js(
    """{
    const state={exec:document.execCommand,focus:document.hasFocus,previous:document.activeElement,calls:0,text:null};
    const input=document.createElement('input');input.value='owned selection';document.body.appendChild(input);
    input.focus();input.setSelectionRange(1,4);state.input=input;
    document.hasFocus=()=>mode!=='unfocused';
    document.execCommand=mode==='missing'?undefined:function(command) {
        if(command!=='copy')throw Error('unexpected command');
        state.calls++;state.text=document.querySelector('[data-vaultstadio-clipboard]').value;
        if(mode==='denied')throw new DOMException('controlled refusal','NotAllowedError');
        if(mode==='throw')throw Error('controlled failure');
        return mode==='copied';
    };globalThis.__clipboardFallback116=state;
}""",
)
private fun fallbackCalls116(): Int = js("globalThis.__clipboardFallback116.calls")
private fun fallbackText116(): JsString = js("globalThis.__clipboardFallback116.text")
private fun fallbackRestored116(): Boolean = js(
    "document.querySelectorAll('[data-vaultstadio-clipboard]').length===0&&document.activeElement===globalThis.__clipboardFallback116.input&&globalThis.__clipboardFallback116.input.selectionStart===1&&globalThis.__clipboardFallback116.input.selectionEnd===4",
)
private fun restoreClipboardFallback116(): Unit = js(
    """{
    const state=globalThis.__clipboardFallback116;document.execCommand=state.exec;document.hasFocus=state.focus;
    state.input.remove();if(state.previous?.isConnected)state.previous.focus();delete globalThis.__clipboardFallback116;
}""",
)

private fun fallbackStatus116(): JsString = js(
    "JSON.stringify({temporary:document.querySelectorAll('[data-vaultstadio-clipboard]').length,focus:document.activeElement===globalThis.__clipboardFallback116.input,start:globalThis.__clipboardFallback116.input.selectionStart,end:globalThis.__clipboardFallback116.input.selectionEnd})",
)
