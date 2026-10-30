@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderSelectionException
import kotlinx.coroutines.test.runTest
import org.w3c.files.Blob
import org.w3c.files.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Controlled metadata abuse on actual browser File/Blob objects; no huge-file readiness claim. */
class Browser121MetadataTest {
    @Test fun folder121BrowserSizeRejectsNegativeValues() = rejects(-1.0)

    @Test fun folder121BrowserSizeRejectsNaN() = rejects(Double.NaN)

    @Test fun folder121BrowserSizeRejectsInfinity() = rejects(Double.POSITIVE_INFINITY)

    @Test fun folder121BrowserSizeRejectsFractionalValues() = rejects(1.5)

    @Test fun folder121BrowserSizeRejectsUnsafeInteger() = rejects(9007199254740992.0)

    @Test fun folder121BrowserSizePreservesValidZeroAndSafeIntegerMetadata() {
        assertEquals(0L, browserFileSize(fileWithReportedSize(0.0)))
        assertEquals(9007199254740991L, browserFileSize(fileWithReportedSize(9007199254740991.0)))
    }

    @Test fun folder121LargeFileSnapshotDoesNotFollowLaterMetadataMutation() {
        val file = fileWithReportedSize(1.0)
        val source = LargeSelectedFile(file)
        changeReportedSize(file, 2.0)
        assertEquals(1L, source.size)
        assertEquals("application/octet-stream", source.mimeType)
    }

    @Test fun folder121ReaderRejectsActualBufferBeyondReportedBudget() = runTest {
        assertFailsWith<FolderSelectionException> { readBrowserBlob(blobWithIncorrectSize(), 1) }
    }

    private fun rejects(size: Double) {
        assertFailsWith<FolderSelectionException> { browserFileSize(fileWithReportedSize(size)) }
    }
}

private fun fileWithReportedSize(size: Double): File = js(
    """(function(){const file=new File([new Uint8Array([7])],'a.bin');
    Object.defineProperty(file,'size',{value:size,configurable:true});return file;})()""",
)
private fun changeReportedSize(
    file: File,
    size: Double,
): Unit = js("{Object.defineProperty(file,'size',{value:size,configurable:true});}")
private fun blobWithIncorrectSize(): Blob = js(
    """(function(){const blob=new Blob([new Uint8Array([1,2,3,4])]);
    Object.defineProperty(blob,'size',{value:1});return blob;})()""",
)
