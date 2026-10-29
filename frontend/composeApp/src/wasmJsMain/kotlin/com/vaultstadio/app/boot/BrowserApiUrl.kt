@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.boot

/** WHATWG host/IP/IDNA validation after common raw-input and path checks. */
internal fun canonicalBrowserApiUrl(value: String): String? = canonicalNativeUrl(value)?.toString()

private fun canonicalNativeUrl(value: String): kotlin.js.JsString? = js(
    """(function() {
    try {
        const url = new URL(value);
        if (!['http:', 'https:'].includes(url.protocol) || !url.hostname ||
            url.username || url.password || url.search || url.hash) return null;
        return url.href;
    } catch (_) { return null; }
})()""",
)
