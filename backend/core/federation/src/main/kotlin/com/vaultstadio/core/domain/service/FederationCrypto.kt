/**
 * VaultStadio Federation Cryptography
 *
 * Provides cryptographic signing and verification for federation messages.
 * Uses Ed25519 for digital signatures.
 */

package com.vaultstadio.core.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

private val logger = KotlinLogging.logger {}

/**
 * Result of signature verification.
 */
sealed class SignatureVerificationResult {
    data object Valid : SignatureVerificationResult()
    data class Invalid(val reason: String) : SignatureVerificationResult()
    data class Error(val message: String, val exception: Throwable? = null) : SignatureVerificationResult()
}

/**
 * Signed message with metadata.
 */
data class SignedPayload(
    val payload: String,
    val signature: String,
    val algorithm: String,
    val keyId: String? = null,
)

/**
 * Service for cryptographic operations in federation.
 *
 * Supports:
 * - Ed25519 (preferred, fast and secure)
 * - RSA-SHA256 (fallback for compatibility)
 */
class FederationCryptoService(
    private val privateKeyBase64: String? = null,
    private val publicKeyBase64: String? = null,
    private val algorithm: String = ALGORITHM_ED25519,
) {
    companion object {
        const val ALGORITHM_ED25519 = "Ed25519"
        const val ALGORITHM_RSA_SHA256 = "SHA256withRSA"

        // Key sizes
        const val RSA_KEY_SIZE = 2048
    }

    private var keyPair: KeyPair? = null
    private var signingKey: PrivateKey? = null
    private var verifyingKey: PublicKey? = null

    init {
        initializeKeys()
    }

    private fun initializeKeys() {
        try {
            if (privateKeyBase64 != null && publicKeyBase64 != null) {
                // Load existing keys
                val keyFactory = getKeyFactory()

                val privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64)
                val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)

                signingKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
                verifyingKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes))

                logger.info { "Loaded existing $algorithm keys for federation" }
            } else {
                // Generate new keys
                keyPair = generateKeyPair()
                signingKey = keyPair?.private
                verifyingKey = keyPair?.public

                logger.info { "Generated new $algorithm keys for federation" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize federation crypto keys" }
        }
    }

    private fun getKeyFactory(): KeyFactory {
        return when (algorithm) {
            ALGORITHM_ED25519 -> KeyFactory.getInstance("Ed25519")
            ALGORITHM_RSA_SHA256 -> KeyFactory.getInstance("RSA")
            else -> throw IllegalArgumentException("Unsupported algorithm: $algorithm")
        }
    }

    /**
     * Generate a new key pair.
     */
    fun generateKeyPair(): KeyPair {
        val generator = when (algorithm) {
            ALGORITHM_ED25519 -> KeyPairGenerator.getInstance("Ed25519")
            ALGORITHM_RSA_SHA256 -> KeyPairGenerator.getInstance("RSA").apply {
                initialize(RSA_KEY_SIZE, SecureRandom())
            }
            else -> throw IllegalArgumentException("Unsupported algorithm: $algorithm")
        }
        return generator.generateKeyPair()
    }

    /**
     * Get the public key as Base64.
     */
    fun getPublicKeyBase64(): String? {
        return verifyingKey?.let {
            Base64.getEncoder().encodeToString(it.encoded)
        }
    }

    /**
     * Get the private key as Base64 (for storage - keep secure!).
     */
    fun getPrivateKeyBase64(): String? {
        return signingKey?.let {
            Base64.getEncoder().encodeToString(it.encoded)
        }
    }

    /**
     * Sign a payload.
     *
     * @param payload The data to sign
     * @return Signed payload with signature
     */
    fun sign(payload: String): SignedPayload? {
        val key = signingKey ?: return null

        return try {
            val signatureAlgorithm = when (algorithm) {
                ALGORITHM_ED25519 -> "Ed25519"
                ALGORITHM_RSA_SHA256 -> "SHA256withRSA"
                else -> throw IllegalArgumentException("Unsupported algorithm: $algorithm")
            }

            val signer = Signature.getInstance(signatureAlgorithm)
            signer.initSign(key)
            signer.update(payload.toByteArray(Charsets.UTF_8))

            val signatureBytes = signer.sign()
            val signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes)

            SignedPayload(
                payload = payload,
                signature = signatureBase64,
                algorithm = algorithm,
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to sign payload" }
            null
        }
    }

    /**
     * Verify a signature.
     *
     * @param payload The original payload
     * @param signatureBase64 The signature to verify (Base64 encoded)
     * @param publicKeyBase64 The public key to use (Base64 encoded)
     * @param signatureAlgorithm The algorithm used for signing
     * @return Verification result
     */
    fun verify(
        payload: String,
        signatureBase64: String,
        publicKeyBase64: String,
        signatureAlgorithm: String = algorithm,
    ): SignatureVerificationResult {
        return try {
            val keyFactory = when (signatureAlgorithm) {
                ALGORITHM_ED25519 -> KeyFactory.getInstance("Ed25519")
                ALGORITHM_RSA_SHA256 -> KeyFactory.getInstance("RSA")
                else -> return SignatureVerificationResult.Invalid("Unsupported algorithm: $signatureAlgorithm")
            }

            val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes))

            val signatureBytes = Base64.getDecoder().decode(signatureBase64)

            val sigAlgorithm = when (signatureAlgorithm) {
                ALGORITHM_ED25519 -> "Ed25519"
                ALGORITHM_RSA_SHA256 -> "SHA256withRSA"
                else -> return SignatureVerificationResult.Invalid("Unsupported algorithm: $signatureAlgorithm")
            }

            val verifier = Signature.getInstance(sigAlgorithm)
            verifier.initVerify(publicKey)
            verifier.update(payload.toByteArray(Charsets.UTF_8))

            if (verifier.verify(signatureBytes)) {
                SignatureVerificationResult.Valid
            } else {
                SignatureVerificationResult.Invalid("Signature verification failed")
            }
        } catch (e: IllegalArgumentException) {
            SignatureVerificationResult.Invalid("Invalid Base64 encoding: ${e.message}")
        } catch (e: Exception) {
            SignatureVerificationResult.Error("Verification error: ${e.message}", e)
        }
    }

    /**
     * Verify a signed payload.
     *
     * @param signedPayload The signed payload to verify
     * @param publicKeyBase64 The public key to use
     * @return Verification result
     */
    fun verify(signedPayload: SignedPayload, publicKeyBase64: String): SignatureVerificationResult {
        return verify(
            payload = signedPayload.payload,
            signatureBase64 = signedPayload.signature,
            publicKeyBase64 = publicKeyBase64,
            signatureAlgorithm = signedPayload.algorithm,
        )
    }

    /**
     * Create a timestamped signed message for federation.
     *
     * @param payload The message payload
     * @param nonce Unique nonce to prevent replay attacks
     * @param timestamp Message timestamp (epoch seconds)
     * @return Signed payload
     */
    fun signFederationMessage(
        payload: String,
        nonce: String,
        timestamp: Long = System.currentTimeMillis() / 1000,
    ): SignedPayload? {
        // Create canonical message format
        val canonicalMessage = "$timestamp:$nonce:$payload"
        return sign(canonicalMessage)
    }

    /**
     * Verify a federation message with timestamp and nonce validation.
     *
     * @param payload Original payload
     * @param signature Signature to verify
     * @param nonce Message nonce
     * @param timestamp Message timestamp
     * @param publicKeyBase64 Sender's public key
     * @param maxAgeSeconds Maximum age of message (default 5 minutes)
     * @return Verification result
     */
    fun verifyFederationMessage(
        payload: String,
        signature: String,
        nonce: String,
        timestamp: Long,
        publicKeyBase64: String,
        signatureAlgorithm: String = algorithm,
        maxAgeSeconds: Long = 300,
    ): SignatureVerificationResult {
        // Check timestamp
        val currentTime = System.currentTimeMillis() / 1000
        if (timestamp < currentTime - maxAgeSeconds) {
            return SignatureVerificationResult.Invalid("Message too old: timestamp $timestamp")
        }
        if (timestamp > currentTime + 60) { // Allow 1 minute clock skew
            return SignatureVerificationResult.Invalid("Message from future: timestamp $timestamp")
        }

        // Verify signature
        val canonicalMessage = "$timestamp:$nonce:$payload"
        return verify(canonicalMessage, signature, publicKeyBase64, signatureAlgorithm)
    }
}
