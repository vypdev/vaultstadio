package com.vaultstadio.core.domain.service

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.VersionDiff
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.storage.model.Visibility
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VersionOwnershipMatrixTest {
    @TestFactory
    fun `all twelve entrypoints require the current owner for every visibility`() =
        VersionServiceFixture.operations.flatMap { operation ->
            Visibility.entries.flatMap { visibility ->
                listOf("owner", "unrelated").map { actor ->
                    DynamicTest.dynamicTest("VER74-SVC-$operation-$visibility-$actor") {
                        runTest {
                            val f = VersionServiceFixture(visibility)
                            val before = f.snapshot()
                            val result = f.invoke(operation, actor)
                            if (actor != "owner") {
                                assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
                                assertEquals(before, f.snapshot())
                                verify { f.versions wasNot Called }
                                verify { f.backend wasNot Called }
                                verify { f.references wasNot Called }
                                if (operation == "deleteVersion") coVerify(exactly = 1) { f.scopes.findItemId("v1") }
                            } else {
                                val value = assertIs<Either.Right<*>>(result).value
                                assertOwnerResult(operation, f, value)
                            }
                        }
                    }
                }
            }
        }

    private fun assertOwnerResult(operation: String, f: VersionServiceFixture, value: Any?) {
        when (operation) {
            "createVersion", "restoreVersion" -> {
                assertCreated(operation, f, value)
            }
            "getHistory" -> {
                assertEquals(f.item, assertIs<FileVersionHistory>(value).item)
            }
            "getVersion" -> {
                assertEquals("v1", assertIs<FileVersion>(value).id)
            }
            "getLatestVersion" -> {
                assertEquals("v2", assertIs<FileVersion>(value).id)
            }
            "listVersions" -> {
                assertEquals(listOf("v2", "v1"), assertIs<List<*>>(value).map { assertIs<FileVersion>(it).id })
            }
            "deleteVersion" -> {
                assertEquals(listOf("v2"), f.rows.map { it.id })
                assertEquals(setOf("key2"), f.bytes.keys)
            }
            "applyRetentionPolicy" -> {
                assertEquals(listOf("v1"), value)
                assertEquals(listOf("v2"), f.rows.map { it.id })
            }
            "compareVersions" -> {
                assertCompared(f, value)
            }
            "getVersionCount" -> {
                assertEquals(2, value)
            }
            "getTotalVersionSize" -> {
                assertEquals(8L, value)
            }
            "cleanupVersionsForItem" -> {
                assertEquals(2, value)
                assertTrue(f.rows.isEmpty())
                assertEquals(setOf("key2"), f.bytes.keys)
            }
        }
    }

    private fun assertCreated(operation: String, f: VersionServiceFixture, value: Any?) {
        val row = assertIs<FileVersion>(value)
        assertEquals(3, row.versionNumber)
        assertEquals("owner", row.createdBy)
        assertEquals(row, f.rows.single { it.id == row.id })
        assertEquals(1, f.rows.count { it.isLatest })
        assertEquals(if (operation == "restoreVersion") 1 else null, row.restoredFrom)
    }

    private fun assertCompared(f: VersionServiceFixture, value: Any?) {
        val diff = assertIs<VersionDiff>(value)
        assertFalse(diff.isBinary)
        assertTrue(diff.patches.isNotEmpty())
        assertEquals(listOf("key1", "key2"), f.closedStreams)
    }
}
