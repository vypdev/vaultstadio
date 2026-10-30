package com.vaultstadio.app.boot

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowserConfigurationErrorTest {
    @Test fun api116FailureReplacesLoaderWithAccessibleSafeCopyAndNativeReload() {
        for (failure in WebStartupFailure.entries) {
            val target = document.createElement("div")
            target.id = "api116-owned-error"
            target.textContent = "old loader"
            document.body!!.appendChild(target)
            var reloads = 0
            try {
                showWebConfigurationError(failure, target.id) { reloads++ }
                assertEquals("VaultStadio cannot start", target.querySelector("h1")?.textContent)
                assertTrue(target.querySelector("[role=alert]")!!.textContent!!.contains("operator"))
                assertEquals(1, target.querySelectorAll("button").length)
                val reload = target.querySelector("button") as HTMLElement
                assertEquals("Reload", reload.textContent)
                reload.click()
                assertEquals(1, reloads)
                assertTrue(!target.textContent!!.contains("old loader"))
            } finally {
                target.remove()
            }
        }
    }
}
