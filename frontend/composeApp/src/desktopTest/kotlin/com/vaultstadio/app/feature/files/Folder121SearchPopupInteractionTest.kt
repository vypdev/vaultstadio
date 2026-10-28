package com.vaultstadio.app.feature.files

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.vaultstadio.app.core.resources.EnglishStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class Folder121SearchPopupInteractionTest {
    @get:Rule val rule = createComposeRule()
    private val query = mutableStateOf("consulta ñ 日本語")
    private val obscured = mutableStateOf(false)
    private var edits = 0

    @Test fun folder121PopupRetainsQueryAndRestoresEditableControl() {
        mount()
        rule.onNodeWithTag("files.search").assertIsDisplayed()
        rule.runOnIdle { obscured.value = true }
        rule.onNodeWithTag("files.search").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals("consulta ñ 日本語", query.value)
            obscured.value = false
        }
        rule.onNodeWithTag("files.search").performTextReplacement("restored edit 🎵")
        rule.runOnIdle {
            assertEquals("restored edit 🎵", query.value)
            assertEquals(1, edits)
        }
    }

    @Test fun folder121RepeatedPopupDoesNotEmitSpuriousQueryChanges() {
        mount()
        repeat(3) {
            rule.runOnIdle { obscured.value = true }
            rule.onNodeWithTag("files.search").assertDoesNotExist()
            rule.runOnIdle { obscured.value = false }
            rule.onNodeWithTag("files.search").assertIsDisplayed()
        }
        rule.runOnIdle {
            assertEquals(0, edits)
            assertEquals("consulta ñ 日本語", query.value)
        }
    }

    @Test fun folder121PopupPreservesMeasuredLayoutSpace() = measuredLayout(1f)

    @Test fun folder121PopupPreservesLayoutWithLargeFontScale() = measuredLayout(2f)

    private fun measuredLayout(fontScale: Float) {
        mount(fontScale)
        val before = rule.onNodeWithTag("search.layer.slot").fetchSemanticsNode().boundsInRoot.height
        rule.runOnIdle { obscured.value = true }
        rule.onNodeWithTag("files.search").assertDoesNotExist()
        val hidden = rule.onNodeWithTag("search.layer.slot").fetchSemanticsNode().boundsInRoot.height
        assertEquals(before, hidden, 0.5f)
        rule.runOnIdle { obscured.value = false }
        rule.onNodeWithTag("files.search").assertIsDisplayed()
        val restored = rule.onNodeWithTag("search.layer.slot").fetchSemanticsNode().boundsInRoot.height
        assertEquals(before, restored, 0.5f)
    }

    private fun mount(fontScale: Float = 1f) {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.testTag("search.layer.slot")) {
                        FilesSearchBar(
                            query.value, {
                                query.value = it
                                edits++
                            }, {}, emptyList(), {}, {}, false,
                            EnglishStrings, obscuredByPopup = obscured.value,
                        )
                    }
                }
            }
        }
    }
}
