package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import arrow.core.left
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class Archive78SelectionFailureTest : Archive78SelectionFixture() {
    @Test fun `root owner denial propagates before any content operation`() = runBlocking {
        val denial = AuthorizationException()
        coEvery { storage.getItem("foreign78", "owner78") } returns denial.left()
        assertSame(denial, (select(listOf("foreign78")) as Either.Left).value)
        coVerify(exactly = 0) { storage.downloadFile(any(), any()) }
    }

    @Test fun `root database failure retains its typed identity`() = runBlocking {
        val error = DatabaseException("private diagnostic")
        coEvery { storage.getItem("file78", "owner78") } returns error.left()
        assertSame(error, (select(listOf("file78")) as Either.Left).value)
    }

    @Test fun `listing provider failure is not an empty folder success`() = runBlocking {
        val root = folder()
        metadata(root)
        val error = DatabaseException("private diagnostic")
        coEvery { storage.listFolder(root.id, "owner78", any()) } returns error.left()
        assertSame(error, (select(listOf(root.id)) as Either.Left).value)
    }

    @Test fun `descendant owner change after listing is denied before content`() = runBlocking {
        val root = folder()
        val child = file(parent = root.id)
        metadata(root)
        children(root.id, child)
        val denial = AuthorizationException()
        coEvery { storage.getItem(child.id, "owner78") } returns denial.left()
        assertSame(denial, (select(listOf(root.id)) as Either.Left).value)
        coVerify(exactly = 0) { storage.downloadFile(any(), any()) }
    }

    @Test fun `moving a listed child outside its parent invalidates the selection`() = runBlocking {
        val root = folder()
        val child = file(parent = "different-parent")
        metadata(root, child)
        children(root.id, child)
        assertEquals(400, (select(listOf(root.id)) as Either.Left).value.httpStatus)
    }

    @ParameterizedTest
    @ValueSource(strings = ["..", "nested/file", "nested\\file"])
    fun `unsafe canonical name cannot become an extraction path`(name: String) = runBlocking {
        metadata(file(name = name))
        assertEquals(400, (select(listOf("file78")) as Either.Left).value.httpStatus)
        coVerify(exactly = 0) { storage.downloadFile(any(), any()) }
    }

    @Test fun `negative canonical size is a bounded failure`() = runBlocking {
        metadata(file(size = -1))
        assertEquals(413, (select(listOf("file78")) as Either.Left).value.httpStatus)
    }

    @Test fun `independent roots with the same ZIP path are ambiguous`() = runBlocking {
        metadata(file("a", "same"), file("b", "same"))
        assertEquals(400, (select(listOf("a", "b")) as Either.Left).value.httpStatus)
    }
}
