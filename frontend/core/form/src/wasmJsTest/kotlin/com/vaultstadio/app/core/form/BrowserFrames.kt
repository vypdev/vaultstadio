package com.vaultstadio.app.core.form

import kotlinx.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal suspend fun afterTwoFrames(): Unit = suspendCoroutine { continuation ->
    window.requestAnimationFrame {
        window.requestAnimationFrame { continuation.resume(Unit) }
    }
}
