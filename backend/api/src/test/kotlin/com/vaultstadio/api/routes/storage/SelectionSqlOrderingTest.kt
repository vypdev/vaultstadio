package com.vaultstadio.api.routes.storage

import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.SortField
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectionSqlOrderingTest {
    private val f = StorageIntegrationFixture()
    private val early = Instant.parse("2026-10-27T00:00:00Z")
    private val late = Instant.parse("2026-10-27T01:00:00Z")

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @BeforeEach fun reset() = runBlocking {
        f.reset()
        save(100, "pA", "/pA", type = ItemType.FOLDER)
        save(101, "pB", "/pB", type = ItemType.FOLDER)
        save(1, "same", "/pA/same", 100, size = 10)
        save(2, "same", "/pB/same", 101, size = 10)
        save(3, "alpha", "/pA/alpha", 100, size = 30, created = late)
        save(4, "zeta", "/pA/zeta", 100, size = 20, updated = late)
        save(5, "alpha", "/alpha", owner = f.bob.id)
        save(6, "hidden", "/hidden", trashed = true)
        save(7, "folder", "/pA/folder", 100, type = ItemType.FOLDER)
    }

    @ParameterizedTest(name = "P91-sql-stable-order {0}/{1}")
    @CsvSource(
        "NAME,ASC,3|1|2|4", "NAME,DESC,4|1|2|3", "SIZE,ASC,1|2|4|3", "SIZE,DESC,3|4|1|2",
        "CREATED_AT,ASC,1|2|4|3", "CREATED_AT,DESC,3|1|2|4", "UPDATED_AT,ASC,1|2|3|4", "UPDATED_AT,DESC,4|1|2|3",
        "TYPE,ASC,3|1|2|4|7|100|101", "TYPE,DESC,7|100|101|3|1|2|4",
    )
    fun pagesKeepUniqueStableOrdering(field: SortField, order: SortOrder, sequence: String) = runBlocking {
        val expected = sequence.split('|').map { id(it.toInt()) }
        val query = StorageItemQuery(
            ownerId = f.alice.id,
            isTrashed = false,
            type = if (field ==
                SortField.TYPE
            ) {
                null
            } else {
                ItemType.FILE
            },
            sortField = field,
            sortOrder = order,
            limit = 2,
        )
        val collected = (expected.indices step 2).flatMap { offset ->
            val page = select(query.copy(offset = offset))
            assertEquals(expected.size.toLong(), page.total)
            assertEquals((expected.size + 1L) / 2, page.totalPages)
            assertEquals(offset / 2 + 1L, page.currentPage)
            assertEquals(offset + page.items.size < expected.size, page.hasMore)
            page.items.map { it.id }
        }
        assertEquals(expected, collected)
        assertEquals(expected, select(query.copy(limit = 1000)).items.map { it.id })
        val beyond = select(query.copy(offset = expected.size + 3))
        assertEquals(0, beyond.items.size)
        assertEquals(expected.size.toLong(), beyond.total)
        assertFalse(beyond.hasMore)
    }

    @ParameterizedTest(name = "P91-sql-filter-boundary {0}")
    @ValueSource(strings = ["root", "child", "trash", "foreign", "empty", "max-offset"])
    fun selectionPredicatesAndPageMetadataRemainBound(case: String) = runBlocking {
        val page = select(predicateQuery(case))
        val expected = expectedPredicateIds(case)
        assertEquals(expected.map(::id), page.items.map { it.id })
        if (case == "max-offset") {
            assertEquals(7L, page.total)
            assertEquals(2147483648L, page.currentPage)
            assertFalse(page.hasMore)
        } else {
            assertEquals(expected.size.toLong(), page.total)
            assertEquals(if (expected.isEmpty()) 0L else 1L, page.totalPages)
        }
    }

    private fun predicateQuery(case: String) = StorageItemQuery(
        ownerId = if (case == "foreign") f.bob.id else f.alice.id,
        filterByParent = case != "max-offset",
        parentId = when (case) {
            "child" -> id(100)
            "empty" -> id(7)
            else -> null
        },
        isTrashed = case == "trash",
        offset = if (case == "max-offset") Int.MAX_VALUE else 0,
        limit = if (case == "max-offset") 1 else 100,
    )

    private fun expectedPredicateIds(case: String): List<Int> = when (case) {
        "root" -> listOf(100, 101)
        "child" -> listOf(3, 7, 1, 4)
        "trash" -> listOf(6)
        "foreign" -> listOf(5)
        else -> emptyList()
    }

    private suspend fun select(query: StorageItemQuery) = f.repository.query(query).fold({ throw it }, { it })

    private suspend fun save(
        number: Int,
        name: String,
        path: String,
        parent: Int? = null,
        owner: String = f.alice.id,
        type: ItemType = ItemType.FILE,
        size: Long = 0,
        created: Instant = early,
        updated: Instant = early,
        trashed: Boolean = false,
    ) {
        f.repository.create(
            StorageItem(
                id = id(number), name = name, path = path, parentId = parent?.let(::id),
                ownerId = owner, type = type, size = size,
                createdAt = created, updatedAt = updated, isTrashed = trashed,
            ),
        )
            .fold({ throw it }, {})
    }

    private fun id(number: Int) = "00000000-0000-0000-0000-" + number.toString().padStart(12, '0')
}
