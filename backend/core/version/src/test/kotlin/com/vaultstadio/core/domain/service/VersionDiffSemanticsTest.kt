package com.vaultstadio.core.domain.service

import arrow.core.Either
import com.vaultstadio.core.domain.model.DiffOperation
import com.vaultstadio.core.domain.model.DiffPatch
import com.vaultstadio.core.domain.model.VersionDiff
import io.mockk.Called
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VersionDiffSemanticsTest {
    private data class Example(val name: String, val from: String, val to: String, val patches: List<DiffPatch>)

    @TestFactory
    fun `extracted line comparison retains content and coordinates`(): List<DynamicTest> {
        fun add(line: Int, text: String) = DiffPatch(DiffOperation.ADD, line, line, newContent = text)
        fun delete(line: Int, text: String) = DiffPatch(DiffOperation.DELETE, line, line, oldContent = text)
        val examples = listOf(
            Example("empty", "", "", emptyList()),
            Example("unicode", "café\n", "mañana\n", listOf(DiffPatch(DiffOperation.MODIFY, 1, 1, "café", "mañana"))),
            Example("unchanged", "a\nb\n", "a\nb\n", emptyList()),
            Example("append-all", "", "a\nb\n", listOf(add(1, "a"), add(2, "b"))),
            Example("remove-all", "a\nb\n", "", listOf(delete(1, "a"), delete(2, "b"))),
            Example("insert", "a\nb\n", "a\nx\nb\n", listOf(add(2, "x"))),
            Example("delete", "a\nx\nb\n", "a\nb\n", listOf(delete(2, "x"))),
            Example(
                "replace",
                "a\nold\nz\n",
                "a\nnew\nz\n",
                listOf(DiffPatch(DiffOperation.MODIFY, 2, 2, "old", "new")),
            ),
            Example("reorder-delete", "a\nb\n", "b\na\n", listOf(delete(1, "a"), add(2, "a"))),
            Example("duplicates", "a\na\nb\n", "a\nb\na\n", listOf(delete(2, "a"), add(3, "a"))),
            Example("reorder-insert", "a\nx\nb\n", "b\na\nx\n", listOf(add(1, "b"), delete(3, "b"))),
        )
        return examples.map { example ->
            DynamicTest.dynamicTest("VER74-DIFF-${example.name}") {
                runTest {
                    val f = VersionServiceFixture()
                    f.bytes["key1"] = example.from.toByteArray()
                    f.bytes["key2"] = example.to.toByteArray()
                    f.rows[0] = f.rows[0].copy(size = f.bytes.getValue("key1").size.toLong())
                    f.rows[1] = f.rows[1].copy(size = f.bytes.getValue("key2").size.toLong())
                    val before = f.snapshot()
                    val diff =
                        assertIs<VersionDiff>(
                            assertIs<Either.Right<*>>(f.service.compareVersions("item", 1, 2, "owner")).value,
                        )
                    assertEquals(example.patches, diff.patches)
                    assertEquals(
                        (example.to.toByteArray().size - example.from.toByteArray().size).toLong(),
                        diff.sizeChange,
                    )
                    assertEquals(listOf("key1", "key2"), f.closedStreams)
                    assertEquals(before, f.snapshot())
                }
            }
        }
    }

    @TestFactory
    fun `MIME categories decide whether content may be read`() =
        listOf(
            null to true,
            "application/octet-stream" to true,
            "text/new-kind" to false,
            "application/vendor+json" to false,
            "application/vendor+xml" to false,
        ).map { (mime, binary) ->
            DynamicTest.dynamicTest("VER74-MIME-${mime ?: "absent"}") {
                runTest {
                    val f = VersionServiceFixture()
                    f.item = f.item.copy(mimeType = mime)
                    val diff =
                        assertIs<VersionDiff>(
                            assertIs<Either.Right<*>>(f.service.compareVersions("item", 1, 2, "owner")).value,
                        )
                    assertEquals(binary, diff.isBinary)
                    if (binary) {
                        verify { f.backend wasNot Called }
                        assertTrue(diff.patches.isEmpty())
                    } else {
                        assertEquals(listOf("key1", "key2"), f.closedStreams)
                    }
                }
            }
        }
}
