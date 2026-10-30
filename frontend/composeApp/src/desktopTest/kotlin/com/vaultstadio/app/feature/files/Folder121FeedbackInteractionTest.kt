package com.vaultstadio.app.feature.files

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.feature.upload.FolderPreparationState
import com.vaultstadio.app.feature.upload.FolderUploadController
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Folder121FeedbackInteractionTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var controller: FolderUploadController
    private val parents = mutableListOf<String?>()

    @Test fun folder121RenderedCancelStopsPreparationWithoutQueueing() {
        mount { awaitCancellation() }
        rule.runOnIdle { controller.start("original") }
        rule.onNodeWithTag("files.folder.cancel").assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertEquals(FolderPreparationState.Canceled, controller.state.value)
            assertTrue(parents.isEmpty())
        }
    }

    @Test fun folder121RenderedRetryKeepsOriginalParentAndCloseDismissesFeedback() {
        var fail = true
        mount {
            if (fail) throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
            listOf(FolderUploadEntry("a", "root/a", 1, "test/binary", byteArrayOf(7)))
        }
        rule.runOnIdle { controller.start("original") }
        rule.waitUntil(5_000) { controller.state.value is FolderPreparationState.Failed }
        rule.runOnIdle { fail = false }
        rule.onNodeWithTag("files.folder.retry").performClick()
        rule.waitUntil(5_000) { controller.state.value == FolderPreparationState.Queued }
        rule.runOnIdle { assertEquals(listOf<String?>("original"), parents.toList()) }
        rule.onNodeWithTag("files.folder.close").performClick()
        rule.onNodeWithTag("files.folder.status").assertDoesNotExist()
    }

    @Test fun folder121RenderedUnsupportedFeedbackDoesNotOfferMisleadingRetry() {
        mount(available = false) { error("unsupported picker must not be invoked") }
        rule.runOnIdle { controller.start(null) }
        rule.onNodeWithTag("files.folder.status").assertIsDisplayed()
        rule.onNodeWithTag("files.folder.retry").assertDoesNotExist()
        rule.onNodeWithTag("files.folder.cancel").assertDoesNotExist()
        rule.onNodeWithTag("files.folder.close").performClick()
        rule.runOnIdle { assertTrue(parents.isEmpty()) }
    }

    @Test fun folder121RenderedIdleFeedbackIsAbsent() {
        mount { emptyList() }
        rule.onNodeWithTag("files.folder.status").assertDoesNotExist()
    }

    private fun mount(available: Boolean = true, pick: suspend () -> List<FolderUploadEntry>) {
        rule.setContent {
            val scope = rememberCoroutineScope()
            controller =
                remember(scope) {
                    FolderUploadController(scope, { available }, pick, { _, parent -> parents += parent })
                }
            DisposableEffect(controller) { onDispose(controller::close) }
            MaterialTheme { FolderUploadFeedback(controller, Modifier) }
        }
    }
}
