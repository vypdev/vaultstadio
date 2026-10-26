package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.browser.document
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebMultilineEditorTest {
    private lateinit var textarea: HTMLTextAreaElement
    private lateinit var editor: WebTextEditor
    private val changes = mutableListOf<String>()

    @BeforeTest fun prepare() {
        textarea = document.createElement("textarea") as HTMLTextAreaElement
        document.body!!.appendChild(textarea)
        editor = WebTextEditor(textarea, spec())
    }

    @AfterTest fun cleanup() {
        editor.close()
        textarea.remove()
    }

    @Test fun multilineUnicodeAndSurrogatePairsReachTheCallerUnchanged() {
        edit("primera ñ\n漢字 🎵\nthird")
        assertEquals(listOf("primera ñ\n漢字 🎵\nthird"), changes)
    }

    @Test fun controlledReplacementClampsTheNativeMultilineSelection() {
        textarea.setSelectionRange(2, 9, "backward")
        editor.update(spec(value = "a\nb"))
        assertEquals("a\nb", textarea.value)
        assertEquals(2, textarea.selectionStart)
        assertEquals(3, textarea.selectionEnd)
        assertEquals("backward", textarea.selectionDirection)
    }

    @Test fun readOnlyMultilineControlRejectsChanges() {
        editor.update(spec(readOnly = true))
        edit("rejected")
        assertTrue(textarea.readOnly)
        assertTrue(changes.isEmpty())
        assertEquals("first\nsecond", textarea.value)
    }

    @Test fun disabledMultilineControlRejectsChanges() {
        editor.update(spec(enabled = false))
        edit("rejected")
        assertTrue(textarea.disabled)
        assertTrue(changes.isEmpty())
        assertEquals("first\nsecond", textarea.value)
    }

    @Test fun multilineEnterKeepsItsNativeDefaultWithoutAnAction() {
        val event = KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", cancelable = true))
        textarea.dispatchEvent(event)
        assertFalse(event.defaultPrevented)
    }

    @Test fun multilineEnterKeepsItsNativeDefaultEvenWithAnImeAction() {
        var submissions = 0
        editor.update(spec(onImeAction = { submissions++ }))
        val event = KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", cancelable = true))
        textarea.dispatchEvent(event)
        assertFalse(event.defaultPrevented, "Textarea must keep native newline handling")
        assertEquals(0, submissions)
        // Synthetic keydown does not perform native insertion; check the separate input callback.
        edit("first\nsecond\nthird")
        assertEquals(listOf("first\nsecond\nthird"), changes)
    }

    @Test fun composingEnterNeverInvokesTheActionEvenWithoutItsEventFlag() {
        var submissions = 0
        editor.update(spec(onImeAction = { submissions++ }))
        textarea.dispatchEvent(Event("compositionstart"))
        textarea.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Enter")))
        assertEquals(0, submissions)
    }

    @Test fun rejectedInputReconcilesAfterTheCallerRenderTurn() = runTest {
        edit("not accepted by the caller")
        afterTwoFrames()
        assertEquals(listOf("not accepted by the caller"), changes)
        assertEquals("first\nsecond", textarea.value)
    }

    @Test fun controlledUpdatePreservesCompositionUntilItsEnd() = runTest {
        textarea.dispatchEvent(Event("compositionstart"))
        textarea.value = "編集中"
        editor.update(spec(value = "authoritative"))
        assertEquals("編集中", textarea.value)
        textarea.dispatchEvent(Event("compositionend"))
        afterTwoFrames()
        assertEquals("authoritative", textarea.value)
        assertTrue(changes.isEmpty())
    }

    @Test fun keyboardHintsDoNotRewriteTheCallersText() {
        editor.update(spec(options = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)))
        edit("mixed ñ")
        assertEquals("characters", textarea.getAttribute("autocapitalize"))
        assertEquals(listOf("mixed ñ"), changes)
    }

    private fun edit(value: String) {
        textarea.value = value
        textarea.dispatchEvent(Event("input"))
    }

    private fun spec(
        value: String = "first\nsecond",
        enabled: Boolean = true,
        readOnly: Boolean = false,
        options: KeyboardOptions = KeyboardOptions.Default,
        onImeAction: (() -> Unit)? = null,
    ) = TextFieldSpec(
        value = value,
        onValueChange = { changes += it },
        identifier = "test.multiline",
        label = "Document content",
        enabled = enabled,
        readOnly = readOnly,
        singleLine = false,
        keyboardOptions = options,
        onImeAction = onImeAction,
    )
}
