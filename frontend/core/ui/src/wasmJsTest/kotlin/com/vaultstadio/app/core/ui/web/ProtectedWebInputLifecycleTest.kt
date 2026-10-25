@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.core.ui.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.w3c.dom.OPEN
import org.w3c.dom.ShadowRootInit
import org.w3c.dom.ShadowRootMode
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtectedWebInputLifecycleTest {
    private lateinit var fixture: WebEditorFixture

    @BeforeTest fun prepare() {
        fixture = WebEditorFixture()
    }

    @AfterTest fun cleanup() {
        fixture.close()
    }

    @Test fun newlyInsertedBackingEditorIsProtectedByObserver() = runTest {
        fixture.bridge()
        val input = fixture.input()
        withContext(Dispatchers.Default) { delay(1) }
        assertEquals("password", input.type)
    }

    @Test fun changedPasswordHintIsProtectedByObserver() = runTest {
        val input = fixture.input(mode = "text")
        fixture.bridge()
        input.setAttribute("inputmode", "password")
        withContext(Dispatchers.Default) { delay(1) }
        assertEquals("password", input.type)
    }

    @Test fun focusEventProtectsEditorSynchronously() {
        fixture.bridge()
        val input = fixture.input()
        input.focus()
        assertEquals("password", input.type)
    }

    @Test fun disposalNeverUnmasksAConnectedPasswordBuffer() {
        val input = fixture.input()
        fixture.bridge().close()
        assertEquals("password", input.type)
        assertEquals("fixture-value", input.value)
    }

    @Test fun closedObserverCannotMutateNewEditors() = runTest {
        fixture.bridge().close()
        val input = fixture.input()
        input.focus()
        withContext(Dispatchers.Default) { delay(1) }
        assertEquals("text", input.type)
    }

    @Test fun staleHintCannotUnmaskAnExistingPasswordBuffer() {
        val input = fixture.input()
        val bridge = fixture.bridge()
        input.setAttribute("inputmode", "text")
        bridge.refresh()
        assertEquals("password", input.type)
    }

    @Test fun repeatedCloseIsHarmlessAndKeepsProtection() {
        val input = fixture.input()
        val bridge = fixture.bridge()
        bridge.close()
        bridge.close()
        assertEquals("password", input.type)
    }

    @Test fun remountCanUpdateTheAdaptersLocalizedName() {
        val input = fixture.input()
        fixture.bridge("Password").close()
        fixture.bridge("Contraseña")
        assertEquals("Contraseña", input.getAttribute("aria-label"))
        assertEquals("password", input.type)
    }

    @Test fun upstreamSpecificNameTakesOwnershipOfItsLabel() {
        val input = fixture.input()
        val bridge = fixture.bridge()
        input.setAttribute("aria-label", "New password")
        bridge.refresh()
        assertEquals("New password", input.getAttribute("aria-label"))
    }

    @Test fun lateShadowRootUsesTheNewEditorScope() {
        fixture.bridge()
        val input = fixture.input()
        val shadow = fixture.host.attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
        shadow.appendChild(input)
        input.focus()
        assertEquals("password", input.type)
        assertTrue(input.matches(":focus"))
    }
}
