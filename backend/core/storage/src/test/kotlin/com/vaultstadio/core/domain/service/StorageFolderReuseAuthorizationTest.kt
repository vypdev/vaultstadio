package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

class StorageFolderReuseAuthorizationTest {
    @TestFactory
    fun `get or create refuses invalid existing results`() = listOf("foreign", "trashed", "file").map { invalid ->
        DynamicTest.dynamicTest("does not return existing $invalid folder") {
            runTest {
                val f = StorageAuthorizationFixture()
                val existing = when (invalid) {
                    "foreign" -> item(owner = FOREIGN)
                    "trashed" -> item(trashed = true)
                    else -> item(type = ItemType.FILE)
                }
                coEvery { f.repository.findByPath("/child", ACTOR) } returns existing.right()
                val error = assertIs<Either.Left<*>>(f.service.getOrCreateFolder("child", null, ACTOR)).value
                if (invalid ==
                    "foreign"
                ) {
                    assertIs<AuthorizationException>(error)
                } else {
                    assertIs<ValidationException>(error)
                }
                f.assertNoEffects()
            }
        }
    }

    @Test
    fun `owned existing active folder is reused without events`() = runTest {
        val f = StorageAuthorizationFixture()
        val existing = item()
        coEvery { f.repository.findByPath("/child", ACTOR) } returns existing.right()
        val result = f.service.getOrCreateFolder("child", null, ACTOR).fold({ throw it }, { it })
        assertSame(existing, result.folder)
        assertFalse(result.created)
        f.assertNoEffects()
    }

    @Test
    fun `get or create creates an owned child after validating all parents`() = runTest {
        val f = StorageAuthorizationFixture()
        coEvery { f.repository.findById("parent") } returns item().right()
        coEvery { f.repository.findByPath("/parent/child", ACTOR) } returns null.right()
        coEvery { f.repository.create(any()) } answers { firstArg<StorageItem>().right() }
        val result = f.service.getOrCreateFolder("child", "parent", ACTOR).fold({ throw it }, { it })
        assertEquals("/parent/child", result.folder.path)
        assertEquals(ACTOR, result.folder.ownerId)
        assertEquals("parent", result.folder.parentId)
        assertEquals(true, result.created)
    }
}
