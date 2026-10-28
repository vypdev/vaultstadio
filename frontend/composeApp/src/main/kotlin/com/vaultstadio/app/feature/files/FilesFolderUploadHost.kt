package com.vaultstadio.app.feature.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.Strings
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.feature.upload.FolderPreparationState
import com.vaultstadio.app.feature.upload.FolderUploadController
import com.vaultstadio.app.feature.upload.LocalUploadManager
import com.vaultstadio.app.platform.isFolderPickerAvailable
import com.vaultstadio.app.platform.openFolderPicker

internal val LocalFolderUploads = staticCompositionLocalOf<FolderUploadController> { error("Folder host is required") }

@Composable
internal fun FilesFolderUploadHost(content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val manager = LocalUploadManager.current
    val controller = remember(scope, manager) {
        FolderUploadController(
            scope,
            available = { manager != null && isFolderPickerAvailable() },
            pick = {
                openFolderPicker().map { FolderUploadEntry(it.name, it.relativePath, it.size, it.mimeType, it.data) }
            },
            enqueue = { entries, parent -> manager?.addFolderEntries(entries, parent) },
        )
    }
    DisposableEffect(controller) { onDispose(controller::close) }
    CompositionLocalProvider(LocalFolderUploads provides controller) {
        Box(Modifier.fillMaxSize()) {
            content()
            FolderUploadFeedback(controller, Modifier.align(Alignment.TopCenter).padding(top = 72.dp))
        }
    }
}

@Composable
internal fun FolderUploadFeedback(controller: FolderUploadController, modifier: Modifier) {
    val current by controller.state.collectAsState()
    val strings = LocalStrings.current
    val copy = folderUploadCopy(Strings.currentLanguage)
    val message = when (val state = current) {
        FolderPreparationState.Idle -> return
        FolderPreparationState.Preparing -> copy.preparing
        FolderPreparationState.Canceled -> copy.canceled
        FolderPreparationState.Queued -> copy.queued
        is FolderPreparationState.Failed -> copy.problem(state.problem)
    }
    Surface(modifier.padding(16.dp).widthIn(max = 640.dp).testTag("files.folder.status"), tonalElevation = 8.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f))
            if (current == FolderPreparationState.Preparing) {
                TextButton(onClick = controller::cancel, modifier = Modifier.testTag("files.folder.cancel")) {
                    Text(strings.actionCancel)
                }
            } else {
                val failed = current as? FolderPreparationState.Failed
                if (failed != null && failed.problem != FolderSelectionProblem.UNSUPPORTED) {
                    TextButton(onClick = {
                        controller.retry()
                    }, modifier = Modifier.testTag("files.folder.retry")) { Text(strings.actionRetry) }
                }
                TextButton(onClick = controller::dismiss, modifier = Modifier.testTag("files.folder.close")) {
                    Text(strings.actionClose)
                }
            }
        }
    }
}
