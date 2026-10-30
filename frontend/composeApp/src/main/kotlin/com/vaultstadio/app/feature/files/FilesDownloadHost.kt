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
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import com.vaultstadio.app.domain.storage.usecase.DownloadArchiveUseCase
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import com.vaultstadio.app.domain.storage.usecase.GetItemUseCase
import com.vaultstadio.app.feature.files.transfer.DownloadState
import com.vaultstadio.app.feature.files.transfer.FilesDownloadController
import com.vaultstadio.app.platform.createDownloadSaver
import org.koin.compose.koinInject

internal val LocalFileDownloads =
    staticCompositionLocalOf<FilesDownloadController> { error("Download host is required") }

@Composable
internal fun FilesDownloadHost(content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val item: GetItemUseCase = koinInject()
    val single: DownloadFileUseCase = koinInject()
    val archive: DownloadArchiveUseCase = koinInject()
    val saver = remember { createDownloadSaver() }
    val controller =
        remember(scope, item, single, archive, saver) { FilesDownloadController(scope, item, single, archive, saver) }
    DisposableEffect(controller) { onDispose(controller::close) }
    CompositionLocalProvider(LocalFileDownloads provides controller) {
        Box(Modifier.fillMaxSize()) {
            content()
            DownloadFeedback(controller, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun DownloadFeedback(controller: FilesDownloadController, modifier: Modifier) {
    val state by controller.state.collectAsState()
    val strings = LocalStrings.current
    val copy = downloadCopy(Strings.currentLanguage)
    if (state == DownloadState.Idle) return
    val current = state
    val message = when (current) {
        DownloadState.Idle -> ""
        is DownloadState.Running -> if (current.saving) copy.saving else copy.fetching
        is DownloadState.Failed -> copy.problem(current.problem)
        is DownloadState.Finished -> when (current.outcome) {
            SaveOutcome.SAVED -> copy.saved
            SaveOutcome.HANDED_TO_BROWSER -> copy.handed
            SaveOutcome.CANCELED -> if (current.destinationMayHaveContent) copy.canceledSaving else copy.canceled
            SaveOutcome.UNSUPPORTED -> copy.unsupported
        }
    }
    Surface(modifier.padding(16.dp).widthIn(max = 640.dp).testTag("files.download.status"), tonalElevation = 8.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f))
            if (current is DownloadState.Running) {
                TextButton(onClick = controller::cancel) { Text(strings.actionCancel) }
            } else {
                if (current is DownloadState.Failed ||
                    (current is DownloadState.Finished && current.outcome == SaveOutcome.CANCELED)
                ) {
                    TextButton(onClick = { controller.retry() }) { Text(copy.retry) }
                }
                TextButton(onClick = controller::dismiss) { Text(strings.actionClose) }
            }
        }
    }
}
