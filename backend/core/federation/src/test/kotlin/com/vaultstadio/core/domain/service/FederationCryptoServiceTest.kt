/**
 * Tests for FederationCryptoService
 */

package com.vaultstadio.core.domain.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FederationCryptoServiceTest {

    private lateinit var cryptoService: FederationCryptoService

    @BeforeEach
    fun setup() {
        // Create service with auto-generated keys
        cryptoService = FederationCryptoService()
    }

    @Nested
    inner class KeyGenerationTests {

        @Test
        fun `generates key pair on initialization`() {
            val publicKey = cryptoService.getPublicKeyBase64()
            val privateKey = cryptoService.getPrivateKeyBase64()

            assertNotNull(publicKey, "Public key should be generated")
            assertNotNull(privateKey, "Private key should be generated")
            assertTrue(publicKey.isNotEmpty(), "Public key should not be empty")
            assertTrue(privateKey.isNotEmpty(), "Private key should not be empty")
        }

        @Test
        fun `can generate new key pair`() {
            val keyPair = cryptoService.generateKeyPair()

            assertNotNull(keyPair.public, "Public key should be generated")
            assertNotNull(keyPair.private, "Private key should be generated")
        }

        @Test
        fun `keys are Base64 encoded`() {
            val publicKey = cryptoService.getPublicKeyBase64()!!

            // Should be valid Base64 (no exception thrown)
            val decoded = java.util.Base64.getDecoder().decode(publicKey)
            assertTrue(decoded.isNotEmpty(), "Decoded key should not be empty")
        }
    }

    @Nested
    inner class SigningTests {

        @Test
        fun `signs payload successfully`() {
            val payload = "This is a test message to sign"

            val signedPayload = cryptoService.sign(payload)

            assertNotNull(signedPayload, "Signed payload should not be null")
            assertEquals(payload, signedPayload.payload)
            assertTrue(signedPayload.signature.isNotEmpty(), "Signature should not be empty")
            assertEquals("Ed25519", signedPayload.algorithm)
        }

        @Test
        fun `different payloads produce different signatures`() {
            val signed1 = cryptoService.sign("Message 1")
            val signed2 = cryptoService.sign("Message 2")

            assertNotNull(signed1)
            assertNotNull(signed2)
            assertTrue(signed1.signature != signed2.signature, "Different messages should have different signatures")
        }

        @Test
        fun `same payload produces different signatures with nonce`() {
            val signed1 = cryptoService.signFederationMessage("Same message", "nonce1")
            val signed2 = cryptoService.signFederationMessage("Same message", "nonce2")

            assertNotNull(signed1)
            assertNotNull(signed2)
            assertTrue(signed1.signature != signed2.signature, "Different nonces should produce different signatures")
        }
    }

    @Nested
    inner class VerificationTests {

        @Test
        fun `verifies valid signature`() {
            val payload = "This is a test message"
            val signedPayload = cryptoService.sign(payload)!!
            val publicKey = cryptoService.getPublicKeyBase64()!!

            val result = cryptoService.verify(signedPayload, publicKey)

            assertTrue(result is SignatureVerificationResult.Valid, "Valid signature should verify")
        }

        @Test
        fun `rejects tampered payload`() {
            val payload = "Original message"
            val signedPayload = cryptoService.sign(payload)!!
            val publicKey = cryptoService.getPublicKeyBase64()!!

            // Tamper with payload
            val tamperedPayload = signedPayload.copy(payload = "Tampered message")

            val result = cryptoService.verify(tamperedPayload, publicKey)

            assertTrue(result is SignatureVerificationResult.Invalid, "Tampered payload should fail verification")
        }

        @Test
        fun `rejects invalid signature`() {
            val payload = "Test message"
            val publicKey = cryptoService.getPublicKeyBase64()!!

            // Create a fake signature
            val fakeSignedPayload = SignedPayload(
                payload = payload,
                signature = java.util.Base64.getEncoder().encodeToString("fake".toByteArray()),
                algorithm = "Ed25519",
            )

            val result = cryptoService.verify(fakeSignedPayload, publicKey)

            assertTrue(
                result is SignatureVerificationResult.Invalid || result is SignatureVerificationResult.Error,
                "Invalid signature should fail verification",
            )
        }

        @Test
        fun `rejects wrong public key`() {
            val payload = "Test message"
            val signedPayload = cryptoService.sign(payload)!!

            // Generate a different key pair
            val otherService = FederationCryptoService()
            val wrongPublicKey = otherService.getPublicKeyBase64()!!

            val result = cryptoService.verify(signedPayload, wrongPublicKey)

            assertTrue(result is SignatureVerificationResult.Invalid, "Wrong public key should fail verification")
        }
    }

    @Nested
    inner class FederationMessageTests {

        @Test
        fun `signs and verifies federation message`() {
            val payload = "Federation payload"
            val nonce = "unique-nonce-123"
            val timestamp = System.currentTimeMillis() / 1000
            val publicKey = cryptoService.getPublicKeyBase64()!!

            val signedPayload = cryptoService.signFederationMessage(payload, nonce, timestamp)!!

            val result = cryptoService.verifyFederationMessage(
                payload = payload,
                signature = signedPayload.signature,
                nonce = nonce,
                timestamp = timestamp,
                publicKeyBase64 = publicKey,
            )

            assertTrue(result is SignatureVerificationResult.Valid, "Valid federation message should verify")
        }

        @Test
        fun `rejects expired message`() {
            val payload = "Expired message"
            val nonce = "nonce"
            val oldTimestamp = (System.currentTimeMillis() / 1000) - 600 // 10 minutes ago
            val publicKey = cryptoService.getPublicKeyBase64()!!

            val signedPayload = cryptoService.signFederationMessage(payload, nonce, oldTimestamp)!!

            val result = cryptoService.verifyFederationMessage(
                payload = payload,
                signature = signedPayload.signature,
                nonce = nonce,
                timestamp = oldTimestamp,
                publicKeyBase64 = publicKey,
                maxAgeSeconds = 300, // 5 minutes
            )

            assertTrue(result is SignatureVerificationResult.Invalid, "Expired message should fail verification")
        }

        @Test
        fun `rejects future message`() {
            val payload = "Future message"
            val nonce = "nonce"
            val futureTimestamp = (System.currentTimeMillis() / 1000) + 120 // 2 minutes in future
            val publicKey = cryptoService.getPublicKeyBase64()!!

            val signedPayload = cryptoService.signFederationMessage(payload, nonce, futureTimestamp)!!

            val result = cryptoService.verifyFederationMessage(
                payload = payload,
                signature = signedPayload.signature,
                nonce = nonce,
                timestamp = futureTimestamp,
                publicKeyBase64 = publicKey,
            )

            assertTrue(result is SignatureVerificationResult.Invalid, "Future message should fail verification")
        }
    }

    @Nested
    inner class LoadExistingKeysTests {

        @Test
        fun `can load existing keys`() {
            // Generate keys
            val originalService = FederationCryptoService()
            val publicKey = originalService.getPublicKeyBase64()!!
            val privateKey = originalService.getPrivateKeyBase64()!!

            // Create new service with same keys
            val loadedService = FederationCryptoService(
                privateKeyBase64 = privateKey,
                publicKeyBase64 = publicKey,
            )

            // Sign with original, verify with loaded
            val payload = "Test message"
            val signed = originalService.sign(payload)!!

            val result = loadedService.verify(signed, publicKey)

            assertTrue(result is SignatureVerificationResult.Valid, "Loaded keys should work correctly")
        }

        @Test
        fun `loaded service can sign and original can verify`() {
            // Generate keys
            val originalService = FederationCryptoService()
            val publicKey = originalService.getPublicKeyBase64()!!
            val privateKey = originalService.getPrivateKeyBase64()!!

            // Create new service with same keys
            val loadedService = FederationCryptoService(
                privateKeyBase64 = privateKey,
                publicKeyBase64 = publicKey,
            )

            // Sign with loaded, verify with original
            val payload = "Test message"
            val signed = loadedService.sign(payload)!!

            val result = originalService.verify(signed, publicKey)

            assertTrue(result is SignatureVerificationResult.Valid, "Cross-verification should work")
        }
    }
}
