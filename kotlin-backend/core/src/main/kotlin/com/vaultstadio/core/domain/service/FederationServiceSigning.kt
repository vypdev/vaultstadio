/**
 * Federation message signing and verification helpers.
 * Extracted from FederationService to keep the main file under the line limit.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.SignedFederationMessage
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.core.exception.StorageException
import kotlinx.datetime.Clock

/**
 * Get the public key for this instance.
 */
internal fun getFederationPublicKey(cryptoService: FederationCryptoService): String? =
    cryptoService.getPublicKeyBase64()

/**
 * Sign a message for federation communication.
 */
internal fun signFederationMessage(
    cryptoService: FederationCryptoService,
    instanceConfig: InstanceConfig,
    payload: String,
): SignedFederationMessage {
    val nonce = java.util.UUID.randomUUID().toString()
    val timestamp = Clock.System.now()

    val signedPayload = cryptoService.signFederationMessage(
        payload = payload,
        nonce = nonce,
        timestamp = timestamp.epochSeconds,
    )

    return SignedFederationMessage(
        payload = payload,
        signature = signedPayload?.signature ?: "",
        timestamp = timestamp,
        nonce = nonce,
        senderDomain = instanceConfig.domain,
        algorithm = signedPayload?.algorithm,
    )
}

/**
 * Verify a signed message from another instance.
 */
internal suspend fun verifyFederationMessage(
    federationRepository: FederationRepository,
    cryptoService: FederationCryptoService,
    message: SignedFederationMessage,
): Either<StorageException, Boolean> {
    val instanceResult = federationRepository.findInstanceByDomain(message.senderDomain)
    return when (instanceResult) {
        is Either.Left -> instanceResult
        is Either.Right -> {
            val instance = instanceResult.value
            val valid = when {
                instance == null || instance.publicKey.isBlank() -> false
                else -> {
                    val result = cryptoService.verifyFederationMessage(
                        payload = message.payload,
                        signature = message.signature,
                        nonce = message.nonce,
                        timestamp = message.timestamp.epochSeconds,
                        publicKeyBase64 = instance.publicKey,
                        signatureAlgorithm = message.algorithm ?: FederationCryptoService.ALGORITHM_ED25519,
                    )
                    result is SignatureVerificationResult.Valid
                }
            }
            valid.right()
        }
    }
}
