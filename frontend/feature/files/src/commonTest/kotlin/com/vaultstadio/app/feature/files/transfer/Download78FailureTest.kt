package com.vaultstadio.app.feature.files.transfer

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Download78FailureTest {
    @Test fun download78UnauthorizedNeverSavesAnErrorBody() = runTest {
        withDownload78 {
            read = { Result.Error("HTTP_401", "private detail") }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.AUTHENTICATION), controller.state.value)
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun download78ForbiddenNeverSavesAnErrorBody() = runTest {
        withDownload78 {
            read = { Result.Error("HTTP_403", "private detail") }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.ACCESS_DENIED), controller.state.value)
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun download78MissingNeverSavesAnErrorBody() = runTest {
        withDownload78 {
            read = { Result.Error("HTTP_404", "private detail") }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.NOT_FOUND), controller.state.value)
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun download78OfflineStateDoesNotContainProviderDiagnostics() = runTest {
        withDownload78 {
            read = { Result.NetworkError("private endpoint and credential-shaped-data") }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.NETWORK), controller.state.value)
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun download78SaveFailureHasSafeImpactAndCanBeRetriedExplicitly() = runTest {
        withDownload78 {
            save = { throw IllegalStateException("private local path") }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.SAVE_FAILED), controller.state.value)
            save = { SaveOutcome.SAVED }
            assertTrue(controller.retry())
            runCurrent()
            assertEquals(SaveOutcome.SAVED, (controller.state.value as DownloadState.Finished).outcome)
            assertEquals(2, saves.size)
        }
    }

    @Test fun download78ServerFailureDoesNotAutomaticallyRetry() = runTest {
        withDownload78 {
            read = { Result.Error("HTTP_500", "private stack") }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.UNAVAILABLE), controller.state.value)
            assertEquals(1, events.count { it.startsWith("bytes:") })
            assertTrue(saves.isEmpty())
            read = { Result.Success(bytes) }
            assertTrue(controller.retry())
            runCurrent()
            assertEquals(1, saves.size)
        }
    }

    @Test fun download78DoubleClickCannotDispatchASecondTransferWhileFetching() = runTest {
        withDownload78 {
            read = { awaitCancellation() }
            assertTrue(controller.item("file78"))
            runCurrent()
            assertFalse(controller.item("other"))
            assertFalse(controller.archive(listOf("a")))
            assertEquals(1, events.count { it.startsWith("bytes:") })
            controller.cancel()
            runCurrent()
            assertEquals(SaveOutcome.CANCELED, (controller.state.value as DownloadState.Finished).outcome)
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun download78DoubleClickCannotDispatchWhileTheChooserIsOpen() = runTest {
        withDownload78 {
            save = { awaitCancellation() }
            controller.item("file78")
            runCurrent()
            assertFalse(controller.item("other"))
            assertEquals(1, saves.size)
            controller.cancel()
            runCurrent()
            assertEquals(SaveOutcome.CANCELED, (controller.state.value as DownloadState.Finished).outcome)
        }
    }

    @Test fun download78CanceledOldBytesCannotReachSavingOrOverwriteANewerOutcome() = runTest {
        withDownload78 {
            val late = CompletableDeferred<Result<ByteArray>>()
            read = { withContext(NonCancellable) { late.await() } }
            controller.item("file78")
            runCurrent()
            controller.cancel()
            read = { Result.Success(bytes) }
            controller.item("file78")
            runCurrent()
            val accepted = controller.state.value
            late.complete(Result.Success(bytes))
            runCurrent()
            assertEquals(1, saves.size)
            assertEquals(accepted, controller.state.value)
        }
    }

    @Test fun download78CancelReleasesBusyAndAllowsAnExplicitRetry() = runTest {
        withDownload78 {
            read = { awaitCancellation() }
            controller.item("file78")
            runCurrent()
            controller.cancel()
            runCurrent()
            read = { Result.Success(bytes) }
            assertTrue(controller.retry())
            runCurrent()
            assertEquals(1, saves.size)
        }
    }

    @Test fun download78CloseCancelsOwnedWorkAndRejectsFurtherActions() = runTest {
        withDownload78 {
            read = { awaitCancellation() }
            controller.item("file78")
            runCurrent()
            controller.close()
            runCurrent()
            assertFalse(controller.item("file78"))
            assertFalse(controller.retry())
            assertTrue(saves.isEmpty())
            assertEquals(1, closed)
        }
    }

    @Test fun download78MetadataAuthenticationFailureDoesNotRequestBytes() = runTest {
        withDownload78 {
            metadata = { Result.Error("AUTHENTICATION_REQUIRED", "private") }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.AUTHENTICATION), controller.state.value)
            assertEquals(listOf("metadata:file78"), events)
        }
    }

    @Test fun download78MetadataMissingFailureHasRecoverableNotFoundState() = runTest {
        withDownload78 {
            metadata = { Result.Error("ITEM_NOT_FOUND", "private") }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.NOT_FOUND), controller.state.value)
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun download78CloseIsIdempotentForPlatformResources() = runTest {
        withDownload78 {
            controller.close()
            controller.close()
            assertEquals(1, closed)
        }
    }

    @Test fun download78CancelDuringSavingRetainsDestinationUncertainty() = runTest {
        withDownload78 {
            save = { awaitCancellation() }
            controller.item("file78")
            runCurrent()
            controller.cancel()
            runCurrent()
            assertTrue((controller.state.value as DownloadState.Finished).destinationMayHaveContent)
        }
    }

    @Test fun download78CancelBeforeSavingHasNoPossibleDestinationEffect() = runTest {
        withDownload78 {
            read = { awaitCancellation() }
            controller.item("file78")
            runCurrent()
            controller.cancel()
            runCurrent()
            assertFalse((controller.state.value as DownloadState.Finished).destinationMayHaveContent)
            assertTrue(saves.isEmpty())
        }
    }
}
