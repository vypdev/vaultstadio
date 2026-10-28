package com.vaultstadio.app.domain.upload

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FolderSelectionPolicyTest {
    @Test fun folder121AcceptsMaximumComposedPathLength() = valid(List(16) { "a".repeat(255) }.joinToString("/"))

    @Test fun folder121PreservesRootAndPath() {
        val file = entry("root/sub/file.bin")
        assertNull(folderSelectionProblem(listOf(file)))
        assertEquals("root/sub/file.bin", file.relativePath)
    }

    @Test fun folder121PreservesUnicodeCaseAndSpaces() = valid(" ra íz /日本語/niño Ñ.bin")

    @Test fun folder121AcceptsEmptyFile() = valid("root/empty", 0)

    @Test fun folder121AcceptsExactAggregateBytes() = valid("root/file", FOLDER_SELECTION_MAX_BYTES)

    @Test fun folder121RejectsByteBoundPlusOne() = invalid(
        FolderSelectionProblem.TOO_LARGE,
        "r/a",
        FOLDER_SELECTION_MAX_BYTES + 1,
    )

    @Test fun folder121RejectsNegativeSize() = invalid(FolderSelectionProblem.TOO_LARGE, "r/a", -1)

    @Test fun folder121RejectsLongOverflowSize() = invalid(FolderSelectionProblem.TOO_LARGE, "r/a", Long.MAX_VALUE)

    @Test fun folder121AccountsForAllDeclaredFiles() {
        val half = FOLDER_SELECTION_MAX_BYTES / 2
        assertNull(folderSelectionProblem(listOf(entry("r/a", half), entry("r/b", half))))
        assertEquals(
            FolderSelectionProblem.TOO_LARGE,
            folderSelectionProblem(
                listOf(
                    entry("r/a", half),
                    entry(
                        "r/b",
                        half + 1,
                    ),
                ),
            ),
        )
    }

    @Test fun folder121RejectsAbsolutePaths() = invalid(FolderSelectionProblem.INVALID_PATH, "/root/file")

    @Test fun folder121RejectsBackslashes() = invalid(FolderSelectionProblem.INVALID_PATH, "root/a\\b")

    @Test fun folder121RejectsDriveSyntax() = invalid(FolderSelectionProblem.INVALID_PATH, "C:/file")

    @Test fun folder121RejectsColonInNestedSegment() = invalid(FolderSelectionProblem.INVALID_PATH, "root/a:b/file")

    @Test fun folder121RejectsDotNavigation() = invalid(FolderSelectionProblem.INVALID_PATH, "root/./file")

    @Test fun folder121RejectsParentNavigation() = invalid(FolderSelectionProblem.INVALID_PATH, "root/../file")

    @Test fun folder121RejectsEmptySegment() = invalid(FolderSelectionProblem.INVALID_PATH, "root//file")

    @Test fun folder121RejectsBlankSegment() = invalid(FolderSelectionProblem.INVALID_PATH, "root/ /file")

    @Test fun folder121RejectsTrailingSeparator() = invalid(FolderSelectionProblem.INVALID_PATH, "root/file/")

    @Test fun folder121RejectsMissingRoot() = invalid(FolderSelectionProblem.INVALID_PATH, "file")

    @Test fun folder121RetainsNativeRelativeFileCompatibility() = assertNull(
        folderSelectionProblem(listOf(entry("file")), requireRoot = false),
    )

    @Test fun folder121RejectsControlCharacters() {
        for (char in listOf(
            '\u0000',
            '\n',
            '\r',
            '\u007f',
            '\u0085',
        )) {
            invalid(FolderSelectionProblem.INVALID_PATH, "root/a${char}b")
        }
    }

    @Test fun folder121RejectsDifferentBasename() = assertEquals(
        FolderSelectionProblem.INVALID_PATH,
        folderSelectionProblem(listOf(entry("root/a").copy(name = "different"))),
    )

    @Test fun folder121RejectsExactDuplicates() = assertEquals(
        FolderSelectionProblem.CONFLICTING_PATHS,
        folderSelectionProblem(listOf(entry("r/a"), entry("r/a"))),
    )

    @Test fun folder121RejectsFileDirectoryCollisionInBothOrders() {
        val files = listOf(entry("r/a"), entry("r/a/b"))
        assertEquals(FolderSelectionProblem.CONFLICTING_PATHS, folderSelectionProblem(files))
        assertEquals(FolderSelectionProblem.CONFLICTING_PATHS, folderSelectionProblem(files.reversed()))
    }

    @Test fun folder121PreservesDistinctCaseAndUnicodeSpellings() = assertNull(
        folderSelectionProblem(listOf(entry("r/A"), entry("r/a"), entry("r/é"), entry("r/e\u0301"))),
    )

    @Test fun folder121AcceptsExactCombinedEntryBound() = assertNull(
        folderSelectionProblem((1..255).map { entry("root/file$it", 0) }),
    )

    @Test fun folder121CountsImplicitRootAgainstEntryBudget() = assertEquals(
        FolderSelectionProblem.TOO_MANY_ENTRIES,
        folderSelectionProblem((1..256).map { entry("root/file$it", 0) }),
    )

    @Test fun folder121RejectsExcessFileCountImmediately() = assertEquals(
        FolderSelectionProblem.TOO_MANY_ENTRIES,
        folderSelectionProblem((1..257).map { entry("root/file$it", 0) }),
    )

    @Test fun folder121CountsAllImplicitDirectories() = assertEquals(
        FolderSelectionProblem.TOO_MANY_ENTRIES,
        folderSelectionProblem((1..130).map { entry("root/dir$it/file", 0) }),
    )

    @Test fun folder121AcceptsExactDepth() = valid((1..16).joinToString("/") { "s$it" })

    @Test fun folder121RejectsExcessDepth() = invalid(
        FolderSelectionProblem.TOO_DEEP,
        (1..17).joinToString("/") {
            "s$it"
        },
    )

    @Test fun folder121AcceptsExactSegmentLength() = valid("root/" + "a".repeat(255))

    @Test fun folder121RejectsExcessSegmentLength() = invalid(
        FolderSelectionProblem.INVALID_PATH,
        "root/" + "a".repeat(256),
    )

    @Test fun folder121RejectsUnboundedPathBeforeSplitting() = invalid(
        FolderSelectionProblem.INVALID_PATH,
        "root/" + "a/".repeat(4096),
    )

    @Test fun folder121AcceptsEmptySelectionWithoutCreatingAnything() = assertNull(folderSelectionProblem(emptyList()))

    private fun entry(path: String, size: Long = 1) = FolderFileMetadata(
        path.substringAfterLast('/'),
        path,
        size,
        "test/binary",
    )
    private fun valid(path: String, size: Long = 1) = assertNull(folderSelectionProblem(listOf(entry(path, size))))
    private fun invalid(problem: FolderSelectionProblem, path: String, size: Long = 1) =
        assertEquals(problem, folderSelectionProblem(listOf(entry(path, size))))
}
