@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import kotlin.js.JsAny

/** A native write cannot be aborted; cancellation only releases callbacks and suppresses late UI success. */
internal fun beginClipboardWrite(
    writer: JsAny,
    text: String,
    copied: () -> Unit,
    denied: () -> Unit,
    failed: () -> Unit,
): JsAny = js(
    """(function() {
    let active = true;
    let onCopy = copied, onDenied = denied, onFailure = failed;
    function clear() { active = false; onCopy = onDenied = onFailure = null; }
    function finish(callback) { if (!active) return; clear(); callback(); }
    Promise.resolve().then(() => {
        if (active) return writer(text);
    }).then(() => {
        if (active) finish(onCopy);
    }, error => {
        if (!active) return;
        finish(error?.name === 'NotAllowedError' || error?.name === 'SecurityError' ? onDenied : onFailure);
    });
    return { cancel: clear };
})()""",
)
