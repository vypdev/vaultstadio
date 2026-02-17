/**
 * VaultStadio WASM Drag & Drop Implementation
 *
 * Uses HTML5 Drag and Drop API.
 */

package com.vaultstadio.app.platform

import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.DragEvent
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.FileList
import org.w3c.files.FileReader
import org.w3c.files.get

private var dropCallback: ((List<SelectedFile>) -> Unit)? = null
private var dragCounter = 0

/**
 * Initialize drag and drop handling for the browser.
 */
actual fun initializeDragDrop(onFilesDropped: (List<SelectedFile>) -> Unit) {
    dropCallback = onFilesDropped

    // Prevent default drag behaviors on the document
    document.addEventListener("dragenter", { event: Event ->
        event.preventDefault()
        event.stopPropagation()
        dragCounter++
        if (dragCounter == 1) {
            DragDropState.setDragging(true)
        }
    })

    document.addEventListener("dragleave", { event: Event ->
        event.preventDefault()
        event.stopPropagation()
        dragCounter--
        if (dragCounter == 0) {
            DragDropState.setDragging(false)
        }
    })

    document.addEventListener("dragover", { event: Event ->
        event.preventDefault()
        event.stopPropagation()
    })

    document.addEventListener("drop", { event: Event ->
        event.preventDefault()
        event.stopPropagation()
        dragCounter = 0
        DragDropState.setDragging(false)

        val dragEvent = event.unsafeCast<DragEvent>()
        val dataTransfer = dragEvent.dataTransfer
        val files = dataTransfer?.files

        if (files != null && files.length > 0) {
            processDroppedFiles(files)
        }
    })
}

/**
 * Process dropped files.
 */
private fun processDroppedFiles(files: FileList) {
    val selectedFiles = mutableListOf<SelectedFile>()
    var pendingReads = files.length

    for (i in 0 until files.length) {
        val file = files[i]
        if (file != null) {
            readDroppedFile(file) { selectedFile ->
                if (selectedFile != null) {
                    selectedFiles.add(selectedFile)
                }
                pendingReads--
                if (pendingReads == 0 && selectedFiles.isNotEmpty()) {
                    dropCallback?.invoke(selectedFiles)
                    DragDropState.emitEvent(DragDropEvent.Drop(selectedFiles))
                }
            }
        } else {
            pendingReads--
        }
    }
}

/**
 * Read a dropped file.
 */
private fun readDroppedFile(file: File, onComplete: (SelectedFile?) -> Unit) {
    val reader = FileReader()
    val fileName = file.name
    val fileSize = file.size.toDouble().toLong()
    val fileMimeType = file.type.ifEmpty { "application/octet-stream" }

    reader.onload = { _ ->
        try {
            val result = reader.result
            if (result != null) {
                val arrayBuffer = result.unsafeCast<ArrayBuffer>()
                val int8Array = Int8Array(arrayBuffer)
                val length = int8Array.length
                val byteArray = ByteArray(length) { i -> int8Array[i] }

                onComplete(
                    SelectedFile(
                        name = fileName,
                        size = fileSize,
                        mimeType = fileMimeType,
                        data = byteArray,
                    ),
                )
            } else {
                onComplete(null)
            }
        } catch (e: Exception) {
            onComplete(null)
        }
    }

    reader.onerror = { _ ->
        onComplete(null)
    }

    reader.readAsArrayBuffer(file)
}
