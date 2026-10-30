@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.share.ClipboardWriteOutcome

/** Only used when the asynchronous API is absent, never to override its denial. */
internal fun browserClipboardFallback(text: String): ClipboardWriteOutcome = when (nativeClipboardFallback(text)) {
    0 -> ClipboardWriteOutcome.COPIED
    1 -> ClipboardWriteOutcome.UNAVAILABLE
    2 -> ClipboardWriteOutcome.DENIED
    else -> ClipboardWriteOutcome.FAILED
}

private fun nativeClipboardFallback(text: String): Int = js(
    """(function() {
    if (!document.body || typeof document.execCommand !== 'function') return 1;
    if (!document.hasFocus()) return 2;
    const previous = document.activeElement;
    const start = previous?.selectionStart, end = previous?.selectionEnd, direction = previous?.selectionDirection;
    const selection = document.getSelection();
    const ranges = [];
    if (selection) for (let i=0; i<selection.rangeCount; i++) ranges.push(selection.getRangeAt(i).cloneRange());
    const field = document.createElement('textarea');
    field.dataset.vaultstadioClipboard = 'temporary';
    field.value = text;
    field.readOnly = true;
    field.setAttribute('aria-hidden','true');
    field.style.cssText = 'position:fixed;left:0;top:0;opacity:0;pointer-events:none';
    try {
        document.body.appendChild(field);
        field.focus({preventScroll:true}); field.select();
        return document.execCommand('copy') ? 0 : 3;
    } catch (error) {
        return error?.name === 'NotAllowedError' || error?.name === 'SecurityError' ? 2 : 3;
    } finally {
        field.remove();
        try {
            if (previous && previous.isConnected) previous.focus({preventScroll:true});
            if (selection) {selection.removeAllRanges();for (const range of ranges) selection.addRange(range);}
            // Restoring a DOM range can alter a native input's selection: restore its offsets last.
            if (typeof start === 'number' && typeof previous?.setSelectionRange === 'function') {
                previous.setSelectionRange(start,end,direction);
            }
        } catch (_) {}
    }
})()""",
)
