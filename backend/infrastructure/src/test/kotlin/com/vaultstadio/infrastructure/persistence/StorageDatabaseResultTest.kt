package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.domain.common.exception.DatabaseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class StorageDatabaseResultTest {
    @Test fun successfulNullableResultIsPreserved() = runBlocking {
        val result = storageDatabaseResult("read") { null }
        assertEquals(null, result.getOrNull())
        assertEquals(true, result.isRight())
    }

    @Test fun successfulValueIsPreservedByIdentity() = runBlocking {
        val value = Any()
        assertSame(value, storageDatabaseResult("read") { value }.getOrNull())
    }

    @ParameterizedTest
    @ValueSource(strings = ["sql", "argument", "state", "missing"])
    fun expectedProviderFailureRetainsItsCause(kind: String) = runBlocking {
        val failure = when (kind) {
            "sql" -> SQLException("private database diagnostic")
            "argument" -> IllegalArgumentException("private enum diagnostic")
            "state" -> IllegalStateException("private transaction diagnostic")
            else -> NoSuchElementException("private mapping diagnostic")
        }
        val result = storageDatabaseResult("safe provider context") { throw failure }
        result.fold(
            {
                assertIs<DatabaseException>(it)
                assertEquals("safe provider context", it.message)
                assertSame(failure, it.cause)
            },
            { error("Provider failure became success") },
        )
    }

    @Test fun cancellationRemainsControlFlowByIdentity() = runBlocking {
        val cancellation = CancellationException("cancelled selection")
        val thrown = assertFailsWith<CancellationException> {
            storageDatabaseResult("read") { throw cancellation }
        }
        assertSame(cancellation, thrown)
    }

    @Test fun unexpectedFailureIsNotMisclassifiedAsADatabaseError() = runBlocking {
        val failure = UnsupportedOperationException("unexpected implementation failure")
        assertSame(
            failure,
            assertFailsWith<UnsupportedOperationException> {
                storageDatabaseResult("read") { throw failure }
            },
        )
    }

    @Test fun fatalFailureIsNeverConvertedToAResult() = runBlocking {
        val failure = AssertionError("fatal fixture")
        assertSame(
            failure,
            assertFailsWith<AssertionError> {
                storageDatabaseResult("read") { throw failure }
            },
        )
    }
}
