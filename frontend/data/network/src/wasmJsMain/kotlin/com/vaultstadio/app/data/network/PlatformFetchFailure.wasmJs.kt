package com.vaultstadio.app.data.network

import io.ktor.client.engine.js.JsError

/** Ktor's public browser fetch wrapper extends Throwable, rather than Exception. */
internal actual fun isPlatformFetchFailure(error: Throwable): Boolean =
    error is JsError || (error is Error && error.cause is JsError)
