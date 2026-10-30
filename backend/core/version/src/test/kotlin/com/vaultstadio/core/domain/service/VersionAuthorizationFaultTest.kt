package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
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
import kotlin.test.assertSame

class VersionAuthorizationFaultTest {
    @TestFactory
    fun `blank missing and failed canonical scope stop every operation`(): List<DynamicTest> {
        return VersionServiceFixture.operations.flatMap { operation ->
            listOf("blank-actor", "missing-item", "item-failure").map { fault ->
                DynamicTest.dynamicTest("VER74-FAULT-$operation-$fault") {
                    runTest {
                        val f = VersionServiceFixture()
                        val before = f.snapshot()
                        val failure = arrange(f, fault)
                        val error = assertIs<Either.Left<*>>(f.invoke(operation, actorFor(fault))).value
                        assertRejection(f, fault, failure, error)
                        assertEquals(before, f.snapshot())
                        verify { f.versions wasNot Called }
                        verify { f.backend wasNot Called }
                        verify { f.references wasNot Called }
                    }
                }
            }
        }
    }

    private fun actorFor(fault: String) = if (fault == "blank-actor") " " else "owner"

    private fun arrange(f: VersionServiceFixture, fault: String): DatabaseException {
        val failure = DatabaseException("private provider diagnostic")
        if (fault == "missing-item") coEvery { f.items.findById("item") } returns null.right()
        if (fault == "item-failure") coEvery { f.items.findById("item") } returns failure.left()
        return failure
    }

    private fun assertRejection(f: VersionServiceFixture, fault: String, failure: DatabaseException, error: Any?) {
        when (fault) {
            "blank-actor" -> {
                assertIs<AuthorizationException>(error)
                coVerify(exactly = 0) {
                    f.items.findById(any())
                    f.scopes.findItemId(any())
                }
            }
            "missing-item" -> {
                assertIs<ItemNotFoundException>(error)
            }
            else -> {
                assertSame(failure, error)
            }
        }
    }

    @Test
    fun `scope resolution missing and provider failure cannot fetch a complete version`() = runTest {
        for (missing in listOf(true, false)) {
            val f = VersionServiceFixture()
            val failure = DatabaseException("scope provider failure")
            coEvery { f.scopes.findItemId("v1") } returns if (missing) null.right() else failure.left()
            val error = assertIs<Either.Left<*>>(f.service.deleteVersion("v1", "owner")).value
            if (missing) assertIs<ItemNotFoundException>(error) else assertSame(failure, error)
            verify { f.items wasNot Called }
            verify { f.versions wasNot Called }
            verify { f.backend wasNot Called }
            verify { f.references wasNot Called }
        }
    }

    @Test
    fun `latest semantic rejection cannot disclose history to a previous creator`() = runTest {
        val f = VersionServiceFixture()
        assertIs<AuthorizationException>(
            assertIs<Either.Left<*>>(f.service.deleteVersion("v2", "previous-owner")).value,
        )
        coVerify(exactly = 0) { f.versions.findById(any()) }
    }

    @Test
    fun `canonical item identity must match even when its claimed owner matches`() = runTest {
        val f = VersionServiceFixture()
        coEvery { f.items.findById("item") } returns f.item.copy(id = "different").right()
        assertIs<DatabaseException>(assertIs<Either.Left<*>>(f.service.getHistory("item", "owner")).value)
        verify { f.versions wasNot Called }
    }
}
