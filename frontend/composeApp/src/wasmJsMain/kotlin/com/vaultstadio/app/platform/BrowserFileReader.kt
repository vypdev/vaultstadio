@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.files.Blob
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny

internal fun browserFileSize(file: File): Long {
    val size = file.size.toDouble()
    if (!size.isFinite() || size < 0 || size > 9007199254740991.0 || size % 1.0 != 0.0) {
        throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
    }
    return size.toLong()
}

/** Actual FileReader with bounded allocation and owned callback/abort cleanup. */
internal suspend fun readBrowserBlob(
    blob: Blob,
    maximumBytes: Long,
): ByteArray = suspendCancellableCoroutine { continuation ->
    val reader = FileReader()
    fun cleanup() {
        reader.onload = null
        reader.onerror = null
        reader.onabort = null
    }
    fun failed() {
        cleanup()
        if (continuation.isActive) {
            continuation.resumeWithException(
                FolderSelectionException(FolderSelectionProblem.READ_FAILED),
            )
        }
    }
    reader.onload = {
        if (continuation.isActive) {
            val value = reader.result
            if (value == null || !isArrayBuffer(value)) {
                failed()
            } else {
                val buffer = value.unsafeCast<ArrayBuffer>()
                if (buffer.byteLength.toLong() > maximumBytes) {
                    failed()
                } else {
                    val array = Int8Array(buffer)
                    val bytes = ByteArray(array.length) { array[it] }
                    cleanup()
                    continuation.resume(bytes)
                }
            }
        }
    }
    reader.onerror = { failed() }
    reader.onabort = { failed() }
    continuation.invokeOnCancellation {
        cleanup()
        if (reader.readyState.toInt() == 1) reader.abort()
    }
    if (continuation.isActive) {
        try {
            if (maximumBytes < 0 ||
                maximumBytes > Int.MAX_VALUE.toLong() ||
                blob.size.toDouble() > maximumBytes ||
                !startBrowserRead(reader, blob)
            ) {
                failed()
            }
        } catch (fatal: Throwable) {
            cleanup()
            if (reader.readyState.toInt() == 1) reader.abort()
            throw fatal
        }
    }
}

internal fun browserSlice(file: File, start: Long, end: Long): Blob = sliceBrowserFile(
    file,
    start.toDouble(),
    end.toDouble(),
)
private fun sliceBrowserFile(file: File, start: Double, end: Double): Blob = js("file.slice(start, end)")
private fun isArrayBuffer(value: JsAny): Boolean = js("value instanceof ArrayBuffer")
private fun startBrowserRead(reader: FileReader, blob: Blob): Boolean = js(
    """(function() {
    try { reader.readAsArrayBuffer(blob); return true; }
    catch (error) {
        if (error instanceof DOMException || error instanceof TypeError) return false;
        throw error;
    }
})()""",
)
