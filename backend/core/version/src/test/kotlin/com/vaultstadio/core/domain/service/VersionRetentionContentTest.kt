package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageBackendException
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

class VersionRetentionContentTest {
    @Test
    fun `retention reclaims removed unreferenced content`() = runTest {
        val f = VersionServiceFixture()
        assertIs<Either.Right<*>>(f.service.applyRetentionPolicy("item", "owner"))
        assertEquals(listOf("v2"), f.rows.map { it.id })
        assertFalse(f.bytes.containsKey("key1"))
        assertEquals("new\n", f.bytes["key2"]?.decodeToString())
    }

    @Test
    fun `retention preserves content referenced by the current file`() = runTest {
        val f = VersionServiceFixture()
        f.item = f.item.copy(storageKey = "key1")
        assertIs<Either.Right<*>>(f.service.applyRetentionPolicy("item", "owner"))
        assertEquals(listOf("v2"), f.rows.map { it.id })
        assertEquals("old\n", f.bytes["key1"]?.decodeToString())
        verify { f.backend wasNot Called }
    }

    @Test
    fun `retention preserves content referenced by a retained version`() = runTest {
        val f = VersionServiceFixture()
        f.rows[1] = f.rows[1].copy(storageKey = "key1")
        assertIs<Either.Right<*>>(f.service.applyRetentionPolicy("item", "owner"))
        assertEquals(listOf("v2"), f.rows.map { it.id })
        assertEquals("old\n", f.bytes["key1"]?.decodeToString())
        verify { f.backend wasNot Called }
    }

    @Test
    fun `retention deletes a shared unreferenced key only once`() = runTest {
        val f = VersionServiceFixture()
        f.rows.add(f.rows.first().copy(id = "v3", versionNumber = 3))
        assertIs<Either.Right<*>>(f.service.applyRetentionPolicy("item", "owner"))
        assertEquals(listOf("v2"), f.rows.map { it.id })
        assertFalse(f.bytes.containsKey("key1"))
        coVerify(exactly = 1) { f.backend.delete("key1") }
    }

    @Test
    fun `retention policy failure preserves rows and all bytes`() = runTest {
        val f = VersionServiceFixture()
        val before = f.snapshot()
        val failure = DatabaseException("policy failure")
        coEvery { f.versions.applyRetentionPolicy(any(), any()) } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.applyRetentionPolicy("item", "owner")).value)
        assertEquals(before, f.snapshot())
        verify {
            f.backend wasNot Called
            f.references wasNot Called
        }
    }

    @Test
    fun `reference failure after metadata retention preserves remaining bytes and is not success`() = runTest {
        val f = VersionServiceFixture()
        val bytes = f.snapshot().third
        val failure = DatabaseException("reference failure")
        coEvery { f.references.hasOtherReferences(any(), any()) } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.applyRetentionPolicy("item", "owner")).value)
        assertEquals(listOf("v2"), f.rows.map { it.id })
        assertEquals(bytes, f.snapshot().third)
        verify { f.backend wasNot Called }
    }

    @Test
    fun `retention content deletion failure propagates after metadata and retains current bytes`() = runTest {
        val f = VersionServiceFixture()
        val bytes = f.snapshot().third
        val failure = StorageBackendException("LOCAL", "delete failed")
        coEvery { f.backend.delete(any()) } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.applyRetentionPolicy("item", "owner")).value)
        assertEquals(listOf("v2"), f.rows.map { it.id })
        assertEquals(bytes, f.snapshot().third)
    }

    @Test
    fun `a later reference failure prevents every retention byte deletion`() = runTest {
        val f = VersionServiceFixture()
        f.rows.add(f.rows.first().copy(id = "v3", versionNumber = 3, storageKey = "key3"))
        f.bytes["key3"] = "third\n".toByteArray()
        val bytes = f.snapshot().third
        val failure = DatabaseException("late reference failure")
        coEvery { f.references.hasOtherReferences("key3", "v3") } returns false.right()
        coEvery { f.references.hasOtherReferences("key1", "v1") } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.applyRetentionPolicy("item", "owner")).value)
        assertEquals(bytes, f.snapshot().third)
        verify { f.backend wasNot Called }
        coVerifyOrder {
            f.references.hasOtherReferences("key3", "v3")
            f.references.hasOtherReferences("key1", "v1")
        }
    }

    @Test
    fun `empty retention does not query references or delete content`() = runTest {
        val f = VersionServiceFixture()
        val before = f.snapshot()
        coEvery { f.versions.applyRetentionPolicy(any(), any()) } returns emptyList<String>().right()
        assertEquals(
            emptyList<String>(),
            assertIs<Either.Right<*>>(f.service.applyRetentionPolicy("item", "owner")).value,
        )
        assertEquals(before, f.snapshot())
        verify {
            f.backend wasNot Called
            f.references wasNot Called
        }
    }
}
