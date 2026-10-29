@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.boot

import kotlin.js.JsAny
import kotlin.js.JsString

/** Owns a bounded UTF-8 stream, total deadline and all abort/reader/timer cleanup. */
internal fun beginConfigurationFetch(
    fetcher: JsAny,
    deadlineMs: Int,
    loaded: (JsString) -> Unit,
    failed: () -> Unit,
): JsAny = js(
    """(function() {
    if (typeof AbortController !== 'function' || typeof TextDecoder !== 'function') {
        failed(); return { cancel() {} };
    }
    const controller = new AbortController();
    const limit = 16384;
    let settled = false;
    let reader = null;
    const started = performance.now();
    function cancelStream(stream) {
        try { Promise.resolve(stream.cancel()).catch(() => {}); } catch (_) {}
    }
    function cleanup() {
        clearTimeout(timer);
        controller.abort();
        if (reader) {
            cancelStream(reader);
            try { reader.releaseLock(); } catch (_) {}
            reader = null;
        }
    }
    function failure() {
        if (settled) return;
        settled = true; cleanup(); failed();
    }
    const timer = setTimeout(failure, deadlineMs);
    (async function() {
        const response = await fetcher('/vaultstadio-config.json', {
            credentials: 'omit', cache: 'no-store', redirect: 'error', signal: controller.signal,
        });
        if (settled) { if (response.body) cancelStream(response.body); return null; }
        if (response.body && typeof response.body.getReader === 'function') reader = response.body.getReader();
        const mime = (response.headers.get('Content-Type') || '').split(';')[0].trim().toLowerCase();
        const length = response.headers.get('Content-Length');
        if (!response.ok || response.redirected || mime !== 'application/json' || !reader ||
            (length !== null && (!/^[0-9]+$/.test(length) || Number(length) > limit))) throw Error();
        const chunks = [];
        let total = 0;
        let count = 0;
        while (true) {
            const part = await reader.read();
            if (settled) return null;
            if (performance.now() - started >= deadlineMs) throw Error();
            if (part.done) break;
            if (!(part.value instanceof Uint8Array) || ++count > limit + 1) throw Error();
            total += part.value.byteLength;
            if (total > limit) throw Error();
            chunks.push(part.value);
        }
        const bytes = new Uint8Array(total);
        let offset = 0;
        for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
        const text = new TextDecoder('utf-8', { fatal: true }).decode(bytes);
        if (performance.now() - started >= deadlineMs) throw Error();
        return text;
    })().then(text => {
        if (settled) return;
        settled = true; cleanup(); loaded(text);
    }, failure);
    return { cancel() { if (!settled) { settled = true; cleanup(); } } };
})()""",
)
