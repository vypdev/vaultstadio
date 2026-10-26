@file:OptIn(ExperimentalComposeUiApi::class)

package com.vaultstadio.app.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BrowserViewportStartupTest {
    @Test
    fun productionViewportEntryPointReplacesTheExistingLoadingContent() = runTest(timeout = 45.seconds) {
        val viewport = document.createElement("div") as HTMLElement
        viewport.id = "startup-viewport-contract"
        viewport.style.width = "800px"
        viewport.style.height = "600px"
        val loading = document.createElement("div") as HTMLElement
        loading.id = "startup-loading-contract"
        loading.textContent = "Loading VaultStadio..."
        viewport.appendChild(loading)
        document.body!!.appendChild(viewport)
        assertTrue(loading.isConnected)
        try {
            ComposeViewport(viewportContainerId = viewport.id, configure = { fontFallbackUrl = null }) {
                MaterialTheme { Text("Ready", Modifier.testTag("startup-ready-contract")) }
            }
            awaitRenderedContent(viewport)
            assertFalse(loading.isConnected, "The initialized viewport must remove the loading child")
        } finally {
            viewport.remove()
        }
    }

    private suspend fun awaitRenderedContent(viewport: HTMLElement) = withContext(Dispatchers.Default) {
        withTimeout(10.seconds) {
            while (!hasRenderedContent(viewport)) delay(25)
        }
    }

    private fun hasRenderedContent(viewport: HTMLElement): Boolean {
        val descendants = viewport.querySelectorAll("*")
        return (0 until descendants.length).any { index ->
            (descendants.item(index) as? HTMLElement)?.shadowRoot
                ?.querySelector("[id='startup-ready-contract']") != null
        }
    }
}
