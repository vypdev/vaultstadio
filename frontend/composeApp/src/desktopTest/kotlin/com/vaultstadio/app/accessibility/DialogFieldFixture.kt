package com.vaultstadio.app.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vaultstadio.app.core.resources.EnglishStrings
import com.vaultstadio.app.feature.collaboration.AddCommentDialog
import com.vaultstadio.app.feature.federation.LinkIdentityDialog
import com.vaultstadio.app.feature.federation.RequestFederationDialog
import com.vaultstadio.app.feature.files.NewFolderDialog
import com.vaultstadio.app.feature.files.RenameDialog
import com.vaultstadio.app.feature.profile.ChangePasswordDialog
import com.vaultstadio.app.feature.profile.EditProfileDialog
import com.vaultstadio.app.feature.sync.RegisterDeviceDialog

@Composable
internal fun DialogFieldFixture(id: String, saving: Boolean = false) {
    var value by remember { mutableStateOf("") }
    when {
        id == "profile.username" -> EditProfileDialog("", saving, {}, {})
        id.startsWith("profile.") -> ChangePasswordDialog(saving, { _, _ -> }, {})
        id == "files.folder-name" -> NewFolderDialog(value, { value = it }, {}, {}, EnglishStrings)
        id == "files.rename" -> RenameDialog(value, { value = it }, {}, {}, EnglishStrings)
        id == "sync.device-name" -> RegisterDeviceDialog({ _, _, _ -> }, {}, EnglishStrings)
        id == "collaboration.comment" -> AddCommentDialog({}, {}, EnglishStrings)
        id == "federation.domain" || id == "federation.message" ->
            RequestFederationDialog(EnglishStrings, {}, { _, _ -> })
        id.startsWith("federation.") -> LinkIdentityDialog(EnglishStrings, {}, { _, _, _ -> })
        else -> error("Missing production dialog for $id")
    }
}
