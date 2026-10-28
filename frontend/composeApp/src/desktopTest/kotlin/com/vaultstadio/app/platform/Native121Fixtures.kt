package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal suspend fun <T> native121Tree(block: suspend (Path) -> T): T {
    val root = Files.createTempDirectory("native121-owned-")
    try {
        return block(root)
    } finally {
        // Default walk does not follow links into another directory.
        Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
    }
}

internal fun native121Sized(path: Path, size: Long) {
    Files.createDirectories(path.parent)
    RandomAccessFile(path.toFile(), "rw").use { it.setLength(size) }
}

internal suspend fun native121RefusedBeforeRead(root: Path, expected: FolderSelectionProblem) {
    var reads = 0
    val failure = assertFailsWith<FolderSelectionException> {
        readDesktopFolderFiles(root.toFile()) { _, _ ->
            reads++
            error("Refused metadata must not start any body read")
        }
    }
    assertEquals(expected, failure.problem)
    assertEquals(0, reads)
}
