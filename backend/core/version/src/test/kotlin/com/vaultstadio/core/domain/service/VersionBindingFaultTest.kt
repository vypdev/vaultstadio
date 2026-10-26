package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersionHistory
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VersionBindingFaultTest {
    private data class Fault(val name: String, val operation: String, val configure: VersionServiceFixture.() -> Unit)

    private val faults = listOf(
        Fault("detail-item", "getVersion") {
            coEvery { versions.findByItemAndVersion("item", 1) } returns foreign().right()
        },
        Fault("detail-number", "getVersion") {
            coEvery { versions.findByItemAndVersion("item", 1) } returns rows.last().right()
        },
        Fault("latest", "getLatestVersion") {
            coEvery { versions.findLatest("item") } returns foreign().right()
        },
        Fault("history-item", "getHistory") {
            coEvery { versions.getHistory("item") } returns
                FileVersionHistory(item.copy(id = "foreign"), rows, 2, 8).right()
        },
        Fault("history-rows", "getHistory") {
            coEvery { versions.getHistory("item") } returns FileVersionHistory(item, listOf(foreign()), 1, 4).right()
        },
        Fault("list", "listVersions") {
            coEvery { versions.listVersions(any(), any(), any()) } returns listOf(rows.last(), foreign()).right()
        },
        Fault("retention", "applyRetentionPolicy") {
            coEvery { versions.listVersions(any(), any(), any()) } returns listOf(rows.last(), foreign()).right()
        },
        Fault("cleanup", "cleanupVersionsForItem") {
            coEvery { versions.listVersions(any(), any(), any()) } returns listOf(rows.last(), foreign()).right()
        },
        Fault("delete-id", "deleteVersion") {
            coEvery { versions.findById("v1") } returns rows.first().copy(id = "not-v1").right()
        },
        Fault("delete-scope", "deleteVersion") {
            coEvery { scopes.findItemId("v1") } returns "stale-item".right()
            coEvery { items.findById("stale-item") } returns item.copy(id = "stale-item").right()
        },
        Fault("compare-from", "compareVersions") {
            coEvery { versions.findByItemAndVersion("item", 1) } returns foreign().right()
        },
        Fault("restore", "restoreVersion") {
            coEvery { versions.findByItemAndVersion("item", 1) } returns foreign().right()
        },
        Fault("compare-to", "compareVersions") {
            coEvery { versions.findByItemAndVersion("item", 2) } returns foreign().right()
        },
        Fault("create-latest", "createVersion") {
            coEvery { versions.findLatest("item") } returns foreign().right()
        },
    )

    private fun VersionServiceFixture.foreign() = rows.first().copy(itemId = "foreign-item")

    @TestFactory
    fun `repository bindings reject mismatched identities`() = faults.map { fault ->
        DynamicTest.dynamicTest("VER74-BIND-${fault.name}") {
            runTest {
                val f = VersionServiceFixture()
                fault.configure(f)
                val before = f.snapshot()
                assertIs<DatabaseException>(assertIs<Either.Left<*>>(f.invoke(fault.operation, "owner")).value)
                assertEquals(before, f.snapshot())
                verify { f.backend wasNot Called }
                verify { f.references wasNot Called }
                coVerify(exactly = 0) {
                    f.versions.create(any())
                    f.versions.update(any())
                    f.versions.delete(any())
                    f.versions.applyRetentionPolicy(any(), any())
                    f.versions.deleteAllForItem(any())
                }
            }
        }
    }

    @Test
    fun `history uses current canonical metadata instead of stale repository names`() = runTest {
        val f = VersionServiceFixture()
        coEvery { f.versions.getHistory("item") } returns
            FileVersionHistory(f.item.copy(name = "stale", ownerId = "previous-owner"), f.rows, 2, 8).right()
        val history = assertIs<Either.Right<*>>(f.service.getHistory("item", "owner")).value
        assertEquals(f.item, assertIs<FileVersionHistory>(history).item)
    }

    @Test
    fun `retention refuses returned IDs outside its authorized snapshot`() = runTest {
        val f = VersionServiceFixture()
        val before = f.snapshot()
        coEvery { f.versions.applyRetentionPolicy(any(), any()) } returns listOf("private-foreign-id").right()
        val error =
            assertIs<DatabaseException>(assertIs<Either.Left<*>>(f.service.applyRetentionPolicy("item", "owner")).value)
        assertTrue(!error.message.contains("private-foreign-id"))
        assertEquals(before, f.snapshot())
        verify { f.backend wasNot Called }
        verify { f.references wasNot Called }
    }
}
