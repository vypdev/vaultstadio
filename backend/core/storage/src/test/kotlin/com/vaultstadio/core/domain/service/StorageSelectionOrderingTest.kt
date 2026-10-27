package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StorageSelectionOrderingTest {
    private val repository = mockk<StorageItemRepository>()
    private val backend = mockk<StorageBackend>()
    private val events = mockk<EventBus>()
    private val service = StorageService(repository, backend, events)

    @ParameterizedTest(name = "P91-service-no-effect {0}/{1}/{2}")
    @CsvSource(
        "root,-1,0", "root,0,0", "root,1001,0", "root,2147483647,0",
        "parent,-1,0", "parent,0,0", "parent,1001,0", "parent,2147483647,0",
        "search,-1,0", "search,0,0", "search,1001,0", "search,2147483647,0",
        "recent,-1,0", "recent,0,0", "recent,1001,0", "recent,2147483647,0",
        "root,1,-1", "root,1,-2147483648", "parent,1,-1", "parent,1,-2147483648",
        "search,1,-1", "search,1,-2147483648", "root,0,-1", "search,0,-1",
    )
    fun invalidInputsPrecedeEvenParentLookup(operation: String, limit: Int, offset: Int) = runTest {
        val result = invoke(operation, limit, offset)
        assertTrue(result.isLeft())
        val failure = result.leftOrNull()
        assertTrue(failure is ValidationException)
        assertEquals(if (limit !in 1..1000) "limit" else "offset", failure.field)
        verify {
            repository wasNot Called
            backend wasNot Called
            events wasNot Called
        }
    }

    @ParameterizedTest(name = "P91-service-valid {0}/{1}")
    @CsvSource(
        "root,1", "root,100", "root,1000", "search,1", "search,50", "search,1000",
        "recent,1", "recent,20", "recent,1000",
    )
    fun validRequestsKeepExactBoundsAndActor(operation: String, limit: Int) = runTest {
        coEvery { repository.query(any()) } answers
            {
                val query = firstArg<StorageItemQuery>()
                PagedResult(emptyList<StorageItem>(), 0, query.offset, query.limit).right()
            }
        assertTrue(invoke(operation, limit, 0).isRight())
        coVerify(exactly = 1) {
            repository.query(match { it.ownerId == "actor" && it.limit == limit && it.offset == 0 })
        }
        if (operation == "root") coVerify { repository.query(match { it.parentId == null && it.filterByParent }) }
        if (operation != "root") coVerify { repository.query(match { it.isTrashed == false }) }
        verify {
            backend wasNot Called
            events wasNot Called
        }
    }

    @ParameterizedTest(name = "P91-service-provider-failure {0}")
    @ValueSource(strings = ["root", "search", "recent"])
    fun databaseFailureIsNotValidationOrAnEmptySuccess(operation: String) = runTest {
        val failure = DatabaseException("selection unavailable")
        coEvery { repository.query(any()) } returns failure.left()
        assertSame(failure, invoke(operation, 1, 0).leftOrNull())
    }

    @ParameterizedTest(name = "P91-service-cancellation {0}")
    @ValueSource(strings = ["root", "search", "recent"])
    fun cancellationRemainsControlFlow(operation: String) = runTest {
        val cancellation = CancellationException("selection cancelled")
        coEvery { repository.query(any()) } throws cancellation
        assertSame(cancellation, assertFailsWith<CancellationException> { invoke(operation, 1, 0) })
    }

    private suspend fun invoke(operation: String, limit: Int, offset: Int): Either<*, *> = when (operation) {
        "root" -> service.listFolder(
            null,
            "actor",
            StorageItemQuery(ownerId = "forged", parentId = "forged", limit = limit, offset = offset),
        )
        "parent" -> service.listFolder("foreign-parent", "actor", StorageItemQuery(limit = limit, offset = offset))
        "search" -> service.search("prefix", "actor", limit, offset)
        else -> service.getRecentItems("actor", limit)
    }
}
