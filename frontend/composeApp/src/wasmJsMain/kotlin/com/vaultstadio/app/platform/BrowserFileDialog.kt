@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal fun browserDirectoryPickerAvailable(): Boolean =
    directoryInputSupported(document.createElement("input") as HTMLInputElement)

/** A fresh input for each operation; change/cancel/coroutine disposal all release it. */
internal suspend fun selectBrowserFiles(
    multiple: Boolean,
    accept: String,
    directory: Boolean = false,
    maximumFiles: Int = Int.MAX_VALUE,
): List<File> = suspendCancellableCoroutine { continuation ->
    val input = document.createElement("input") as HTMLInputElement
    if (directory && !directoryInputSupported(input)) {
        continuation.resumeWithException(FolderSelectionException(FolderSelectionProblem.UNSUPPORTED))
        return@suspendCancellableCoroutine
    }
    input.type = "file"
    input.multiple = multiple
    input.accept = accept
    input.style.display = "none"
    if (directory) input.setAttribute("webkitdirectory", "")
    val body = document.body
    if (body == null) {
        continuation.resumeWithException(FolderSelectionException(FolderSelectionProblem.UNSUPPORTED))
        return@suspendCancellableCoroutine
    }
    lateinit var changed: (Event) -> Unit
    lateinit var canceled: (Event) -> Unit
    fun cleanup() {
        input.removeEventListener("change", changed)
        input.removeEventListener("cancel", canceled)
        input.parentNode?.removeChild(input)
    }
    changed = {
        if (continuation.isActive) {
            val files = input.files
            val selected = if (files == null || files.length > maximumFiles) {
                emptyList()
            } else {
                (0 until files.length).mapNotNull { index -> files[index] }
            }
            val problem = when {
                files != null && files.length > maximumFiles -> FolderSelectionProblem.TOO_MANY_ENTRIES
                files != null && selected.size != files.length -> FolderSelectionProblem.READ_FAILED
                else -> null
            }
            cleanup()
            if (problem == null) {
                continuation.resume(selected)
            } else {
                continuation.resumeWithException(FolderSelectionException(problem))
            }
        }
    }
    canceled = {
        if (continuation.isActive) {
            cleanup()
            continuation.resume(emptyList())
        }
    }
    input.addEventListener("change", changed)
    input.addEventListener("cancel", canceled)
    body.appendChild(input)
    continuation.invokeOnCancellation { cleanup() }
    if (continuation.isActive) {
        try {
            if (!activateFileInput(input) && continuation.isActive) {
                cleanup()
                continuation.resumeWithException(FolderSelectionException(FolderSelectionProblem.UNSUPPORTED))
            }
        } catch (fatal: Throwable) {
            cleanup()
            throw fatal
        }
    }
}

private fun directoryInputSupported(input: HTMLInputElement): Boolean = js("'webkitdirectory' in input")

private fun activateFileInput(input: HTMLInputElement): Boolean = js(
    """(function() {
    try { input.click(); return true; }
    catch (error) { if (error instanceof DOMException) return false; throw error; }
})()""",
)
