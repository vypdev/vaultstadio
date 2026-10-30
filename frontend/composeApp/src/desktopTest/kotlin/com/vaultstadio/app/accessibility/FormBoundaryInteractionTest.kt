package com.vaultstadio.app.accessibility

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import com.vaultstadio.app.core.resources.EnglishStrings
import com.vaultstadio.app.feature.collaboration.AddCommentDialog
import com.vaultstadio.app.feature.federation.LinkIdentityDialog
import com.vaultstadio.app.feature.federation.RequestFederationDialog
import com.vaultstadio.app.feature.files.NewFolderDialog
import com.vaultstadio.app.feature.files.RenameDialog
import com.vaultstadio.app.feature.profile.ChangePasswordDialog
import com.vaultstadio.app.feature.profile.EditProfileDialog
import com.vaultstadio.app.feature.sync.RegisterDeviceDialog
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormBoundaryInteractionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun blankFolderCannotBeSubmitted() {
        var calls = 0
        rule.setContent { NewFolderDialog("", {}, { calls++ }, {}, EnglishStrings) }
        rule.onNodeWithText("Done").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test fun folderSubmitReceivesEditedUnicodeName() {
        var submitted: String? = null
        rule.setContent {
            var value by remember { mutableStateOf("") }
            NewFolderDialog(value, { value = it }, { submitted = value }, {}, EnglishStrings)
        }
        rule.onNodeWithTag("files.folder-name").performTextReplacement("Carpeta ñ 漢字")
        rule.onNodeWithText("Done").performClick()
        assertEquals("Carpeta ñ 漢字", submitted)
    }

    @Test fun blankRenameCannotBeSubmitted() {
        var calls = 0
        rule.setContent { RenameDialog("", {}, { calls++ }, {}, EnglishStrings) }
        rule.onNode(
            hasText("Rename") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        ).assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test fun renameSubmitReceivesEditedName() {
        var submitted: String? = null
        rule.setContent {
            var value by remember { mutableStateOf("old") }
            RenameDialog(value, { value = it }, { submitted = value }, {}, EnglishStrings)
        }
        rule.onNodeWithTag("files.rename").performTextReplacement("nuevo ñ")
        rule.onNode(
            hasText("Rename") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        ).performClick()
        assertEquals("nuevo ñ", submitted)
    }

    @Test fun profileSaveReceivesEditedUsername() {
        var submitted: String? = null
        rule.setContent { EditProfileDialog("before", false, { submitted = it }, {}) }
        rule.onNodeWithTag("profile.username").performTextReplacement("Usuario ñ")
        rule.onNodeWithText("Save").performClick()
        assertEquals("Usuario ñ", submitted)
    }

    @Test fun clearingProfileNameDisablesSave() {
        rule.setContent { EditProfileDialog("before", false, {}, {}) }
        rule.onNodeWithTag("profile.username").performTextClearance()
        rule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test fun passwordMismatchCannotBeSubmitted() {
        var submitted: Pair<String, String>? = null
        rule.setContent { ChangePasswordDialog(false, { old, new -> submitted = old to new }, {}) }
        fillProfilePasswords("old", "different", "mismatch")
        rule.onNode(hasText("Change Password") and hasClickAction()).assertIsNotEnabled().performClick()
        assertNull(submitted)
    }

    @Test fun matchingPasswordsReachNormalCallback() {
        var submitted: Pair<String, String>? = null
        rule.setContent { ChangePasswordDialog(false, { old, new -> submitted = old to new }, {}) }
        fillProfilePasswords("old", "new-value", "new-value")
        rule.onNode(hasText("Change Password") and hasClickAction()).performClick()
        assertEquals("old" to "new-value", submitted)
    }

    @Test fun cancellingPasswordDialogHasNoSubmissionEffect() {
        var submissions = 0
        var dismissals = 0
        rule.setContent { ChangePasswordDialog(false, { _, _ -> submissions++ }, { dismissals++ }) }
        fillProfilePasswords("old", "new-value", "new-value")
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(0, submissions)
        assertEquals(1, dismissals)
    }

    @Test fun dialogReopenClearsItsRememberedPassword() {
        var open by mutableStateOf(true)
        rule.setContent { if (open) ChangePasswordDialog(false, { _, _ -> }, { open = false }) }
        rule.onNodeWithTag("profile.current-password").performTextReplacement("temporary")
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { open = true }
        rule.onNodeWithTag("profile.current-password").assert(hasTextExactly("Current password", ""))
    }

    @Test fun emptyCommentCannotBeSubmitted() {
        var calls = 0
        rule.setContent { AddCommentDialog({ calls++ }, {}, EnglishStrings) }
        rule.onNodeWithText("Add").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test fun commentSubmitReceivesUnicodeText() {
        var submitted: String? = null
        rule.setContent { AddCommentDialog({ submitted = it }, {}, EnglishStrings) }
        rule.onNodeWithTag("collaboration.comment").performTextReplacement("Comentario ñ 漢字")
        rule.onNodeWithText("Add").performClick()
        assertEquals("Comentario ñ 漢字", submitted)
    }

    @Test fun federationRequestNormalizesBlankOptionalMessage() {
        var submitted: Pair<String, String?>? = null
        rule.setContent {
            RequestFederationDialog(EnglishStrings, {}, { domain, message -> submitted = domain to message })
        }
        rule.onNodeWithTag("federation.domain").performTextReplacement("remote.audit.invalid")
        rule.onNodeWithTag("federation.message").performTextReplacement("   ")
        rule.onNodeWithText("Request").performClick()
        assertEquals("remote.audit.invalid" to null, submitted)
    }

    @Test fun linkedIdentityRequiresEveryRequiredField() {
        var calls = 0
        rule.setContent { LinkIdentityDialog(EnglishStrings, {}, { _, _, _ -> calls++ }) }
        rule.onNodeWithTag("federation.remote-user").performTextReplacement("remote-user")
        rule.onNodeWithTag("federation.remote-instance").performTextReplacement("remote.audit.invalid")
        rule.onNodeWithText("Link").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
        rule.onNodeWithTag("federation.display-name").performTextReplacement("Nombre ñ")
        rule.onNodeWithText("Link").assertIsEnabled().performClick()
        assertEquals(1, calls)
    }

    @Test fun deviceTypeChipChangesSubmittedSelection() {
        var selectedType: String? = null
        rule.setContent { RegisterDeviceDialog({ _, _, type -> selectedType = type }, {}, EnglishStrings) }
        rule.onNodeWithTag("sync.device-name").performTextReplacement("Equipo ñ")
        rule.onNodeWithText("Mobile").performClick()
        rule.onNodeWithText("Mobile").assertIsSelected()
        rule.onNodeWithText("Desktop").assertIsNotSelected()
        rule.onNodeWithText("Register Device").performClick()
        assertEquals("MOBILE", selectedType)
    }

    private fun fillProfilePasswords(current: String, new: String, confirm: String) {
        rule.onNodeWithTag("profile.current-password").performTextReplacement(current)
        rule.onNodeWithTag("profile.new-password").performTextReplacement(new)
        rule.onNodeWithTag("profile.confirm-password").performTextReplacement(confirm)
    }
}
