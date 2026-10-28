@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.files.transfer

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Download78LifecycleTest {
    @Test fun transfer78CompletedFeedbackCanBeDismissedAndNextOperationStillWorks() = runTest {
        withDownload78 {
            controller.item("file78")
            runCurrent()
            assertIs<DownloadState.Finished>(controller.state.value)
            controller.dismiss()
            assertEquals(DownloadState.Idle, controller.state.value)
            assertTrue(controller.item("file78"))
            runCurrent()
            assertEquals(2, saves.size)
        }
    }

    @Test fun transfer78DismissCannotDiscardOwnershipOfARunningOperation() = runTest {
        withDownload78 {
            read = { awaitCancellation() }
            controller.item("file78")
            runCurrent()
            val running = assertIs<DownloadState.Running>(controller.state.value)
            controller.dismiss()
            assertEquals(running, controller.state.value)
            assertFalse(controller.item("file78"))
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun transfer78ScopeCanceledBeforeCoroutineDispatchReleasesBusyWithoutAnyEffects() = runTest {
        val owner = SupervisorJob()
        val fixture = Download78Fixture(CoroutineScope(owner + StandardTestDispatcher(testScheduler)))
        try {
            assertTrue(fixture.controller.item("file78"))
            owner.cancel()
            runCurrent()
            assertEquals(SaveOutcome.CANCELED, assertIs<DownloadState.Finished>(fixture.controller.state.value).outcome)
            assertTrue(fixture.events.isEmpty())
            assertTrue(fixture.saves.isEmpty())
        } finally {
            owner.cancel()
            fixture.controller.close()
        }
    }

    @Test fun transfer78HttpAndTypedSizeRefusalsMapToSafeTooLargeState() = runTest {
        for (code in listOf("HTTP_413", "FILE_TOO_LARGE")) {
            withDownload78 {
                zip = { Result.Error(code, "private size details") }
                controller.archive(listOf("file78"))
                runCurrent()
                assertEquals(DownloadState.Failed(DownloadProblem.TOO_LARGE), controller.state.value)
                assertTrue(saves.isEmpty())
            }
        }
    }

    @Test fun transfer78HttpAndTypedValidationRefusalsMapToSafeSelectionState() = runTest {
        for (code in listOf("HTTP_400", "VALIDATION_ERROR")) {
            withDownload78 {
                zip = { Result.Error(code, "private selection details") }
                controller.archive(listOf("file78"))
                runCurrent()
                assertEquals(DownloadState.Failed(DownloadProblem.INVALID_SELECTION), controller.state.value)
                assertTrue(saves.isEmpty())
            }
        }
    }

    @Test fun transfer78EverySupportedConcurrentChangeCodeRefusesSavingPartialArchive() = runTest {
        for (code in listOf("HTTP_409", "DOWNLOAD_INCOMPLETE", "CONCURRENT_MODIFICATION")) {
            withDownload78 {
                zip = { Result.Error(code, "private version details") }
                controller.archive(listOf("file78"))
                runCurrent()
                assertEquals(DownloadState.Failed(DownloadProblem.CONTENT_CHANGED), controller.state.value)
                assertTrue(saves.isEmpty())
            }
        }
    }
}
