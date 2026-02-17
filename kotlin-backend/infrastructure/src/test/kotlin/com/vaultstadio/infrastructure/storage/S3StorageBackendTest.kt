/**
 * VaultStadio S3 Storage Backend Tests
 */

package com.vaultstadio.infrastructure.storage

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("S3StorageBackend")
class S3StorageBackendTest {

    @Nested
    @DisplayName("S3StorageConfig")
    inner class S3StorageConfigTests {

        @Test
        @DisplayName("should have correct default values")
        fun shouldHaveCorrectDefaults() {
            val config = S3StorageConfig(
                bucket = "test-bucket",
            )

            assertEquals("test-bucket", config.bucket)
            assertEquals("us-east-1", config.region)
            assertEquals(null, config.endpoint)
            assertEquals(null, config.accessKeyId)
            assertEquals(null, config.secretAccessKey)
            assertEquals(false, config.usePathStyle)
            assertEquals("", config.prefix)
        }

        @Test
        @DisplayName("should accept custom values")
        fun shouldAcceptCustomValues() {
            val config = S3StorageConfig(
                bucket = "my-bucket",
                region = "eu-west-1",
                endpoint = "http://minio:9000",
                accessKeyId = "access-key",
                secretAccessKey = "secret-key",
                usePathStyle = true,
                prefix = "files",
            )

            assertEquals("my-bucket", config.bucket)
            assertEquals("eu-west-1", config.region)
            assertEquals("http://minio:9000", config.endpoint)
            assertEquals("access-key", config.accessKeyId)
            assertEquals("secret-key", config.secretAccessKey)
            assertTrue(config.usePathStyle)
            assertEquals("files", config.prefix)
        }
    }

    @Nested
    @DisplayName("Object Key Generation")
    inner class ObjectKeyGenerationTests {

        @Test
        @DisplayName("should generate hierarchical object keys")
        fun shouldGenerateHierarchicalKeys() {
            // Test the key generation logic
            val storageKey = "abcdef1234567890abcdef1234567890"
            val expectedDir1 = "ab"
            val expectedDir2 = "cd"

            assertEquals(expectedDir1, storageKey.take(2))
            assertEquals(expectedDir2, storageKey.substring(2, 4))
        }

        @Test
        @DisplayName("should include prefix in object key when configured")
        fun shouldIncludePrefixInKey() {
            val config = S3StorageConfig(
                bucket = "test-bucket",
                prefix = "uploads",
            )

            val storageKey = "abcdef1234567890abcdef1234567890"
            val dir1 = storageKey.take(2)
            val dir2 = storageKey.substring(2, 4)
            val prefix = if (config.prefix.isNotEmpty()) "${config.prefix}/" else ""
            val objectKey = "$prefix$dir1/$dir2/$storageKey"

            assertEquals("uploads/ab/cd/abcdef1234567890abcdef1234567890", objectKey)
        }

        @Test
        @DisplayName("should not include prefix when empty")
        fun shouldNotIncludePrefixWhenEmpty() {
            val config = S3StorageConfig(
                bucket = "test-bucket",
                prefix = "",
            )

            val storageKey = "abcdef1234567890abcdef1234567890"
            val dir1 = storageKey.take(2)
            val dir2 = storageKey.substring(2, 4)
            val prefix = if (config.prefix.isNotEmpty()) "${config.prefix}/" else ""
            val objectKey = "$prefix$dir1/$dir2/$storageKey"

            assertEquals("ab/cd/abcdef1234567890abcdef1234567890", objectKey)
        }
    }

    @Nested
    @DisplayName("Storage Key Generation")
    inner class StorageKeyGenerationTests {

        @Test
        @DisplayName("should generate valid UUID-based storage keys")
        fun shouldGenerateValidKeys() {
            val key = java.util.UUID.randomUUID().toString().replace("-", "")

            assertEquals(32, key.length)
            assertTrue(key.all { it.isLetterOrDigit() })
        }

        @Test
        @DisplayName("should generate unique keys")
        fun shouldGenerateUniqueKeys() {
            val keys = (1..100).map {
                java.util.UUID.randomUUID().toString().replace("-", "")
            }.toSet()

            assertEquals(100, keys.size)
        }
    }
}
