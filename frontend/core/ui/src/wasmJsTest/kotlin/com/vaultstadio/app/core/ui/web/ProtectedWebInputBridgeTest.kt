@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.core.ui.web

import kotlinx.browser.document
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtectedWebInputBridgeTest {
    private lateinit var fixture: WebEditorFixture

    @BeforeTest fun prepare() {
        fixture = WebEditorFixture()
    }

    @AfterTest fun cleanup() {
        fixture.close()
    }

    @Test fun passwordHintUsesNativePasswordType() {
        val input = fixture.input()
        fixture.bridge()
        assertEquals("password", input.type)
    }

    @Test fun protectionPreservesExistingValue() {
        val input = fixture.input()
        fixture.bridge()
        assertEquals("fixture-value", input.value)
    }

    @Test fun protectionPreservesSelectionAndDirection() {
        val input = fixture.input()
        input.setSelectionRange(1, 5, "backward")
        fixture.bridge()
        assertEquals(1, input.selectionStart)
        assertEquals(5, input.selectionEnd)
        assertEquals("backward", input.selectionDirection)
    }

    @Test fun protectedEditorStillSupportsUnicodeReplacementAtCaret() {
        val input = fixture.input()
        fixture.bridge()
        input.setSelectionRange(2, 4)
        input.setRangeText("ñ", 2, 4)
        assertEquals("fiñure-value", input.value)
        assertEquals(2, input.selectionStart)
        assertEquals(3, input.selectionEnd)
    }

    @Test fun protectionNeverStealsFocus() {
        val input = fixture.input()
        fixture.bridge()
        assertFalse(document.activeElement == input)
    }

    @Test fun protectionPreservesReadOnlyAndDisabledState() {
        val input = fixture.input()
        input.readOnly = true
        input.disabled = true
        fixture.bridge()
        assertTrue(input.readOnly)
        assertTrue(input.disabled)
        assertEquals("password", input.type)
    }

    @Test fun unrelatedPasswordHintInputIsUntouched() {
        val input = fixture.input(frameworkOwned = false)
        fixture.bridge()
        assertEquals("text", input.type)
        assertNull(input.getAttribute("aria-label"))
    }

    @Test fun ordinaryFrameworkEmailEditorIsUntouched() {
        val input = fixture.input(mode = "email")
        fixture.bridge()
        assertEquals("text", input.type)
        assertEquals("fixture-value", input.value)
    }

    @Test fun backingEditorGetsALocalizedAccessibleName() {
        val input = fixture.input()
        fixture.bridge("Contraseña")
        assertEquals("Contraseña", input.getAttribute("aria-label"))
    }

    @Test fun existingSpecificAccessibleNameIsPreserved() {
        val input = fixture.input()
        input.setAttribute("aria-label", "Current password")
        fixture.bridge()
        assertEquals("Current password", input.getAttribute("aria-label"))
    }

    @Test fun noSyntheticInputEventIsEmittedByProtection() {
        val input = fixture.input()
        var events = 0
        input.addEventListener("input", { events++ })
        fixture.bridge()
        assertEquals(0, events)
    }

    @Test fun originalInputEventListenersRemainFunctional() {
        val input = fixture.input()
        var events = 0
        input.addEventListener("input", { events++ })
        fixture.bridge()
        input.dispatchEvent(Event("input"))
        assertEquals(1, events)
    }

    @Test fun blankAccessibleNameIsRejected() {
        assertFailsWith<IllegalArgumentException> { fixture.bridge(" ") }
    }
}
