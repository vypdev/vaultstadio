/**
 * VaultStadio Storage Item Repository Tests
 *
 * Integration tests for ExposedStorageItemRepository using Testcontainers PostgreSQL.
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.domain.storage.repository.SortField
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import com.vaultstadio.infrastructure.persistence.entities.StorageItemMetadataTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import com.vaultstadio.infrastructure.persistence.entities.UsersTable
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedStorageItemRepositoryTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("vaultstadio_test")
            withUsername("test")
            withPassword("test")
        }
    }

    private lateinit var storageItemRepository: ExposedStorageItemRepository
    private lateinit var userRepository: ExposedUserRepository
    private lateinit var database: Database
    private lateinit var testUserId: String

    @BeforeAll
    fun setupDatabase() {
        postgres.start()
        database = Database.connect(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password,
        )

        transaction(database) {
            SchemaUtils.create(UsersTable, StorageItemsTable, StorageItemMetadataTable)
        }

        storageItemRepository = ExposedStorageItemRepository()
        userRepository = ExposedUserRepository()
    }

    @AfterAll
    fun teardown() {
        postgres.stop()
    }

    @BeforeEach
    fun cleanup() = runTest {
        transaction(database) {
            StorageItemMetadataTable.deleteAll()
            StorageItemsTable.deleteAll()
            UsersTable.deleteAll()
        }

        // Create test user
        testUserId = UUID.randomUUID().toString()
        val now = Clock.System.now()
        val user = User(
            id = testUserId,
            email = "test@example.com",
            username = "testuser",
            passwordHash = "hashed",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            quotaBytes = 10L * 1024 * 1024 * 1024,
            avatarUrl = null,
            preferences = null,
            lastLoginAt = null,
            createdAt = now,
            updatedAt = now,
        )
        userRepository.create(user)
    }

    @Nested
    inner class CreateTests {

        @Test
        fun `should create folder successfully`() = runTest {
            // Given
            val folder = createTestFolder("/Documents")

            // When
            val result = storageItemRepository.create(folder)

            // Then
            assertTrue(result.isRight())
            val created = (result as Either.Right).value
            assertEquals("Documents", created.name)
            assertEquals(ItemType.FOLDER, created.type)
        }

        @Test
        fun `should create file successfully`() = runTest {
            // Given
            val file = createTestFile("/test.txt")

            // When
            val result = storageItemRepository.create(file)

            // Then
            assertTrue(result.isRight())
            val created = (result as Either.Right).value
            assertEquals("test.txt", created.name)
            assertEquals(ItemType.FILE, created.type)
            assertEquals("text/plain", created.mimeType)
        }

        @Test
        fun `should create file in folder`() = runTest {
            // Given
            val folder = createTestFolder("/Documents")
            storageItemRepository.create(folder)

            val file = createTestFile("/Documents/test.txt", parentId = folder.id)

            // When
            val result = storageItemRepository.create(file)

            // Then
            assertTrue(result.isRight())
            val created = (result as Either.Right).value
            assertEquals(folder.id, created.parentId)
        }
    }

    @Nested
    inner class FindTests {

        @Test
        fun `should find item by ID`() = runTest {
            // Given
            val file = createTestFile("/test.txt")
            storageItemRepository.create(file)

            // When
            val result = storageItemRepository.findById(file.id)

            // Then
            assertTrue(result.isRight())
            val found = (result as Either.Right).value
            assertNotNull(found)
            assertEquals(file.id, found.id)
        }

        @Test
        fun `should return null when item not found`() = runTest {
            // When
            val result = storageItemRepository.findById("nonexistent-id")

            // Then
            assertTrue(result.isRight())
            assertEquals(null, (result as Either.Right).value)
        }

        @Test
        fun `should find children of folder`() = runTest {
            // Given
            val folder = createTestFolder("/Documents")
            storageItemRepository.create(folder)

            val file1 = createTestFile("/Documents/file1.txt", parentId = folder.id)
            val file2 = createTestFile("/Documents/file2.txt", parentId = folder.id)
            storageItemRepository.create(file1)
            storageItemRepository.create(file2)

            // When - use query to find children
            val query = StorageItemQuery(
                parentId = folder.id,
                filterByParent = true,
                ownerId = testUserId,
                isTrashed = false,
            )
            val result = storageItemRepository.query(query)

            // Then
            assertTrue(result.isRight())
            val children = (result as Either.Right).value.items
            assertEquals(2, children.size)
        }

        @Test
        fun `should return only root items when querying root with filterByParent`() = runTest {
            // Given: one file at root, one folder with a file
            val rootFile = createTestFile("/root-file.txt", parentId = null)
            val folder = createTestFolder("/Subfolder")
            storageItemRepository.create(rootFile)
            storageItemRepository.create(folder)
            val folderFile = createTestFile("/Subfolder/child.txt", parentId = folder.id)
            storageItemRepository.create(folderFile)

            // When - query root (parentId = null, filterByParent = true)
            val query = StorageItemQuery(
                parentId = null,
                filterByParent = true,
                ownerId = testUserId,
                isTrashed = false,
            )
            val result = storageItemRepository.query(query)

            // Then - only root file and root folder, not folder's child
            assertTrue(result.isRight())
            val items = (result as Either.Right).value.items
            assertEquals(2, items.size)
            assertTrue(items.any { it.name == "root-file.txt" })
            assertTrue(items.any { it.name == "Subfolder" })
            assertFalse(items.any { it.name == "child.txt" })
        }
    }

    @Nested
    inner class UpdateTests {

        @Test
        fun `should update item name`() = runTest {
            // Given
            val file = createTestFile("/test.txt")
            storageItemRepository.create(file)

            val updated = file.copy(
                name = "renamed.txt",
                path = "/renamed.txt",
                updatedAt = Clock.System.now(),
                version = 2,
            )

            // When
            val result = storageItemRepository.update(updated)

            // Then
            assertTrue(result.isRight())

            val found = storageItemRepository.findById(file.id)
            assertEquals("renamed.txt", (found as Either.Right).value?.name)
        }

        @Test
        fun `should update starred status`() = runTest {
            // Given
            val file = createTestFile("/test.txt")
            storageItemRepository.create(file)

            val updated = file.copy(
                isStarred = true,
                updatedAt = Clock.System.now(),
                version = 2,
            )

            // When
            storageItemRepository.update(updated)

            // Then
            val found = storageItemRepository.findById(file.id)
            assertEquals(true, (found as Either.Right).value?.isStarred)
        }

        @Test
        fun `should update trashed status`() = runTest {
            // Given
            val file = createTestFile("/test.txt")
            storageItemRepository.create(file)

            val now = Clock.System.now()
            val updated = file.copy(
                isTrashed = true,
                trashedAt = now,
                updatedAt = now,
                version = 2,
            )

            // When
            storageItemRepository.update(updated)

            // Then
            val found = storageItemRepository.findById(file.id)
            val foundItem = (found as Either.Right).value
            assertEquals(true, foundItem?.isTrashed)
            assertNotNull(foundItem?.trashedAt)
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `should delete item successfully`() = runTest {
            // Given
            val file = createTestFile("/test.txt")
            storageItemRepository.create(file)

            // When
            val result = storageItemRepository.delete(file.id)

            // Then
            assertTrue(result.isRight())

            val found = storageItemRepository.findById(file.id)
            assertEquals(null, (found as Either.Right).value)
        }
    }

    @Nested
    inner class QueryTests {

        @Test
        fun `should query items by parent`() = runTest {
            // Given
            val folder = createTestFolder("/Documents")
            storageItemRepository.create(folder)

            repeat(3) { i ->
                val file = createTestFile("/Documents/file$i.txt", parentId = folder.id)
                storageItemRepository.create(file)
            }

            // When
            val query = StorageItemQuery(
                parentId = folder.id,
                filterByParent = true,
                ownerId = testUserId,
                isTrashed = false,
            )
            val result = storageItemRepository.query(query)

            // Then
            assertTrue(result.isRight())
            val paged = (result as Either.Right).value
            assertEquals(3, paged.items.size)
        }

        @Test
        fun `should query starred items`() = runTest {
            // Given
            val file1 = createTestFile("/file1.txt").copy(isStarred = true)
            val file2 = createTestFile("/file2.txt").copy(isStarred = false)
            val file3 = createTestFile("/file3.txt").copy(isStarred = true)
            storageItemRepository.create(file1)
            storageItemRepository.create(file2)
            storageItemRepository.create(file3)

            // When
            val query = StorageItemQuery(
                ownerId = testUserId,
                isStarred = true,
                isTrashed = false,
            )
            val result = storageItemRepository.query(query)

            // Then
            assertTrue(result.isRight())
            val paged = (result as Either.Right).value
            assertEquals(2, paged.items.size)
        }

        @Test
        fun `should query trashed items`() = runTest {
            // Given
            val now = Clock.System.now()
            val trashedFile = createTestFile("/trashed.txt").copy(isTrashed = true, trashedAt = now)
            val normalFile = createTestFile("/normal.txt")
            storageItemRepository.create(trashedFile)
            storageItemRepository.create(normalFile)

            // When
            val query = StorageItemQuery(
                ownerId = testUserId,
                isTrashed = true,
            )
            val result = storageItemRepository.query(query)

            // Then
            assertTrue(result.isRight())
            val paged = (result as Either.Right).value
            assertEquals(1, paged.items.size)
            assertEquals("trashed.txt", paged.items.first().name)
        }

        @Test
        fun `should query with pagination`() = runTest {
            // Given
            repeat(10) { i ->
                val file = createTestFile("/file$i.txt")
                storageItemRepository.create(file)
            }

            // When
            val query = StorageItemQuery(
                ownerId = testUserId,
                isTrashed = false,
                limit = 5,
                offset = 0,
            )
            val result = storageItemRepository.query(query)

            // Then
            assertTrue(result.isRight())
            val paged = (result as Either.Right).value
            assertEquals(5, paged.items.size)
            assertEquals(10, paged.total)
        }

        @Test
        fun `should query recent items sorted by updatedAt desc`() = runTest {
            // Given: create items (filterByParent false = recent/starred/trash style query)
            val file1 = createTestFile("/recent1.txt")
            val file2 = createTestFile("/recent2.txt")
            val file3 = createTestFile("/recent3.txt")
            storageItemRepository.create(file1)
            storageItemRepository.create(file2)
            storageItemRepository.create(file3)

            // When: query like getRecentItems (no parent filter, sort by updatedAt DESC)
            val query = StorageItemQuery(
                ownerId = testUserId,
                filterByParent = false,
                isTrashed = false,
                sortField = SortField.UPDATED_AT,
                sortOrder = SortOrder.DESC,
                limit = 10,
            )
            val result = storageItemRepository.query(query)

            // Then
            assertTrue(result.isRight())
            val paged = (result as Either.Right).value
            assertTrue(paged.items.size in 1..3)
            assertEquals(3, paged.total)
        }
    }

    @Nested
    inner class PathTests {

        @Test
        fun `should check if path exists`() = runTest {
            // Given
            val file = createTestFile("/test.txt")
            storageItemRepository.create(file)

            // When
            val result = storageItemRepository.existsByPath("/test.txt", testUserId)

            // Then
            assertTrue(result.isRight())
            assertEquals(true, (result as Either.Right).value)
        }

        @Test
        fun `should return false for non-existing path`() = runTest {
            // When
            val result = storageItemRepository.existsByPath("/nonexistent.txt", testUserId)

            // Then
            assertTrue(result.isRight())
            assertEquals(false, (result as Either.Right).value)
        }
    }

    // Helper functions

    private fun createTestFolder(path: String, parentId: String? = null): StorageItem {
        val now = Clock.System.now()
        val name = path.substringAfterLast("/")
        return StorageItem(
            id = UUID.randomUUID().toString(),
            name = name,
            path = path,
            type = ItemType.FOLDER,
            parentId = parentId,
            ownerId = testUserId,
            size = 0,
            mimeType = null,
            checksum = null,
            storageKey = null,
            visibility = Visibility.PRIVATE,
            isTrashed = false,
            isStarred = false,
            createdAt = now,
            updatedAt = now,
            trashedAt = null,
            version = 1,
        )
    }

    private fun createTestFile(path: String, parentId: String? = null): StorageItem {
        val now = Clock.System.now()
        val name = path.substringAfterLast("/")
        return StorageItem(
            id = UUID.randomUUID().toString(),
            name = name,
            path = path,
            type = ItemType.FILE,
            parentId = parentId,
            ownerId = testUserId,
            size = 1024,
            mimeType = "text/plain",
            checksum = "abc123",
            storageKey = "storage-${UUID.randomUUID()}",
            visibility = Visibility.PRIVATE,
            isTrashed = false,
            isStarred = false,
            createdAt = now,
            updatedAt = now,
            trashedAt = null,
            version = 1,
        )
    }
}
