package com.vaultstadio.app.accessibility

import androidx.compose.ui.Modifier
import com.vaultstadio.app.core.ui.inputSemantics
import org.junit.Test
import kotlin.test.assertFailsWith

class InputSemanticsContractTest {
    @Test fun blankIdentifiersCannotProduceUnaddressableControls() {
        assertFailsWith<IllegalArgumentException> { Modifier.inputSemantics("  ", "Name") }
    }

    @Test fun blankNamesCannotProduceUnnamedControls() {
        assertFailsWith<IllegalArgumentException> { Modifier.inputSemantics("input.name", "  ") }
    }
}
