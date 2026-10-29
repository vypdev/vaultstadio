@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.boot

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny

internal suspend fun readBrowserConfiguration(
    fetcher: JsAny? = nativeConfigurationFetch(),
    deadlineMs: Int = 5000,
): String {
    require(deadlineMs in 1..5000)
    return suspendCancellableCoroutine { continuation ->
        if (!continuation.isActive) return@suspendCancellableCoroutine
        if (fetcher == null) {
            continuation.resumeWithException(WebConfigurationLoadException())
        } else {
            val handle = beginConfigurationFetch(fetcher, deadlineMs, {
                if (continuation.isActive) continuation.resume(it.toString())
            }, {
                if (continuation.isActive) continuation.resumeWithException(WebConfigurationLoadException())
            })
            continuation.invokeOnCancellation { cancelConfigurationFetch(handle) }
        }
    }
}

private fun nativeConfigurationFetch(): JsAny? = js(
    "typeof window.fetch === 'function' ? window.fetch.bind(window) : null",
)
private fun cancelConfigurationFetch(handle: JsAny): Unit = js("handle.cancel()")
