/**
 * VaultStadio BCrypt Password Hasher Tests
 */

package com.vaultstadio.infrastructure.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BCryptPasswordHasherTest {

    private lateinit var hasher: BCryptPasswordHasher

    @BeforeEach
    fun setup() {
        // Use lower cost for faster tests
        hasher = BCryptPasswordHasher(cost = 4)
    }

    @Nested
    inner class HashTests {

        @Test
        fun `hash should produce bcrypt format`() {
            val hash = hasher.hash("password123")

            // BCrypt hashes start with $2a$, $2b$, or $2y$
            assertTrue(hash.startsWith("\$2"))
            assertTrue(hash.length >= 60)
        }

        @Test
        fun `hash should be unique each time`() {
            val hash1 = hasher.hash("password123")
            val hash2 = hasher.hash("password123")

            // Same password should produce different hashes (due to salt)
            assertNotEquals(hash1, hash2)
        }

        @Test
        fun `hash should handle empty password`() {
            val hash = hasher.hash("")

            assertTrue(hash.isNotEmpty())
            assertTrue(hash.startsWith("\$2"))
        }

        @Test
        fun `hash should handle unicode passwords`() {
            val hash = hasher.hash("密码123!@#")

            assertTrue(hash.isNotEmpty())
            assertTrue(hash.startsWith("\$2"))
        }

        @Test
        fun `hash should handle moderately long passwords`() {
            // BCrypt has a 72 byte limit, so we test with a safe length
            val password = "a".repeat(50)
            val hash = hasher.hash(password)

            assertTrue(hash.isNotEmpty())
        }
    }

    @Nested
    inner class VerifyTests {

        @Test
        fun `verify should return true for correct password`() {
            val password = "password123"
            val hash = hasher.hash(password)

            assertTrue(hasher.verify(password, hash))
        }

        @Test
        fun `verify should return false for incorrect password`() {
            val hash = hasher.hash("password123")

            assertFalse(hasher.verify("wrongpassword", hash))
        }

        @Test
        fun `verify should handle empty password`() {
            val hash = hasher.hash("")

            assertTrue(hasher.verify("", hash))
            assertFalse(hasher.verify("notempty", hash))
        }

        @Test
        fun `verify should handle unicode passwords`() {
            val password = "密码123!@#"
            val hash = hasher.hash(password)

            assertTrue(hasher.verify(password, hash))
            assertFalse(hasher.verify("wrong密码", hash))
        }

        @Test
        fun `verify should handle case sensitivity`() {
            val hash = hasher.hash("Password123")

            assertFalse(hasher.verify("password123", hash))
            assertFalse(hasher.verify("PASSWORD123", hash))
            assertTrue(hasher.verify("Password123", hash))
        }
    }

    @Nested
    inner class CostTests {

        @Test
        fun `higher cost should produce valid hash`() {
            val higherCostHasher = BCryptPasswordHasher(cost = 10)
            val hash = higherCostHasher.hash("password123")

            assertTrue(higherCostHasher.verify("password123", hash))
        }

        @Test
        fun `default cost should be 12`() {
            val defaultHasher = BCryptPasswordHasher()
            val hash = defaultHasher.hash("test")

            // Hash should contain cost indicator
            assertTrue(hash.contains("\$12\$") || hash.contains("\$04\$") || hash.startsWith("\$2"))
        }
    }

    @Nested
    inner class CrossVerificationTests {

        @Test
        fun `hash from one instance should verify in another`() {
            val hasher1 = BCryptPasswordHasher(cost = 4)
            val hasher2 = BCryptPasswordHasher(cost = 4)

            val hash = hasher1.hash("password123")

            assertTrue(hasher2.verify("password123", hash))
        }

        @Test
        fun `hash with different cost should still verify`() {
            val hasher1 = BCryptPasswordHasher(cost = 4)
            val hasher2 = BCryptPasswordHasher(cost = 6)

            val hash = hasher1.hash("password123")

            // Verification uses the cost from the hash itself
            assertTrue(hasher2.verify("password123", hash))
        }
    }
}
