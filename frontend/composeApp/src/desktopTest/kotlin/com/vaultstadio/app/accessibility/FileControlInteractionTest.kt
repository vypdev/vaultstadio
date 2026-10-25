package com.vaultstadio.app.accessibility

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
import com.vaultstadio.app.ui.components.files.FileGridItem
import com.vaultstadio.app.ui.components.files.SelectableFileGridItem
import com.vaultstadio.app.ui.components.files.SelectableFileListItem
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class FileControlInteractionTest {
    @get:Rule val rule = createComposeRule()
    private val item = StorageItem(
        "file-test", "Archivo ñ.txt", "/Archivo ñ.txt", ItemType.FILE, null, 1, "text/plain",
        Visibility.PRIVATE, false, false, Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(0),
    )
    private val itemButton get() = rule.onNode(
        hasText(item.name) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
    )

    @Test fun listItemActivatesItsNormalOpenCallbackOnce() {
        var opens = 0
        rule.setContent { SelectableFileListItem(item, false, false, { opens++ }, {}, {}, {}) }
        itemButton.performClick()
        assertEquals(1, opens)
    }

    @Test fun listLongPressInvokesContextActionWithoutOpening() {
        var opens = 0
        var contexts = 0
        rule.setContent { SelectableFileListItem(item, false, false, { opens++ }, { contexts++ }, {}, {}) }
        itemButton.performTouchInput { longClick() }
        assertEquals(0, opens)
        assertEquals(1, contexts)
    }

    @Test fun listSelectionCheckboxChangesStateWithoutOpeningFile() {
        var opens = 0
        var selections = 0
        rule.setContent {
            var selected by remember { mutableStateOf(false) }
            SelectableFileListItem(
                item,
                selected,
                true,
                { opens++ },
                {},
                {},
                {},
                onToggleSelection = {
                    selected = !selected
                    selections++
                },
            )
        }
        val checkbox = rule.onNodeWithTag("files.selection.file-test")
        checkbox.assertIsOff().performClick()
        checkbox.assertIsOn()
        assertEquals(0, opens)
        assertEquals(1, selections)
    }

    @Test fun gridSelectionCheckboxChangesStateWithoutOpeningFile() {
        var opens = 0
        var selections = 0
        rule.setContent {
            var selected by remember { mutableStateOf(false) }
            SelectableFileGridItem(item, selected, {
                selected = !selected
                selections++
            }, { opens++ }, modifier = Modifier.size(160.dp))
        }
        val checkbox = rule.onNodeWithTag("files.selection.file-test")
        checkbox.assertIsOff().performClick()
        checkbox.assertIsOn()
        assertEquals(0, opens)
        assertEquals(1, selections)
    }

    @Test fun normalGridItemExposesButtonActionAndOpensOnce() {
        var opens = 0
        rule.setContent { FileGridItem(item, { opens++ }, modifier = Modifier.size(160.dp)) }
        itemButton.performClick()
        assertEquals(1, opens)
    }
}
