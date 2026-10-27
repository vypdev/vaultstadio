package com.vaultstadio.app.data.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Preserve caller cancellation through Ktor wrappers; genuine transport deadlines stay failures. */
suspend fun rethrowTransportCancellation(error: Exception) {
    currentCoroutineContext().ensureActive()
    if (error is CancellationException) throw error
    val causes = mutableSetOf<Throwable>()
    var next: Throwable? = error
    while (next != null && causes.add(next)) next = next.cause
    // Ktor wraps its request/connection deadlines as IO failures; caller cancellation stays control flow.
    if (causes.any {
            it is HttpRequestTimeoutException ||
                it is ConnectTimeoutException ||
                it is SocketTimeoutException
        }
    ) {
        return
    }
    causes.filterIsInstance<CancellationException>().firstOrNull()?.let { throw it }
}
