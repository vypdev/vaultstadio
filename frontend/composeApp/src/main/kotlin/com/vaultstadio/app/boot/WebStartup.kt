package com.vaultstadio.app.boot

import com.vaultstadio.app.data.network.webApiRootCandidate
import kotlinx.coroutines.CancellationException

/** Composition policy: no application/client effects before data and native URI validation. */
// The injected configuration reader is an IO boundary; only its failures become LOAD.
@Suppress("TooGenericExceptionCaught")
internal suspend fun startConfiguredWebApplication(
    pageOrigin: String,
    readConfiguration: suspend () -> String,
    canonicalizeNativeUrl: (String) -> String?,
    launchApplication: (String) -> Unit,
    showFailure: (WebStartupFailure) -> Unit,
) {
    val source = try {
        readConfiguration()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        showFailure(WebStartupFailure.LOAD)
        return
    }
    val candidate = webApiRootCandidate(pageOrigin, source)
    val canonical = candidate?.let(canonicalizeNativeUrl)
    if (canonical == null) showFailure(WebStartupFailure.INVALID) else launchApplication(canonical)
}
