package com.vaultstadio.app.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Each collected case renders the production component and edits its actual state. */
@RunWith(Parameterized::class)
class ActiveFieldInteractionTest(private val id: String) {
    @get:Rule val rule = createComposeRule()
    private val sample get() = if (id in numericFields) "123" else "Edición ñ 漢字 😀"

    @Before
    fun renderProductionControl() {
        val auth = inputAuthViewModel()
        rule.setContent {
            MaterialTheme {
                when {
                    id.startsWith("auth.") -> AuthFieldFixture(id, auth)
                    id.startsWith("versions.") -> VersionFieldFixture()
                    id.startsWith("password.") || id in screenFields -> ScreenFieldFixture(id)
                    else -> DialogFieldFixture(id)
                }
            }
        }
        if (id.startsWith("versions.")) {
            if (id == "versions.restore-comment") {
                rule.onNodeWithText("Restore").performClick()
            } else {
                rule.onNodeWithContentDescription("Cleanup").performClick()
            }
        }
        if (id.startsWith("password.")) rule.onNodeWithTag(id).performScrollTo()
    }

    @Test
    fun editUpdatesProductionState() {
        rule.onNodeWithTag(id).apply {
            performTextReplacement(sample)
            assert(SemanticsMatcher.expectValue(SemanticsProperties.InputText, AnnotatedString(sample)))
            val visible = if (id.contains("password") ||
                id.startsWith("password.")
            ) {
                "•".repeat(sample.length)
            } else {
                sample
            }
            assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(visible)))
        }
    }

    @Test
    fun clearUpdatesProductionState() {
        rule.onNodeWithTag(id).apply {
            performTextReplacement(sample)
            performTextClearance()
            assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        }
    }

    @Test
    fun clickMovesRealInputFocus() {
        rule.onNodeWithTag(id).apply {
            performClick()
            assertIsFocused()
        }
    }

    companion object {
        private val numericFields = setOf("admin.quota", "versions.keep-last", "versions.max-age")
        private val screenFields = setOf("files.search", "ai.input", "collaboration.document", "admin.quota")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun fields(): List<Array<String>> = listOf(
            "auth.login.email", "auth.login.password", "auth.register.email", "auth.register.username",
            "auth.register.password", "auth.register.confirm-password", "profile.username", "profile.current-password",
            "profile.new-password", "profile.confirm-password", "password.current", "password.new", "password.confirm",
            "files.search", "files.folder-name", "files.rename", "federation.domain", "federation.message",
            "federation.remote-user", "federation.remote-instance", "federation.display-name", "collaboration.comment",
            "collaboration.document", "versions.restore-comment", "versions.keep-last", "versions.max-age",
            "admin.quota", "sync.device-name", "ai.input",
        ).map { arrayOf(it) }
    }
}
