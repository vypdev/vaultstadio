package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FOLDER_SELECTION_MAX_DEPTH
import com.vaultstadio.app.domain.upload.FOLDER_SELECTION_MAX_ENTRIES
import com.vaultstadio.app.domain.upload.FolderFileMetadata
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.folderSelectionProblem
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.CoroutineContext

/** Lazy, no-follow traversal stops before unbounded metadata allocation or any body read. */
internal fun collectDesktopFolderMetadata(selected: Path, context: CoroutineContext): List<DesktopFolderRecord> {
    val root = selected.toAbsolutePath().normalize()
    context.ensureActive()
    if (Files.isSymbolicLink(root)) throw FolderSelectionException(FolderSelectionProblem.INVALID_PATH)
    if (!Files.isDirectory(
            root,
            LinkOption.NOFOLLOW_LINKS,
        )
    ) {
        throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
    }
    val records = mutableListOf<DesktopFolderRecord>()
    var visited = 0
    fun guard(path: Path) {
        context.ensureActive()
        if (++visited >
            FOLDER_SELECTION_MAX_ENTRIES
        ) {
            throw FolderSelectionException(FolderSelectionProblem.TOO_MANY_ENTRIES)
        }
        if (path != root && root.relativize(path).nameCount > FOLDER_SELECTION_MAX_DEPTH) {
            throw FolderSelectionException(FolderSelectionProblem.TOO_DEEP)
        }
    }
    Files.walkFileTree(
        root,
        object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                guard(dir)
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                guard(file)
                if (!attrs.isRegularFile ||
                    attrs.isSymbolicLink
                ) {
                    throw FolderSelectionException(FolderSelectionProblem.INVALID_PATH)
                }
                val metadata = FolderFileMetadata(
                    file.fileName.toString(),
                    root.relativize(file).joinToString("/") { it.toString() },
                    attrs.size(),
                    "application/octet-stream",
                )
                folderSelectionProblem(records.map { it.metadata } + metadata, requireRoot = false)
                    ?.let { throw FolderSelectionException(it) }
                records += DesktopFolderRecord(file, metadata)
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc != null) throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
                return FileVisitResult.CONTINUE
            }
        },
    )
    context.ensureActive()
    return records
}
