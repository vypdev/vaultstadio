@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.core.ui.web

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.OPEN
import org.w3c.dom.ShadowRootInit
import org.w3c.dom.ShadowRootMode
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ComposeWebInputHostTest {
    private lateinit var fixture: WebEditorFixture

    @BeforeTest fun prepare() {
        fixture = WebEditorFixture()
    }

    @AfterTest fun cleanup() {
        fixture.close()
    }

    @Test fun nestedComposeShadowHostIsResolved() {
        val host = frameworkHost(fixture.host)
        assertSame(host, composeWebInputHost(fixture.host))
    }

    @Test fun directComposeShadowHostIsResolved() {
        markFrameworkRoot(fixture.host)
        assertSame(fixture.host, composeWebInputHost(fixture.host))
    }

    @Test fun unrelatedShadowHostInsideViewportIsIgnored() {
        val unrelated = document.createElement("div") as HTMLElement
        fixture.host.appendChild(unrelated)
        unrelated.attachShadow(ShadowRootInit(ShadowRootMode.OPEN)).appendChild(document.createElement("canvas"))
        val host = frameworkHost(fixture.host)
        assertSame(host, composeWebInputHost(fixture.host))
    }

    @Test fun validFrameworkHostOutsideOwnedViewportIsExcluded() {
        val outside = WebEditorFixture()
        try {
            frameworkHost(outside.host)
            val owned = frameworkHost(fixture.host)
            assertSame(owned, composeWebInputHost(fixture.host))
        } finally {
            outside.close()
        }
    }

    @Test fun missingFrameworkHostFailsClosed() {
        assertFailsWith<IllegalStateException> { composeWebInputHost(fixture.host) }
    }

    @Test fun ambiguousFrameworkHostsFailClosed() {
        frameworkHost(fixture.host)
        frameworkHost(fixture.host)
        assertFailsWith<IllegalStateException> { composeWebInputHost(fixture.host) }
    }

    @Test fun bridgeProtectsResolvedEditorAndLeavesOutsideEditorUntouched() {
        val outside = WebEditorFixture()
        try {
            val unrelated = outside.input()
            val host = frameworkHost(fixture.host)
            val input = fixture.input()
            checkNotNull(host.shadowRoot).appendChild(input)
            fixture.bridge(editorHost = composeWebInputHost(fixture.host))
            assertEquals("password", input.type)
            assertEquals("text", unrelated.type)
        } finally {
            outside.close()
        }
    }

    private fun frameworkHost(viewport: HTMLElement): HTMLElement {
        val position = document.createElement("div") as HTMLElement
        val host = document.createElement("div") as HTMLElement
        viewport.appendChild(position)
        position.appendChild(host)
        markFrameworkRoot(host)
        return host
    }

    private fun markFrameworkRoot(host: HTMLElement) {
        val root = host.attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
        val canvas = document.createElement("canvas")
        canvas.setAttribute("role", "generic")
        root.appendChild(canvas)
        val semantics = document.createElement("div")
        semantics.id = "cmp_a11y_root"
        root.appendChild(semantics)
    }
}
