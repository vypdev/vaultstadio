package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ValidationException
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class StorageHierarchyTest {
    @Test
    fun `foreign ancestor denies creation without exposing ancestor names`() = runTest {
        val f = StorageAuthorizationFixture()
        coEvery { f.repository.findById("parent") } returns item(parentId = "foreign").right()
        coEvery { f.repository.findById("foreign") } returns item("foreign", FOREIGN).right()
        assertIs<AuthorizationException>(assertIs<Either.Left<*>>(f.parentWrite("create")).value)
        f.assertNoNameLookup()
        f.assertNoEffects()
    }

    @Test
    fun `breadcrumbs reject foreign ancestor rather than return partial metadata`() = runTest {
        val f = StorageAuthorizationFixture()
        f.source("parent")
        coEvery { f.repository.findById("parent") } returns item(parentId = "foreign").right()
        coEvery { f.repository.findById("foreign") } returns item("foreign", FOREIGN).right()
        assertIs<AuthorizationException>(assertIs<Either.Left<*>>(f.service.getBreadcrumbs("source", ACTOR)).value)
        f.assertNoEffects()
    }

    @Test
    fun `breadcrumbs allow owned trashed ancestors in root first order`() = runTest {
        val f = StorageAuthorizationFixture()
        f.source("parent")
        val parent = item(trashed = true, parentId = "root")
        val root = item("root")
        coEvery { f.repository.findById("parent") } returns parent.right()
        coEvery { f.repository.findById("root") } returns root.right()
        assertEquals(listOf(root, parent), assertIs<Either.Right<*>>(f.service.getBreadcrumbs("source", ACTOR)).value)
    }

    @Test
    fun `ancestor database failure keeps its typed identity`() = runTest {
        val f = StorageAuthorizationFixture()
        coEvery { f.repository.findById("parent") } returns item(parentId = "failed").right()
        val failure = DatabaseException("Unavailable")
        coEvery { f.repository.findById("failed") } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.parentWrite("upload")).value)
        f.assertNoEffects()
    }

    @Test
    fun `hierarchy cycles terminate without effects`() = runTest {
        val f = StorageAuthorizationFixture()
        coEvery { f.repository.findById("parent") } returns item(parentId = "parent").right()
        assertIs<ValidationException>(assertIs<Either.Left<*>>(f.parentWrite("upload")).value)
        coVerify(exactly = 1) { f.repository.findById("parent") }
        f.assertNoEffects()
    }

    @Test
    fun `moving into self or own descendant is rejected`() = runTest {
        for (destination in listOf("source", "child")) {
            val f = StorageAuthorizationFixture()
            f.source()
            coEvery { f.repository.findById("child") } returns item("child", parentId = "source").right()
            val result = f.service.moveItem(MoveItemInput("source", destination, userId = ACTOR))
            assertIs<ValidationException>(assertIs<Either.Left<*>>(result).value)
            f.assertNoEffects()
        }
    }

    @Test
    fun `128 ancestors succeed while a 129th fails before reading it`() = runTest {
        for (depth in listOf(128, 129)) {
            val f = StorageAuthorizationFixture()
            for (index in 0 until depth) {
                coEvery { f.repository.findById("p$index") } returns item(
                    id = "p$index", parentId = if (index + 1 < depth) "p${index + 1}" else null,
                ).right()
            }
            val result = ownedParentPath(f.repository, "p0", ACTOR)
            if (depth == 128) assertEquals("/p0", assertIs<Either.Right<*>>(result).value)
            else {
                assertIs<ValidationException>(assertIs<Either.Left<*>>(result).value)
                coVerify(exactly = 0) { f.repository.findById("p128") }
            }
            f.assertNoEffects()
        }
    }

    @Test
    fun `implicit root has no metadata lookup`() = runTest {
        val f = StorageAuthorizationFixture()
        assertEquals("", assertIs<Either.Right<*>>(ownedParentPath(f.repository, null, ACTOR)).value)
        coVerify(exactly = 0) { f.repository.findById(any()) }
    }
}
