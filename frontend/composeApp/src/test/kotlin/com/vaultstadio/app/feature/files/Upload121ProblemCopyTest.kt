package com.vaultstadio.app.feature.files

import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Upload121ProblemCopyTest {
    @Test fun folder121OrdinaryReadFailureUsesFileCopy() = assertFileReadCopy(UploadAction.Files)

    @Test fun folder121LargeReadFailureUsesFileCopy() = assertFileReadCopy(UploadAction.LargeFiles)

    @Test fun folder121OrdinaryUnavailableDoesNotDescribeDirectory() = assertFileCapability(UploadAction.Files)

    @Test fun folder121LargeUnavailableDoesNotDescribeDirectory() = assertFileCapability(UploadAction.LargeFiles)

    @Test fun folder121OrdinaryProblemNeverAdvertisesFolderBounds() = assertFileBounds(UploadAction.Files)

    @Test fun folder121LargeProblemNeverAdvertisesFolderBounds() = assertFileBounds(UploadAction.LargeFiles)

    @Test fun folder121FolderProblemRetainsDirectorySpecificCopy() {
        for (language in Language.entries) {
            for (problem in FolderSelectionProblem.entries) {
                assertEquals(
                    folderUploadCopy(language).problem(problem),
                    uploadSelectionProblemCopy(language, UploadAction.Folder, problem),
                )
            }
        }
    }

    @Test fun folder121FileProblemsAreLocalizedInEveryMaintainedLanguage() {
        val nouns = mapOf(
            Language.ENGLISH to "files",
            Language.SPANISH to "archivos",
            Language.FRENCH to "fichiers",
            Language.GERMAN to "Dateien",
            Language.PORTUGUESE to "ficheiros",
            Language.CHINESE to "文件",
            Language.JAPANESE to "ファイル",
        )
        assertEquals(Language.entries.toSet(), nouns.keys)
        for ((language, noun) in nouns) {
            for (action in listOf(UploadAction.Files, UploadAction.LargeFiles)) {
                val read = uploadSelectionProblemCopy(language, action, FolderSelectionProblem.READ_FAILED)
                val unsupported = uploadSelectionProblemCopy(language, action, FolderSelectionProblem.UNSUPPORTED)
                assertTrue(read.contains(noun, ignoreCase = true), "$language: $read")
                assertNotEquals(read, unsupported)
                assertNotEquals(folderUploadCopy(language).unsupported, unsupported)
                for (problem in FolderSelectionProblem.entries) {
                    assertTrue(uploadSelectionProblemCopy(language, action, problem).isNotBlank())
                }
            }
        }
    }

    private fun assertFileReadCopy(action: UploadAction) {
        val text = uploadSelectionProblemCopy(Language.ENGLISH, action, FolderSelectionProblem.READ_FAILED)
        assertTrue(text.contains("files", ignoreCase = true), text)
        assertTrue(text.contains("Nothing was added"), text)
        assertTrue(text.contains("again"), text)
        assertFalse(text.contains("folder", ignoreCase = true), text)
    }

    private fun assertFileCapability(action: UploadAction) {
        val text = uploadSelectionProblemCopy(Language.ENGLISH, action, FolderSelectionProblem.UNSUPPORTED)
        assertTrue(text.contains("file", ignoreCase = true), text)
        assertTrue(text.contains("unavailable", ignoreCase = true), text)
        assertFalse(text.contains("directory", ignoreCase = true), text)
        assertFalse(text.contains("folder", ignoreCase = true), text)
    }

    private fun assertFileBounds(action: UploadAction) {
        for (problem in listOf(
            FolderSelectionProblem.TOO_LARGE,
            FolderSelectionProblem.TOO_MANY_ENTRIES,
            FolderSelectionProblem.TOO_DEEP,
        )) {
            val text = uploadSelectionProblemCopy(Language.ENGLISH, action, problem)
            assertTrue(text.contains("files", ignoreCase = true), text)
            assertFalse(text.contains("folder", ignoreCase = true), text)
            assertFalse(listOf("32", "256", "16").any(text::contains), text)
        }
    }
}
