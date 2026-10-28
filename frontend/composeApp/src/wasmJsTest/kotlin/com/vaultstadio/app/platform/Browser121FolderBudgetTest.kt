@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FOLDER_SELECTION_MAX_BYTES
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

/** Full directory adapter with actual bounded File/FileReader payloads; controlled chooser. */
class Browser121FolderBudgetTest {
    @Test fun folder121BrowserReadsEveryByteAtExactAggregateBound() = runTest {
        withBudgetFiles(FOLDER_SELECTION_MAX_BYTES.toInt(), 1) {
            val file = openFolderPicker().single()
            assertEquals(FOLDER_SELECTION_MAX_BYTES, file.size)
            assertEquals(FOLDER_SELECTION_MAX_BYTES.toInt(), file.data.size)
            assertPattern(file.data)
            assertEquals(1, budgetReadCount())
        }
    }

    @Test fun folder121BrowserReadsTwoFilesAtExactSharedAggregateBound() = runTest {
        withBudgetFiles((FOLDER_SELECTION_MAX_BYTES / 2).toInt(), 2) {
            val files = openFolderPicker()
            assertEquals(FOLDER_SELECTION_MAX_BYTES, files.sumOf { it.size })
            assertEquals(listOf("root/file0.bin", "root/file1.bin"), files.map { it.relativePath })
            files.forEach {
                assertEquals(it.size, it.data.size.toLong())
                assertPattern(it.data)
            }
            assertEquals(2, budgetReadCount())
        }
    }

    @Test fun folder121BrowserRejectsActualBoundPlusOneBeforeFileReaderStarts() = runTest {
        withBudgetFiles(FOLDER_SELECTION_MAX_BYTES.toInt() + 1, 1) {
            assertEquals(
                FolderSelectionProblem.TOO_LARGE,
                assertFailsWith<FolderSelectionException> {
                    openFolderPicker()
                }.problem,
            )
            assertEquals(0, budgetReadCount())
        }
    }

    private fun assertPattern(bytes: ByteArray) {
        for (index in bytes.indices) {
            if (bytes[index] != (index * 31 + 7).toByte()) fail("Byte mismatch at owned fixture offset $index")
        }
    }

    private suspend fun withBudgetFiles(size: Int, count: Int, block: suspend () -> Unit) {
        installBudgetFiles(size, count)
        try {
            block()
        } finally {
            restoreBudgetFiles()
        }
    }
}

private fun installBudgetFiles(size: Int, count: Int): Unit = js(
    """{
    const s={click:HTMLInputElement.prototype.click,read:FileReader.prototype.readAsArrayBuffer,reads:0};
    globalThis.__folderBudget121=s;
    FileReader.prototype.readAsArrayBuffer=function(blob){s.reads++;return s.read.call(this,blob);};
    HTMLInputElement.prototype.click=function(){
        const transfer=new DataTransfer();
        for(let j=0;j<count;j++){
            const data=new Uint8Array(size);for(let i=0;i<size;i++)data[i]=(i*31+7)&255;
            const file=new File([data],'file'+j+'.bin',{type:'application/octet-stream'});
            Object.defineProperty(file,'webkitRelativePath',{value:'root/'+file.name});transfer.items.add(file);
        }
        this.files=transfer.files;this.dispatchEvent(new Event('change'));
    };
}""",
)
private fun restoreBudgetFiles(): Unit = js(
    """{
    const s=globalThis.__folderBudget121;
    HTMLInputElement.prototype.click=s.click;FileReader.prototype.readAsArrayBuffer=s.read;
    delete globalThis.__folderBudget121;
}""",
)
private fun budgetReadCount(): Int = js("globalThis.__folderBudget121.reads")
