package com.vaultstadio.app.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vaultstadio.app.core.resources.EnglishStrings
import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.admin.model.UserStatus
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.feature.admin.QuotaEditDialog
import com.vaultstadio.app.feature.ai.AIInputArea
import com.vaultstadio.app.feature.ai.AIMode
import com.vaultstadio.app.feature.changepassword.ChangePasswordScreen
import com.vaultstadio.app.feature.collaboration.DocumentEditor
import com.vaultstadio.app.feature.files.FilesSearchBar
import kotlinx.datetime.Instant

internal fun quotaUser() = AdminUser(
    "input-test", "input@audit.invalid", "input-test", UserRole.USER, UserStatus.ACTIVE,
    null, 1024L * 1024 * 1024, 0, Instant.fromEpochMilliseconds(0), null,
)

@Composable
internal fun ScreenFieldFixture(id: String, loading: Boolean = false) {
    var value by remember { mutableStateOf("") }
    when {
        id == "files.search" -> FilesSearchBar(value, {
            value = it
        }, { value = "" }, emptyList(), {}, {}, false, EnglishStrings)
        id == "ai.input" -> AIInputArea(value, AIMode.CHAT, false, null, { value = it }, {}, EnglishStrings)
        id == "collaboration.document" -> DocumentEditor(null, value, { value = it })
        id == "admin.quota" -> QuotaEditDialog(quotaUser(), {}, {})
        id.startsWith("password.") -> ChangePasswordScreen(
            currentPassword = if (id == "password.current") value else "",
            newPassword = if (id == "password.new") value else "",
            confirmPassword = if (id == "password.confirm") value else "",
            isLoading = loading, errorMessage = null, isSuccess = false,
            showCurrentPassword = false, showNewPassword = false, showConfirmPassword = false,
            onCurrentPasswordChange = { value = it }, onNewPasswordChange = { value = it },
            onConfirmPasswordChange = { value = it }, onToggleCurrentPasswordVisibility = {},
            onToggleNewPasswordVisibility = {}, onToggleConfirmPasswordVisibility = {},
            onChangePassword = {}, onDismissSuccess = {}, onNavigateBack = {},
        )
        else -> error("Missing production screen for $id")
    }
}
