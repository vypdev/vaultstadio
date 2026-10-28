@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.feature.files.folderUploadCopy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Folder121UploadControllerTest {
    @Test fun folder121ControllerReportsUnsupportedWithoutOpeningChooser() = runTest {
        val h = Harness(this)
        h.available = false
        assertFalse(h.controller.start("parent"))
        assertEquals(FolderPreparationState.Failed(FolderSelectionProblem.UNSUPPORTED), h.controller.state.value)
        assertEquals(0, h.calls)
        assertTrue(h.queued.isEmpty())
    }

    @Test fun folder121ControllerQueuesIntoOriginalParent() = runTest {
        val h = Harness(this)
        val gate = CompletableDeferred<Unit>()
        h.pick = {
            gate.await()
            listOf(entry())
        }
        var parent = "original"
        assertTrue(h.controller.start(parent))
        runCurrent()
        parent = "navigated"
        gate.complete(Unit)
        runCurrent()
        assertEquals("navigated", parent)
        assertEquals(listOf("original"), h.queued.map { it.second })
        assertEquals(FolderPreparationState.Queued, h.controller.state.value)
    }

    @Test fun folder121ControllerRejectsDuplicateActivation() = runTest {
        val h = Harness(this)
        h.pick = { awaitCancellation() }
        assertTrue(h.controller.start("first"))
        assertFalse(h.controller.start("second"))
        runCurrent()
        assertEquals(1, h.calls)
        assertEquals(FolderPreparationState.Preparing, h.controller.state.value)
        h.controller.cancel()
    }

    @Test fun folder121ControllerCancelBeforeDispatchNeverOpensChooser() = runTest {
        val h = Harness(this)
        h.controller.start("parent")
        h.controller.cancel()
        runCurrent()
        assertEquals(0, h.calls)
        assertTrue(h.queued.isEmpty())
        assertEquals(FolderPreparationState.Canceled, h.controller.state.value)
    }

    @Test fun folder121ControllerIgnoresLateResultAfterCancelAndNewOperation() = runTest {
        val h = Harness(this)
        val gate = CompletableDeferred<Unit>()
        h.pick = {
            withContext(NonCancellable) { gate.await() }
            listOf(entry())
        }
        h.controller.start("old")
        runCurrent()
        h.controller.cancel()
        h.pick = { listOf(entry("new")) }
        h.controller.start("new-parent")
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("new-parent"), h.queued.map { it.second })
        assertEquals("new", h.queued.single().first.single().name)
    }

    @Test fun folder121ControllerCloseDiscardsLateResultAndPreventsRestart() = runTest {
        val h = Harness(this)
        val gate = CompletableDeferred<Unit>()
        h.pick = {
            withContext(NonCancellable) { gate.await() }
            listOf(entry())
        }
        h.controller.start("parent")
        runCurrent()
        h.controller.close()
        gate.complete(Unit)
        runCurrent()
        assertTrue(h.queued.isEmpty())
        assertEquals(FolderPreparationState.Idle, h.controller.state.value)
        assertFalse(h.controller.start("another"))
    }

    @Test fun folder121ControllerExpectedFailureKeepsTypedProblemAndBusyCanClear() = runTest {
        val h = Harness(this)
        h.pick = { throw FolderSelectionException(FolderSelectionProblem.INVALID_PATH) }
        h.controller.start("parent")
        runCurrent()
        assertEquals(FolderPreparationState.Failed(FolderSelectionProblem.INVALID_PATH), h.controller.state.value)
        assertTrue(h.queued.isEmpty())
        h.controller.dismiss()
        assertEquals(FolderPreparationState.Idle, h.controller.state.value)
    }

    @Test fun folder121ControllerRetryRequiresExplicitActionAndUsesOriginalDestination() = runTest {
        val h = Harness(this)
        h.pick = { throw FolderSelectionException(FolderSelectionProblem.READ_FAILED) }
        h.controller.start("original")
        runCurrent()
        assertEquals(1, h.calls)
        assertTrue(h.queued.isEmpty())
        h.pick = { listOf(entry()) }
        assertTrue(h.controller.retry())
        runCurrent()
        assertEquals(2, h.calls)
        assertEquals("original", h.queued.single().second)
        assertFalse(h.controller.retry())
    }

    @Test fun folder121ControllerRejectsPreparedPathBeforeQueueMutation() = runTest {
        val h = Harness(this)
        h.pick = { listOf(entry().copy(relativePath = "root/../file")) }
        h.controller.start("parent")
        runCurrent()
        assertEquals(FolderPreparationState.Failed(FolderSelectionProblem.INVALID_PATH), h.controller.state.value)
        assertTrue(h.queued.isEmpty())
    }

    @Test fun folder121ControllerRejectsPreparedSizeMismatch() = runTest {
        val h = Harness(this)
        h.pick = { listOf(entry().copy(size = 2)) }
        h.controller.start("parent")
        runCurrent()
        assertEquals(FolderPreparationState.Failed(FolderSelectionProblem.READ_FAILED), h.controller.state.value)
        assertTrue(h.queued.isEmpty())
    }

    @Test fun folder121ControllerEmptySelectionDoesNotAnnounceUpload() = runTest {
        val h = Harness(this)
        h.pick = { emptyList() }
        h.controller.start(null)
        runCurrent()
        assertTrue(h.queued.isEmpty())
        assertEquals(FolderPreparationState.Canceled, h.controller.state.value)
    }

    @Test fun folder121ControllerDismissCannotHideActivePreparation() = runTest {
        val h = Harness(this)
        h.pick = { awaitCancellation() }
        h.controller.start(null)
        runCurrent()
        h.controller.dismiss()
        assertEquals(FolderPreparationState.Preparing, h.controller.state.value)
        h.controller.close()
    }

    @Test fun folder121ControllerUnavailableScopeDoesNotStartWork() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val h = Harness(scope)
        scope.cancel()
        assertFalse(h.controller.start(null))
        assertEquals(0, h.calls)
    }

    @Test fun folder121ControllerFatalErrorIsReportedToOwner() = runTest {
        val failures = mutableListOf<Throwable>()
        val scope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(testScheduler) +
                CoroutineExceptionHandler { _, error -> failures += error },
        )
        val h = Harness(scope)
        val fatal = Error("owned fatal selection")
        h.pick = { throw fatal }
        h.controller.start(null)
        runCurrent()
        assertEquals(listOf<Throwable>(fatal), failures.toList())
        assertEquals(FolderPreparationState.Failed(FolderSelectionProblem.READ_FAILED), h.controller.state.value)
        assertTrue(h.queued.isEmpty())
        scope.cancel()
    }

    @Test fun folder121ControllerDoesNotExposeProviderExceptionText() = runTest {
        val h = Harness(this)
        h.pick = { error("INTERNAL_PROVIDER_DIAGNOSTIC_121") }
        h.controller.start(null)
        runCurrent()
        val failure = h.controller.state.value as FolderPreparationState.Failed
        for (language in Language.entries) {
            val copy = folderUploadCopy(language)
            assertTrue(copy.hint.isNotBlank())
            for (problem in FolderSelectionProblem.entries) assertTrue(copy.problem(problem).isNotBlank())
            assertFalse(copy.problem(failure.problem).contains("INTERNAL_PROVIDER_DIAGNOSTIC_121"))
        }
    }

    private class Harness(scope: CoroutineScope) {
        var calls = 0
        var available = true
        var pick: suspend () -> List<FolderUploadEntry> = { listOf(entry()) }
        val queued = mutableListOf<Pair<List<FolderUploadEntry>, String?>>()
        val controller = FolderUploadController(scope, { available }, {
            calls++
            pick()
        }, { files, parent ->
            queued +=
                files to parent
        })
    }

    companion object {
        private fun entry(
            name: String = "file",
        ) = FolderUploadEntry(name, "root/$name", 1, "test/binary", byteArrayOf(7))
    }
}
