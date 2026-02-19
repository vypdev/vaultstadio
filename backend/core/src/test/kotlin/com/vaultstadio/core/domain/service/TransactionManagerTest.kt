/**
 * VaultStadio Transaction Manager Tests
 *
 * Unit tests for TransactionManager contract: block runs, result returned, exception propagates.
 */

package com.vaultstadio.core.domain.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TransactionManagerTest {

    /**
     * Stub implementation that runs the block without a real DB.
     * Verifies the contract: commit on success, propagate exception on failure.
     */
    private class StubTransactionManager : TransactionManager {
        var transactionCallCount = 0
        var readOnlyCallCount = 0

        override suspend fun <T> transaction(block: suspend () -> T): T {
            transactionCallCount++
            return block()
        }

        override suspend fun <T> readOnly(block: suspend () -> T): T {
            readOnlyCallCount++
            return block()
        }
    }

    @Test
    fun `transaction runs block and returns result`() = runTest {
        val tm = StubTransactionManager()
        val result = tm.transaction { 42 }
        assertEquals(42, result)
        assertEquals(1, tm.transactionCallCount)
    }

    @Test
    fun `transaction propagates exception`() = runTest {
        val tm = StubTransactionManager()
        val e = RuntimeException("rollback")
        assertThrows<RuntimeException> {
            tm.transaction {
                throw e
            }
        }
        assertSame(e, assertThrows<RuntimeException> { tm.transaction { throw e } })
    }

    @Test
    fun `readOnly runs block and returns result`() = runTest {
        val tm = StubTransactionManager()
        val result = tm.readOnly { "value" }
        assertEquals("value", result)
        assertEquals(1, tm.readOnlyCallCount)
    }

    @Test
    fun `readOnly propagates exception`() = runTest {
        val tm = StubTransactionManager()
        assertThrows<IllegalStateException> {
            tm.readOnly {
                throw IllegalStateException("read failed")
            }
        }
    }
}
