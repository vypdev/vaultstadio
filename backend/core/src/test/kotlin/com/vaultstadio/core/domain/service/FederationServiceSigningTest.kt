/**
 * VaultStadio Federation Signing Helpers Tests
 *
 * Unit tests for internal signing helpers: getFederationPublicKey, signFederationMessage,
 * verifyFederationMessage.
 */

package com.vaultstadio.core.domain.service

import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.InstanceStatus
import com.vaultstadio.core.domain.model.SignedFederationMessage
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.core.exception.DatabaseException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FederationServiceSigningTest {

    @Test
    fun `getFederationPublicKey returns crypto public key`() {
        val crypto = FederationCryptoService()
        val key = getFederationPublicKey(crypto)
        assertNotNull(key)
        assertTrue(key!!.isNotEmpty())
    }

    @Test
    fun `signFederationMessage returns message with payload and sender domain`() {
        val crypto = FederationCryptoService()
        val config = InstanceConfig(
            domain = "local.example.com",
            name = "Local",
            version = "1.0",
            publicKey = crypto.getPublicKeyBase64() ?: "",
            privateKey = crypto.getPrivateKeyBase64() ?: "",
            capabilities = emptyList(),
        )
        val message = signFederationMessage(crypto, config, "payload")
        assertEquals("payload", message.payload)
        assertEquals("local.example.com", message.senderDomain)
        assertTrue(message.nonce.isNotEmpty())
        assertTrue(message.timestamp.epochSeconds > 0)
    }

    @Test
    fun `verifyFederationMessage returns Left when repository returns Left`() = runTest {
        val repo = mockk<FederationRepository>()
        val crypto = FederationCryptoService()
        coEvery { repo.findInstanceByDomain(any()) } returns DatabaseException("error").left()
        val message = SignedFederationMessage(
            payload = "p",
            signature = "s",
            timestamp = Clock.System.now(),
            nonce = "n",
            senderDomain = "remote.com",
        )
        val result = verifyFederationMessage(repo, crypto, message)
        assertTrue(result.isLeft())
    }

    @Test
    fun `verifyFederationMessage returns Right false when instance is null`() = runTest {
        val repo = mockk<FederationRepository>()
        val crypto = FederationCryptoService()
        coEvery { repo.findInstanceByDomain(any()) } returns null.right()
        val message = SignedFederationMessage(
            payload = "p",
            signature = "s",
            timestamp = Clock.System.now(),
            nonce = "n",
            senderDomain = "remote.com",
        )
        val result = verifyFederationMessage(repo, crypto, message)
        assertTrue(result.isRight())
        result.onRight { valid -> assertFalse(valid) }
    }

    @Test
    fun `verifyFederationMessage returns Right true when signature valid`() = runTest {
        val signerCrypto = FederationCryptoService()
        val publicKey = signerCrypto.getPublicKeyBase64()!!
        val now = Clock.System.now()
        val payload = "hello"
        val nonce = "n1"
        val signed = signerCrypto.signFederationMessage(payload, nonce, now.epochSeconds)!!
        val message = SignedFederationMessage(
            payload = payload,
            signature = signed.signature,
            timestamp = now,
            nonce = nonce,
            senderDomain = "remote.com",
            algorithm = signed.algorithm,
        )
        val repo = mockk<FederationRepository>()
        val instance = FederatedInstance(
            id = "i1",
            domain = "remote.com",
            name = "R",
            version = "1.0",
            publicKey = publicKey,
            status = InstanceStatus.ONLINE,
            registeredAt = Clock.System.now(),
        )
        coEvery { repo.findInstanceByDomain("remote.com") } returns instance.right()
        val verifierCrypto = FederationCryptoService()
        val result = verifyFederationMessage(repo, verifierCrypto, message)
        assertTrue(result.isRight())
        result.onRight { valid -> assertTrue(valid) }
    }
}
