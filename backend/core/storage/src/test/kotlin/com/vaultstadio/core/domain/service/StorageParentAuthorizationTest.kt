package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.Visibility
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class StorageParentAuthorizationTest {
    private val writes = listOf("create", "get-or-create", "upload", "move", "copy", "restore")

    @TestFactory
    fun `foreign parents never grant writes regardless of visibility`() = writes.flatMap { operation ->
        Visibility.entries.map { visibility ->
            DynamicTest.dynamicTest("$operation rejects foreign $visibility parent") {
                runTest {
                    val f = StorageAuthorizationFixture()
                    f.source(parentId = "parent")
                    coEvery { f.repository.findById("parent") } returns item(owner = FOREIGN, visibility = visibility).right()
                    assertIs<AuthorizationException>(assertIs<Either.Left<*>>(f.parentWrite(operation)).value)
                    f.assertNoNameLookup()
                    f.assertNoEffects()
                }
            }
        }
    }

    @TestFactory
    fun `invalid owned parents never start an effect`() = writes.flatMap { operation ->
        listOf("file", "trashed", "missing", "database-error").map { invalid ->
            DynamicTest.dynamicTest("$operation rejects $invalid parent") {
                runTest {
                    val f = StorageAuthorizationFixture()
                    f.source(parentId = "parent")
                    val failure = DatabaseException("Unavailable")
                    coEvery { f.repository.findById("parent") } returns when (invalid) {
                        "file" -> item(type = ItemType.FILE).right()
                        "trashed" -> item(trashed = true).right()
                        "missing" -> null.right()
                        else -> failure.left()
                    }
                    val error = assertIs<Either.Left<*>>(f.parentWrite(operation)).value
                    when (invalid) {
                        "missing" -> assertIs<ItemNotFoundException>(error)
                        "database-error" -> assertSame(failure, error)
                        else -> assertIs<ValidationException>(error)
                    }
                    f.assertNoNameLookup()
                    f.assertNoEffects()
                }
            }
        }
    }

    @TestFactory
    fun `foreign invalid parents are forbidden without revealing their state`() = writes.map { operation ->
        DynamicTest.dynamicTest("$operation checks ownership before type and trash") {
            runTest {
                val f = StorageAuthorizationFixture()
                f.source(parentId = "parent")
                coEvery { f.repository.findById("parent") } returns item(owner = FOREIGN, type = ItemType.FILE, trashed = true).right()
                val error = assertIs<AuthorizationException>(assertIs<Either.Left<*>>(f.parentWrite(operation)).value)
                assertEquals("Access denied", error.message)
                f.assertNoEffects()
            }
        }
    }
}
