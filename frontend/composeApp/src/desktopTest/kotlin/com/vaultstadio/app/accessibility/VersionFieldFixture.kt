package com.vaultstadio.app.accessibility

import androidx.compose.runtime.Composable
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.feature.versionhistory.VersionHistoryScreen
import kotlinx.datetime.Instant

@Composable
internal fun VersionFieldFixture() {
    val version =
        FileVersion("v1", "item", 1, 1, "checksum", "input-test", Instant.fromEpochMilliseconds(0), isLatest = false)
    VersionHistoryScreen(
        itemId = "item", itemName = "input.txt",
        versionHistory = FileVersionHistory("item", "input.txt", listOf(version), 1, 1),
        selectedVersion = null, versionDiff = null, downloadUrl = null, isLoading = false, error = null,
        onLoadHistory = {}, onGetVersion = { _, _ -> }, onRestoreVersion = { _, _, _ -> },
        onDeleteVersion = {}, onCompareVersions = { _, _, _ -> }, onDownloadVersion = { _, _ -> },
        onCleanupVersions = { _, _, _, _ -> }, onClearSelectedVersion = {}, onClearDownloadUrl = {},
        onClearDiff = {}, onClearError = {}, onBack = {},
    )
}
