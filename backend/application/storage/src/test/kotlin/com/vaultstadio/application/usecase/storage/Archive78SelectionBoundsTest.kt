package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.ArchiveLimits
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class Archive78SelectionBoundsTest : Archive78SelectionFixture() {
    @Test fun `empty selection rejects before any metadata lookup`() = runBlocking {
        assertEquals(400, (select(emptyList()) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.getItem(any(), any()) }
    }

    @Test fun `duplicate selection rejects before any metadata lookup`() = runBlocking {
        assertEquals(400, (select(listOf("a", "a")) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.getItem(any(), any()) }
    }

    @Test fun `selection count budget rejects before metadata`() = runBlocking {
        assertEquals(400, (select(listOf("a", "b"), ArchiveLimits(entries = 1)) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.getItem(any(), any()) }
    }

    @Test fun `blank identifier is not normalized into a valid selection`() = runBlocking {
        assertEquals(400, (select(listOf(" ")) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.getItem(any(), any()) }
    }

    @Test fun `declared file bytes exceeding the aggregate limit open no content`() = runBlocking {
        metadata(file(size = 4))
        assertEquals(413, (select(listOf("file78"), ArchiveLimits(inputBytes = 3)) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.downloadFile(any(), any()) }
    }

    @Test fun `aggregate declared bytes are counted across selected files`() = runBlocking {
        metadata(file("a", size = 3), file("b", size = 3))
        assertEquals(413, (select(listOf("a", "b"), ArchiveLimits(inputBytes = 5)) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.downloadFile(any(), any()) }
    }

    @Test fun `folder metadata size is not counted as file content`() = runBlocking {
        val root = folder().copy(size = 1000)
        val child = file(parent = root.id)
        metadata(root, child)
        children(root.id, child)
        val result = select(listOf(root.id), ArchiveLimits(inputBytes = 1)) as Either.Right
        assertEquals(listOf(root.id, child.id), result.value.map { it.item.id })
        coVerify(exactly = 0) { storage.downloadFile(any(), any()) }
    }

    @Test fun `entry limit includes directory entries`() = runBlocking {
        val root = folder()
        val child = file(parent = root.id)
        metadata(root, child)
        children(root.id, child)
        assertEquals(400, (select(listOf(root.id), ArchiveLimits(entries = 1)) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.downloadFile(any(), any()) }
    }

    @Test fun `folder depth limit prevents further recursive traversal`() = runBlocking {
        val root = folder()
        val nested = folder("nested", root.id)
        metadata(root, nested)
        children(root.id, nested)
        assertEquals(400, (select(listOf(root.id), ArchiveLimits(depth = 1)) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.listFolder(nested.id, any(), any()) }
    }

    @Test fun `a stalled page with hasMore cannot become a truncated success`() = runBlocking {
        val root = folder()
        metadata(root)
        coEvery { storage.listFolder(root.id, "owner78", any()) } returns
            PagedResult<com.vaultstadio.domain.storage.model.StorageItem>(emptyList(), 1, 0, 256).right()
        assertEquals(400, (select(listOf(root.id)) as Either.Left).value.httpStatus)
    }

    @Test fun `repeated child across pages is rejected rather than looping or duplicating`() = runBlocking {
        val root = folder()
        val child = file(parent = root.id)
        metadata(root, child)
        coEvery { storage.listFolder(root.id, "owner78", any()) } returnsMany listOf(
            PagedResult(listOf(child), 2, 0, 1).right(),
            PagedResult(listOf(child), 2, 1, 1).right(),
        )
        assertEquals(400, (select(listOf(root.id)) as Either.Left).value.httpStatus)
    }

    @Test fun `cyclic folder metadata cannot recurse indefinitely`() = runBlocking {
        val root = folder().copy(parentId = "folder78")
        metadata(root)
        children(root.id, root)
        assertEquals(400, (select(listOf(root.id)) as Either.Left).value.httpStatus)
    }

    @Test fun `a file at the permitted folder depth remains selectable`() = runBlocking {
        val root = folder()
        val child = file(parent = root.id)
        metadata(root, child)
        children(root.id, child)
        assertIs<Either.Right<*>>(select(listOf(root.id), ArchiveLimits(depth = 1)))
    }
}
