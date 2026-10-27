package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.domain.common.exception.DatabaseException
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VersionRetainedContentTest {
    @Test
    fun `deleting the source version preserves bytes referenced by its restored version`() = runTest {
        val f = VersionServiceFixture()
        val restored = assertIs<FileVersion>(
            assertIs<Either.Right<*>>(f.service.restoreVersion(RestoreVersionInput("item", 1), "owner")).value,
        )
        assertIs<Either.Right<*>>(f.service.deleteVersion("v1", "owner"))
        assertTrue(f.rows.any { it.id == restored.id && it.storageKey == "key1" })
        assertEquals("old\n", f.bytes["key1"]?.decodeToString())
    }

    @Test
    fun `purging history preserves the current file content`() = runTest {
        val f = VersionServiceFixture()
        assertIs<Either.Right<*>>(f.service.cleanupVersionsForItem("item", "owner"))
        assertTrue(f.rows.isEmpty())
        assertEquals("new\n", f.bytes[f.item.storageKey]?.decodeToString())
    }

    @TestFactory
    fun `reference lookup failure stops metadata and byte deletion`() =
        listOf("deleteVersion", "cleanupVersionsForItem").map { operation ->
            DynamicTest.dynamicTest("VER74-CONTENT-FAIL-$operation") {
                runTest {
                    val f = VersionServiceFixture()
                    val before = f.snapshot()
                    val failure = DatabaseException("private reference diagnostic")
                    coEvery { f.references.hasOtherReferences(any(), any()) } returns failure.left()
                    assertSame(failure, assertIs<Either.Left<*>>(f.invoke(operation, "owner")).value)
                    assertEquals(before, f.snapshot())
                    verify { f.backend wasNot Called }
                    coVerify(exactly = 0) {
                        f.versions.delete(any())
                        f.versions.deleteAllForItem(any())
                    }
                }
            }
        }

    @Test
    fun `a later reference failure cannot delete an earlier unreferenced blob`() = runTest {
        val f = VersionServiceFixture()
        f.item = f.item.copy(storageKey = null)
        val before = f.snapshot()
        val failure = DatabaseException("later reference lookup failed")
        coEvery { f.references.hasOtherReferences("key2", setOf("v1", "v2")) } returns false.right()
        coEvery { f.references.hasOtherReferences("key1", setOf("v1", "v2")) } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.cleanupVersionsForItem("item", "owner")).value)
        assertEquals(before, f.snapshot())
        verify { f.backend wasNot Called }
        coVerify(exactly = 0) { f.versions.deleteAllForItem(any()) }
    }

    @Test
    fun `purging restored history removes shared historical bytes but retains current file`() = runTest {
        val f = VersionServiceFixture()
        assertIs<Either.Right<*>>(f.service.restoreVersion(RestoreVersionInput("item", 1), "owner"))
        assertIs<Either.Right<*>>(f.service.cleanupVersionsForItem("item", "owner"))
        assertTrue(f.rows.isEmpty())
        assertFalse(f.bytes.containsKey("key1"))
        assertEquals("new\n", f.bytes["key2"]?.decodeToString())
    }
}
