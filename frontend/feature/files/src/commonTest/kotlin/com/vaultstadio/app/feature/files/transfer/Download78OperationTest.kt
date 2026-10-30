package com.vaultstadio.app.feature.files.transfer

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Download78OperationTest {
    @Test fun download78FileCallsMetadataThenBytesThenSaveExactlyOnce() = runTest {
        withDownload78 {
            controller.item("file78")
            runCurrent()
            assertEquals(listOf("metadata:file78", "bytes:file78", "save"), events)
            assertContentEquals(bytes, saves.single().bytes)
            assertEquals(item.name, saves.single().name)
            assertEquals(SaveOutcome.SAVED, (controller.state.value as DownloadState.Finished).outcome)
        }
    }

    @Test fun download78EmptyFileIsARealSavedPayload() = runTest {
        withDownload78 {
            metadata = { Result.Success(item.copy(size = 0)) }
            read = { Result.Success(byteArrayOf()) }
            controller.item("file78")
            runCurrent()
            assertContentEquals(byteArrayOf(), saves.single().bytes)
        }
    }

    @Test fun download78FolderUsesTheArchivePortInsteadOfFileBytes() = runTest {
        withDownload78 {
            metadata = { Result.Success(item.copy(type = ItemType.FOLDER, size = 0)) }
            controller.item("file78")
            runCurrent()
            assertEquals(listOf(listOf("file78")), archives)
            assertFalse(events.any { it.startsWith("bytes:") })
            assertEquals("VaultStadio.zip", saves.single().name)
            assertEquals("application/zip", saves.single().mimeType)
        }
    }

    @Test fun download78ArchiveSelectionIsAnOwnedSnapshot() = runTest {
        withDownload78 {
            val selected = mutableListOf("a", "b")
            controller.archive(selected)
            selected.clear()
            runCurrent()
            assertEquals(listOf(listOf("a", "b")), archives)
            assertEquals(1, saves.size)
        }
    }

    @Test fun download78NoNetworkRequestForAnEmptyArchiveSelection() = runTest {
        withDownload78 {
            controller.archive(emptyList())
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.INVALID_SELECTION), controller.state.value)
            assertTrue(events.isEmpty())
        }
    }

    @Test fun download78DuplicateArchiveIdentifiersAreNotSent() = runTest {
        withDownload78 {
            controller.archive(listOf("a", "a"))
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.INVALID_SELECTION), controller.state.value)
            assertTrue(events.isEmpty())
        }
    }

    @Test fun download78ExcessiveArchiveSelectionIsNotSent() = runTest {
        withDownload78 {
            controller.archive((1..257).map { "item$it" })
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.INVALID_SELECTION), controller.state.value)
            assertTrue(events.isEmpty())
        }
    }

    @Test fun download78UnsafeFilenameCannotReachTheByteOrSavePorts() = runTest {
        withDownload78 {
            metadata = { Result.Success(item.copy(name = "../private.bin")) }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.INVALID_NAME), controller.state.value)
            assertEquals(listOf("metadata:file78"), events)
        }
    }

    @Test fun download78OversizedMetadataStopsBeforeContentAllocation() = runTest {
        withDownload78 {
            metadata = { Result.Success(item.copy(size = 32L * 1024 * 1024 + 1)) }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.TOO_LARGE), controller.state.value)
            assertEquals(listOf("metadata:file78"), events)
        }
    }

    @Test fun download78ChangedContentLengthDoesNotSavePartialBytes() = runTest {
        withDownload78 {
            read = { Result.Success(byteArrayOf(1)) }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.CONTENT_CHANGED), controller.state.value)
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun download78WrongMetadataIdentityCannotDownloadAnotherItem() = runTest {
        withDownload78 {
            metadata = { Result.Success(item.copy(id = "different")) }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.CONTENT_CHANGED), controller.state.value)
            assertTrue(saves.isEmpty())
        }
    }

    @Test fun download78NativeChooserCancellationIsNotAnErrorOrSavedClaim() = runTest {
        withDownload78 {
            save = { SaveOutcome.CANCELED }
            controller.item("file78")
            runCurrent()
            assertEquals(SaveOutcome.CANCELED, (controller.state.value as DownloadState.Finished).outcome)
        }
    }

    @Test fun download78BrowserHandoffIsDistinctFromDiskSave() = runTest {
        withDownload78 {
            save = { SaveOutcome.HANDED_TO_BROWSER }
            controller.item("file78")
            runCurrent()
            assertEquals(SaveOutcome.HANDED_TO_BROWSER, (controller.state.value as DownloadState.Finished).outcome)
        }
    }

    @Test fun download78UnsupportedPlatformCannotClaimSuccess() = runTest {
        withDownload78 {
            save = { SaveOutcome.UNSUPPORTED }
            controller.item("file78")
            runCurrent()
            assertEquals(DownloadState.Failed(DownloadProblem.UNSUPPORTED), controller.state.value)
        }
    }
}
