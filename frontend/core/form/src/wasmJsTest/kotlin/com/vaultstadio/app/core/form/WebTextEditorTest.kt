package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.browser.document
import org.w3c.dom.EventInit
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebTextEditorTest {
    private lateinit var input: HTMLInputElement
    private lateinit var editor: WebTextEditor
    private val changes = mutableListOf<String>()
    private var submissions = 0

    @BeforeTest fun prepare() {
        input = document.createElement("input") as HTMLInputElement
        editor = WebTextEditor(input, spec())
        document.body!!.appendChild(input)
    }

    @AfterTest fun cleanup() {
        editor.close()
        input.remove()
    }

    @Test fun defaultIsAnActualNativePasswordInput() {
        assertEquals("password", input.type)
        assertEquals("Password", input.getAttribute("aria-label"))
        assertEquals("fixture ñ", input.value)
    }

    @Test fun explicitVisibilityCanRevealAndProtectAgain() {
        editor.update(spec(visible = true))
        assertEquals("text", input.type)
        editor.update(spec())
        assertEquals("password", input.type)
    }

    @Test fun visibilityChangePreservesSelectionAndDirection() {
        input.setSelectionRange(1, 5, "backward")
        editor.update(spec(visible = true))
        assertEquals(1, input.selectionStart)
        assertEquals(5, input.selectionEnd)
        assertEquals("backward", input.selectionDirection)
    }

    @Test fun unchangedControlledValueKeepsTheCaret() {
        input.setSelectionRange(3, 3)
        editor.update(spec())
        assertEquals(3, input.selectionStart)
    }

    @Test fun shorterControlledValueClampsSelection() {
        input.setSelectionRange(3, 7)
        editor.update(spec(value = "ab"))
        assertEquals("ab", input.value)
        assertEquals(2, input.selectionStart)
        assertEquals(2, input.selectionEnd)
    }

    @Test fun unicodeInputReachesTheProductionCallback() {
        edit("contraseña 漢字")
        assertEquals(listOf("contraseña 漢字"), changes)
    }

    @Test fun clearingReachesTheProductionCallback() {
        edit("")
        assertEquals(listOf(""), changes)
    }

    @Test fun updatesUseTheLatestCallback() {
        val fresh = mutableListOf<String>()
        editor.update(spec(onChange = { fresh += it }))
        edit("new value")
        assertTrue(changes.isEmpty())
        assertEquals(listOf("new value"), fresh)
    }

    @Test fun disabledInputCannotChangeApplicationState() {
        editor.update(spec(enabled = false))
        edit("rejected")
        assertTrue(input.disabled)
        assertTrue(changes.isEmpty())
        assertEquals("fixture ñ", input.value)
    }

    @Test fun readOnlyInputCannotChangeApplicationState() {
        editor.update(spec(readOnly = true))
        edit("rejected")
        assertTrue(input.readOnly)
        assertTrue(changes.isEmpty())
        assertEquals("fixture ñ", input.value)
    }

    @Test fun enabledStateCanBeRestoredWithoutReplacingTheEditor() {
        editor.update(spec(enabled = false))
        editor.update(spec())
        edit("accepted")
        assertFalse(input.disabled)
        assertEquals(listOf("accepted"), changes)
    }

    @Test fun localeAndValidationAreUpdated() {
        editor.update(spec(label = "Contraseña", error = true))
        assertEquals("Contraseña", input.getAttribute("aria-label"))
        assertEquals("true", input.getAttribute("aria-invalid"))
        editor.update(spec())
        assertEquals("false", input.getAttribute("aria-invalid"))
    }

    @Test fun loginUsesTheExistingCredentialAutocompleteHint() {
        assertEquals("current-password", input.getAttribute("autocomplete"))
    }

    @Test fun registrationUsesTheNewCredentialAutocompleteHint() {
        editor.update(spec(identifier = "auth.register.password"))
        assertEquals("new-password", input.getAttribute("autocomplete"))
    }

    @Test fun spellingAndAutomaticTextChangesRemainDisabled() {
        assertEquals("off", input.getAttribute("autocorrect"))
        assertEquals("off", input.getAttribute("autocapitalize"))
        assertEquals("false", input.getAttribute("spellcheck"))
    }

    @Test fun enterInvokesTheCurrentImeActionOnce() {
        input.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", cancelable = true)))
        assertEquals(1, submissions)
    }

    @Test fun missingImeCallbackRetainsNativeDefaultHandling() {
        editor.update(TextFieldSpec("fixture", {}, "test.password", "Password"))
        val event = KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", cancelable = true))
        input.dispatchEvent(event)
        assertFalse(event.defaultPrevented)
    }

    @Test fun composingModifiedAndUnrelatedKeysNeverSubmit() {
        input.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", isComposing = true)))
        input.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", ctrlKey = true)))
        input.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", shiftKey = true)))
        input.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "a")))
        assertEquals(0, submissions)
    }

    @Test fun singleLineCompositionSessionBlocksEnterWithoutTheKeyboardCompositionFlag() {
        input.dispatchEvent(Event("compositionstart"))
        val composing = KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", cancelable = true))
        input.dispatchEvent(composing)
        assertFalse(composing.defaultPrevented)
        assertEquals(0, submissions)
        input.dispatchEvent(Event("compositionend"))
        val committed = KeyboardEvent("keydown", KeyboardEventInit(key = "Enter", cancelable = true))
        input.dispatchEvent(committed)
        assertTrue(committed.defaultPrevented)
        assertEquals(1, submissions)
    }

    @Test fun disabledAndReadOnlyEditorsNeverSubmit() {
        editor.update(spec(enabled = false))
        input.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Enter")))
        editor.update(spec(readOnly = true))
        input.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Enter")))
        assertEquals(0, submissions)
    }

    @Test fun nextActionExposesItsKeyboardHint() {
        editor.update(spec(ime = ImeAction.Next))
        assertEquals("next", input.getAttribute("enterkeyhint"))
    }

    @Test fun disposalRemovesBothListenersAndCannotUnmaskTheEditor() {
        editor.close()
        editor.close()
        editor.update(spec(visible = true))
        edit("ignored after disposal")
        input.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Enter")))
        assertTrue(changes.isEmpty())
        assertEquals(0, submissions)
        assertEquals("password", input.type)
    }

    private fun edit(value: String) {
        input.value = value
        input.dispatchEvent(Event("input", EventInit(bubbles = true)))
    }

    private fun spec(
        value: String = "fixture ñ",
        identifier: String = "auth.login.password",
        label: String = "Password",
        enabled: Boolean = true,
        readOnly: Boolean = false,
        error: Boolean = false,
        visible: Boolean = false,
        ime: ImeAction = ImeAction.Done,
        onChange: (String) -> Unit = { changes += it },
    ) = TextFieldSpec(
        value = value,
        onValueChange = onChange,
        identifier = identifier,
        label = label,
        enabled = enabled,
        readOnly = readOnly,
        isError = error,
        passwordVisible = visible,
        isPassword = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = ime,
        ),
        onImeAction = { submissions++ },
    )
}
