package com.vaultstadio.domain.storage.model

import arrow.core.Either
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ValidationException
import kotlinx.datetime.Clock
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertIs
import kotlin.test.assertSame

class StorageAccessPolicyTest {
    @TestFactory
    fun `folder access matrix`() = Visibility.entries.flatMap { visibility ->
        listOf(true, false).flatMap { owner ->
            ItemType.entries.flatMap { type ->
                listOf(true, false).flatMap { trashed ->
                    listOf(true, false).map { active ->
                        DynamicTest.dynamicTest("$visibility owner=$owner $type trashed=$trashed active=$active") {
                            val now = Clock.System.now()
                            val item = StorageItem(
                                name = "test", path = "/test", ownerId = if (owner) "actor" else "foreign",
                                type = type, visibility = visibility, isTrashed = trashed,
                                createdAt = now, updatedAt = now,
                            )
                            val result = StorageAccessPolicy.requireFolder(item, "actor", active)
                            when {
                                !owner -> assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
                                type != ItemType.FOLDER || (active && trashed) ->
                                    assertIs<ValidationException>(assertIs<Either.Left<*>>(result).value)
                                else -> assertSame(item, assertIs<Either.Right<*>>(result).value)
                            }
                        }
                    }
                }
            }
        }
    }
}
