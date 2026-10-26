@file:OptIn(ExperimentalComposeUiApi::class)

package com.vaultstadio.app.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.ComposeViewport
import com.vaultstadio.app.core.form.PasswordTextField
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.w3c.dom.END
import org.w3c.dom.EventInit
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.SelectionMode
import org.w3c.dom.events.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

/** Verifies the production widget's native DOM and Compose state/release integration. */
class BrowserPasswordFieldTest {
    @Test
    fun actualWidgetProtectsEditsAndReleasesItsNativeEditor() = runTest(timeout = 45.seconds) {
        val viewport = document.createElement("div") as HTMLElement
        viewport.style.width = "800px"
        viewport.style.height = "600px"
        document.body!!.appendChild(viewport)
        val value = mutableStateOf("fixture-value")
        val visible = mutableStateOf(false)
        val mounted = mutableStateOf(true)
        var callbacks = 0
        try {
            ComposeViewport(viewport, configure = { fontFallbackUrl = null }) {
                MaterialTheme {
                    Column {
                        if (mounted.value) {
                            PasswordTextField(
                                value.value,
                                {
                                    value.value = it
                                    callbacks++
                                },
                                "widget.password",
                                "Contraseña",
                                passwordVisible = visible.value,
                            )
                        }
                        Button(
                            onClick = { visible.value = !visible.value },
                            modifier = Modifier.testTag("widget-visibility"),
                        ) { Text("Toggle visibility") }
                    }
                }
            }
            val input = awaitValue { viewport.querySelector("input") as? HTMLInputElement }
            assertEquals("password", input.type)
            assertEquals("Contraseña", input.getAttribute("aria-label"))
            input.focus()
            input.setSelectionRange(0, input.value.length)
            input.setRangeText("edición ñ 漢字", 0, input.value.length, SelectionMode.END)
            input.dispatchEvent(Event("input", EventInit(bubbles = true)))
            awaitValue { value.value.takeIf { it == "edición ñ 漢字" } }
            assertEquals(1, callbacks)
            val toggle = awaitValue {
                val descendants = viewport.querySelectorAll("*")
                (0 until descendants.length).mapNotNull { descendants.item(it) as? HTMLElement }
                    .mapNotNull { it.shadowRoot?.querySelector("[id='widget-visibility']") as? HTMLElement }
                    .singleOrNull()
            }
            toggle.click()
            awaitValue { input.takeIf { it.type == "text" } }
            assertEquals("edición ñ 漢字", input.value)
            toggle.click()
            awaitValue { input.takeIf { it.type == "password" } }
            mounted.value = false
            awaitValue { true.takeIf { !input.isConnected } }
            assertNull(viewport.querySelector("input"))
            input.value = "detached fixture"
            input.dispatchEvent(Event("input", EventInit(bubbles = true)))
            assertEquals(1, callbacks)
        } finally {
            mounted.value = false
            viewport.remove()
        }
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
