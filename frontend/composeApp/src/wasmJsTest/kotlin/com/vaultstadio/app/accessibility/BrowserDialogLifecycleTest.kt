@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.vaultstadio.app.accessibility

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.ComposeViewport
import com.vaultstadio.app.feature.profile.EditProfileDialog
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.w3c.dom.HTMLElement
import org.w3c.dom.ShadowRoot
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Exercises actual exported semantics across production dialog owner creation/removal. */
class BrowserDialogLifecycleTest {
    @Test
    fun cancelRestoresBackgroundControlsAndAllowsRepeatedReopening() = runTest(timeout = 45.seconds) {
        val viewport = document.createElement("div") as HTMLElement
        viewport.style.width = "800px"
        viewport.style.height = "600px"
        document.body!!.appendChild(viewport)
        val dialogOpen = mutableStateOf(false)
        try {
            ComposeViewport(viewport, configure = { fontFallbackUrl = null }) {
                MaterialTheme {
                    Button(
                        onClick = { dialogOpen.value = true },
                        modifier = Modifier.testTag("dialog-lifecycle-open"),
                    ) { Text("Open profile editor") }
                    if (dialogOpen.value) {
                        EditProfileDialog(
                            currentUsername = "fixture-name",
                            isSaving = false,
                            onSave = { error("Cancel must never invoke save") },
                            onDismiss = { dialogOpen.value = false },
                        )
                    }
                }
            }
            val shadow = awaitValue("framework shadow root") { frameworkShadow(viewport) }
            repeat(2) {
                awaitValue("background button before opening") {
                    shadow.querySelector("[id='dialog-lifecycle-open']") as? HTMLElement
                }.click()
                val field = awaitValue("production profile field") {
                    shadow.querySelector("[id='profile.username']") as? HTMLElement
                }
                assertTrue(field.textContent.orEmpty().contains("fixture-name"))
                awaitValue("production Cancel action") {
                    val buttons = shadow.querySelectorAll("[role='button']")
                    (0 until buttons.length).mapNotNull { buttons.item(it) as? HTMLElement }
                        .singleOrNull { it.textContent?.trim() == "Cancel" }
                }.click()
                awaitValue("background semantics after dialog dismissal") {
                    (shadow.querySelector("[id='dialog-lifecycle-open']") as? HTMLElement)
                        ?.takeIf { shadow.querySelector("[id='profile.username']") == null }
                }
            }
        } finally {
            dialogOpen.value = false
            viewport.remove()
        }
    }

    private fun frameworkShadow(viewport: HTMLElement): ShadowRoot? {
        val descendants = viewport.querySelectorAll("*")
        return (0 until descendants.length).mapNotNull { index ->
            (descendants.item(index) as? HTMLElement)?.shadowRoot
        }.singleOrNull { it.querySelector("canvas[role='generic']") != null }
    }

    private suspend fun <T : Any> awaitValue(description: String, read: () -> T?): T =
        withContext(Dispatchers.Default) {
            withTimeout(10.seconds) {
                var value = read()
                while (value == null) {
                    delay(25)
                    value = read()
                }
                checkNotNull(value) { "Missing $description" }
            }
        }
}
