@file:OptIn(ExperimentalComposeUiApi::class)

package com.vaultstadio.app.feature.files

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeViewport
import com.vaultstadio.app.core.resources.EnglishStrings
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.w3c.dom.EventInit
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Actual production renderer and public component lifecycle; no framework backing state mutation. */
class Browser121SearchLayerTest {
    @Test fun folder121PopupReleasesNativeSearchHitTarget() = runTest(timeout = 45.seconds) {
        val h = Fixture()
        try {
            val original = h.input()
            h.obscured.value = true
            frames()
            assertNull(h.viewport.querySelector("input"), "Background native input must not cover an owned popup")
            assertFalse(original.isConnected)
        } finally {
            h.viewport.remove()
        }
    }

    @Test fun folder121PopupReopeningPreservesQueryAndRealEditing() = runTest(timeout = 45.seconds) {
        val h = Fixture()
        try {
            h.input()
            repeat(3) {
                h.obscured.value = true
                frames()
                assertNull(h.viewport.querySelector("input"))
                h.obscured.value = false
                assertEquals("consulta ñ 日本語", h.input().value)
            }
            val restored = h.input()
            restored.focus()
            restored.value = "edición 🎵"
            restored.dispatchEvent(Event("input", EventInit(bubbles = true)))
            frames()
            assertEquals("edición 🎵", h.query.value)
            assertEquals(1, h.edits)
        } finally {
            h.viewport.remove()
        }
    }

    @Test fun folder121DisposedBackgroundInputDoesNotPublishStaleEdits() = runTest(timeout = 45.seconds) {
        val h = Fixture()
        try {
            val original = h.input()
            h.obscured.value = true
            frames()
            assertFalse(original.isConnected)
            original.value = "stale edit"
            original.dispatchEvent(Event("input", EventInit(bubbles = true)))
            frames()
            assertEquals("consulta ñ 日本語", h.query.value)
            assertEquals(0, h.edits)
        } finally {
            h.viewport.remove()
        }
    }

    @Test fun folder121PopupPreservesMeasuredSlot() = runTest(timeout = 45.seconds) { measuredSlot(1f) }

    @Test fun folder121PopupPreservesMeasuredSlotWithLargerFonts() = runTest(timeout = 45.seconds) { measuredSlot(2f) }

    @Test fun folder121DefaultSearchRemainsVisibleAndEditable() = runTest(timeout = 45.seconds) {
        val h = Fixture()
        try {
            val input = h.input()
            assertTrue(input.isConnected)
            input.value = "normal edit"
            input.dispatchEvent(Event("input", EventInit(bubbles = true)))
            frames()
            assertEquals("normal edit", h.query.value)
            assertEquals(1, h.edits)
        } finally {
            h.viewport.remove()
        }
    }

    private suspend fun measuredSlot(fontScale: Float) {
        val h = Fixture(fontScale)
        try {
            h.input()
            frames()
            val height = h.height
            assertTrue(height > 0)
            h.obscured.value = true
            frames()
            assertNull(h.viewport.querySelector("input"))
            assertEquals(height, h.height)
            h.obscured.value = false
            assertNotNull(h.input())
            frames()
            assertEquals(height, h.height)
        } finally {
            h.viewport.remove()
        }
    }

    private class Fixture(fontScale: Float = 1f) {
        val query = mutableStateOf("consulta ñ 日本語")
        val obscured = mutableStateOf(false)
        var edits = 0
        var height = 0
        val viewport = (document.createElement("div") as HTMLElement).also {
            it.style.width = "800px"
            it.style.height = "600px"
            document.body!!.appendChild(it)
        }

        init {
            ComposeViewport(viewport, configure = { fontFallbackUrl = null }) {
                MaterialTheme {
                    CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                        Box(Modifier.onSizeChanged { height = it.height }) {
                            FilesSearchBar(
                                query.value, {
                                    query.value = it
                                    edits++
                                }, {}, emptyList(), {}, {}, false,
                                EnglishStrings, obscuredByPopup = obscured.value,
                            )
                        }
                    }
                }
            }
        }

        suspend fun input(): HTMLInputElement = withContext(Dispatchers.Default) {
            withTimeout(10.seconds) {
                var field = viewport.querySelector("input") as? HTMLInputElement
                while (field == null || field.getBoundingClientRect().height <= 0) {
                    delay(25)
                    field = viewport.querySelector("input") as? HTMLInputElement
                }
                field
            }
        }
    }
}

private suspend fun frames() = withContext(Dispatchers.Default) {
    repeat(4) { suspendCoroutine { continuation -> window.requestAnimationFrame { continuation.resume(Unit) } } }
}
