@file:OptIn(ExperimentalComposeUiApi::class)

package com.vaultstadio.app.accessibility

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.vaultstadio.app.core.form.AccessibleBasicTextField
import com.vaultstadio.app.core.form.AccessibleTextField
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.w3c.dom.END
import org.w3c.dom.EventInit
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.SelectionMode
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class BrowserTextFieldTest {
    @Test
    fun sequentialEditsKeepTheControlledValueAndCaret() = runTest(timeout = 45.seconds) {
        val value = mutableStateOf("")
        val viewport = viewport {
            AccessibleTextField(value.value, { value.value = it }, "test.name", "Name", singleLine = true)
        }
        try {
            val input = awaitValue {
                (viewport.querySelector("input") as? HTMLInputElement)?.takeIf { it.getBoundingClientRect().width > 0 }
            }
            input.focus()
            var expected = ""
            for (chunk in listOf("a", "ñ", "🎵")) {
                input.setRangeText(chunk, input.selectionStart!!, input.selectionEnd!!, SelectionMode.END)
                input.dispatchEvent(Event("input", EventInit(bubbles = true)))
                expected += chunk
                afterTwoFrames()
                afterTwoFrames()
                assertEquals(expected, value.value)
                assertEquals(expected, input.value)
                assertEquals(expected.length, input.selectionStart)
            }
        } finally {
            viewport.remove()
        }
    }

    @Test
    fun productionPlainEditorRetainsMultilineUnicodeAndSelection() = runTest(timeout = 45.seconds) {
        val value = mutableStateOf("first\nsecond")
        val viewport = viewport {
            AccessibleBasicTextField(
                value.value,
                { value.value = it },
                "test.document",
                "Document content",
                modifier = Modifier.fillMaxSize(),
            )
        }
        try {
            val textarea = awaitValue {
                (viewport.querySelector("textarea") as? HTMLTextAreaElement)?.takeIf {
                    it.getBoundingClientRect().height >
                        0
                }
            }
            textarea.focus()
            val replacement = "ñ\n漢字 🎵"
            textarea.setRangeText(replacement, 0, textarea.value.length, SelectionMode.END)
            textarea.dispatchEvent(Event("input", EventInit(bubbles = true)))
            afterTwoFrames()
            afterTwoFrames()
            assertEquals(replacement, value.value)
            assertEquals(replacement, textarea.value)
            assertEquals(replacement.length, textarea.selectionStart)
        } finally {
            viewport.remove()
        }
    }

    private fun viewport(content: @Composable () -> Unit): HTMLElement {
        val host = document.createElement("div") as HTMLElement
        host.style.width = "800px"
        host.style.height = "600px"
        document.body!!.appendChild(host)
        ComposeViewport(host, configure = { fontFallbackUrl = null }) { MaterialTheme { content() } }
        return host
    }

    private suspend fun afterTwoFrames(): Unit = suspendCoroutine { continuation ->
        window.requestAnimationFrame { window.requestAnimationFrame { continuation.resume(Unit) } }
    }

    private suspend fun <T : Any> awaitValue(read: () -> T?): T = withContext(Dispatchers.Default) {
        withTimeout(10.seconds) {
            var value = read()
            while (value == null) {
                delay(25)
                value = read()
            }
            checkNotNull(value)
        }
    }
}
