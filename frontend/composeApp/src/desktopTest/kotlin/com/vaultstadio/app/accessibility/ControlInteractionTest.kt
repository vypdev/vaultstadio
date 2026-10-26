package com.vaultstadio.app.accessibility

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.vaultstadio.app.core.resources.EnglishStrings
import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.core.resources.ThemeMode
import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.feature.admin.QuotaEditDialog
import com.vaultstadio.app.feature.admin.RoleEditDialog
import com.vaultstadio.app.feature.ai.AIInputArea
import com.vaultstadio.app.feature.ai.AIMode
import com.vaultstadio.app.feature.settings.LanguageSelectionDialog
import com.vaultstadio.app.feature.settings.SettingsToggleItem
import com.vaultstadio.app.feature.settings.ThemeSelectionDialog
import com.vaultstadio.app.navigation.MainDestination
import com.vaultstadio.app.ui.components.layout.MainSidebar
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ControlInteractionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun settingsSwitchExposesStateAndDispatchesOnce() {
        var calls = 0
        rule.setContent {
            var checked by remember { mutableStateOf(false) }
            SettingsToggleItem(Icons.Default.Settings, "Toggle setting", "Description", checked, {
                checked = it
                calls++
            })
        }
        val control = rule.onNodeWithText("Toggle setting")
        control.assertIsOff().performClick()
        control.assertIsOn()
        assertEquals(1, calls)
    }

    @Test fun unlimitedQuotaCheckboxRemovesInputAndSubmitsNull() {
        var submitted: Long? = 123
        rule.setContent { QuotaEditDialog(quotaUser(), {}, { submitted = it }) }
        val checkbox = rule.onNodeWithText("Unlimited storage")
        checkbox.assertIsOff().performClick()
        checkbox.assertIsOn()
        rule.onNodeWithTag("admin.quota").assertDoesNotExist()
        rule.onNodeWithText("Save").performClick()
        assertNull(submitted)
    }

    @Test fun quotaDropdownChangesUnitUsedBySubmission() {
        var submitted: Long? = null
        rule.setContent { QuotaEditDialog(quotaUser(), {}, { submitted = it }) }
        rule.onNodeWithTag("admin.quota").performTextReplacement("4")
        rule.onNodeWithText("GB").performClick()
        rule.onNodeWithText("MB").performClick()
        rule.onNodeWithText("Save").performClick()
        assertEquals(4L * 1024 * 1024, submitted)
    }

    @Test fun quotaNumericInputFiltersNonDigits() {
        rule.setContent { QuotaEditDialog(quotaUser(), {}, {}) }
        rule.onNodeWithTag("admin.quota").performTextReplacement("1a2-3")
        rule.onNodeWithTag("admin.quota").assert(hasText("123"))
    }

    @Test fun roleRadioExposesSelectionAndSubmitsChosenValue() {
        var submitted: String? = null
        rule.setContent { RoleEditDialog(quotaUser(), {}, { submitted = it }) }
        val admin = rule.onNode(
            hasText("ADMIN") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
        admin.performClick()
        admin.assertIsSelected()
        rule.onNodeWithText("Save").performClick()
        assertEquals("ADMIN", submitted)
    }

    @Test fun themeRadioSelectionUpdatesActualState() {
        var selected by mutableStateOf(ThemeMode.LIGHT)
        rule.setContent { ThemeSelectionDialog(selected, { selected = it }, {}) }
        rule.onNodeWithText(EnglishStrings.settingsThemeDark).performClick()
        rule.onNodeWithText(EnglishStrings.settingsThemeDark).assertIsSelected()
        assertEquals(ThemeMode.DARK, selected)
    }

    @Test fun languageRadioSelectionUpdatesActualState() {
        var selected by mutableStateOf(Language.ENGLISH)
        rule.setContent { LanguageSelectionDialog(selected, { selected = it }, {}) }
        rule.onNodeWithText(Language.SPANISH.displayName).performClick()
        rule.onNodeWithText(Language.SPANISH.displayName).assertIsSelected()
        assertEquals(Language.SPANISH, selected)
    }

    @Test fun sidebarNavigationExposesSelectedDestination() {
        var destination by mutableStateOf(MainDestination.FILES)
        rule.setContent { MainSidebar(destination, false, null, { destination = it }, {}) }
        rule.onNodeWithText(EnglishStrings.navRecent).performClick()
        rule.onNodeWithText(EnglishStrings.navRecent).assertIsSelected()
        rule.onNodeWithText(EnglishStrings.filesHome).assertIsNotSelected()
        assertEquals(MainDestination.RECENT, destination)
    }

    @Test fun sidebarDoesNotOfferAdminRouteToOrdinaryUser() {
        rule.setContent { MainSidebar(MainDestination.FILES, false, null, {}, {}) }
        rule.onNodeWithText(EnglishStrings.navAdmin).assertDoesNotExist()
    }

    @Test fun aiWithoutProviderCannotSubmitEvenWhenInputExists() {
        var calls = 0
        rule.setContent { AIInputArea("draft", AIMode.CHAT, false, null, {}, { calls++ }, EnglishStrings) }
        rule.onNodeWithTag("ai.send").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test fun busyAiSendRemainsNamedAndCannotSubmitTwice() {
        var calls = 0
        val provider = AIProviderInfo(AIProviderType.OLLAMA, "http://127.0.0.1:1")
        rule.setContent { AIInputArea("draft", AIMode.CHAT, true, provider, {}, { calls++ }, EnglishStrings) }
        rule.onNodeWithTag("ai.send").assertContentDescriptionEquals(EnglishStrings.commonSend)
            .assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test fun readyAiSendInvokesNormalCallbackOnce() {
        var calls = 0
        val provider = AIProviderInfo(AIProviderType.OLLAMA, "http://127.0.0.1:1")
        rule.setContent { AIInputArea("draft", AIMode.CHAT, false, provider, {}, { calls++ }, EnglishStrings) }
        rule.onNodeWithTag("ai.send").assertIsEnabled().performClick()
        assertEquals(1, calls)
    }
}
