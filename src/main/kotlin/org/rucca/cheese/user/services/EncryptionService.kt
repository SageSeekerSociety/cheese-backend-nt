package org.rucca.cheese.user.services

import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.models.EncryptionKey
import org.rucca.cheese.user.models.EncryptionKeyRepository
import org.rucca.cheese.user.models.KeyPurpose
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EncryptionService(
    private val encryptionKeyRepository: EncryptionKeyRepository,
    @Value("\${cheese.encryption.master-key}") private val masterKey: String,
) {
    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)
    private val algorithm = "AES/ECB/PKCS5Padding"
    private val maxRetries = 3

    /**
     * Generate a new encryption key for a specific purpose and entity Uses persisted key or creates
     * a new one if it doesn't exist
     */
    @Transactional
    fun generateKey(purpose: KeyPurpose, entityId: IdType?): EncryptionKey {
        // First check if a key already exists for this purpose and entity
        if (entityId != null) {
            val existingKey =
                encryptionKeyRepository.findByPurposeAndRelatedEntityId(purpose, entityId)
            if (existingKey != null) {
                return existingKey
            }
        }

        // Create a completely new key with no version conflicts
        try {
            val randomKey = UUID.randomUUID().toString()
            // Encrypt the random key with the master key
            val encryptedKey = encrypt(randomKey, masterKey)

            // Use a fixed ID derived from purpose and entityId to avoid duplicate keys
            val keyId =
                if (entityId != null) {
                    // Create a deterministic ID based on purpose and entityId
                    UUID.nameUUIDFromBytes("${purpose}_${entityId}".toByteArray()).toString()
                } else {
                    // For keys not tied to an entity, use a random UUID
                    UUID.randomUUID().toString()
                }

            val key =
                EncryptionKey(
                    id = keyId,
                    keyValue = encryptedKey,
                    purpose = purpose,
                    relatedEntityId = entityId,
                )

            return try {
                encryptionKeyRepository.save(key)
            } catch (e: Exception) {
                // If save fails, there's a chance another thread created the key
                // Try to find it one more time
                if (entityId != null) {
                    val lastAttemptToFind =
                        encryptionKeyRepository.findByPurposeAndRelatedEntityId(purpose, entityId)
                    if (lastAttemptToFind != null) {
                        return lastAttemptToFind
                    }
                }
                // If we still didn't find it, throw the original exception
                throw e
            }
        } catch (e: Exception) {
            logger.error("Failed to generate encryption key: ${e.message}")
            throw RuntimeException("Failed to generate encryption key", e)
        }
    }

    /**
     * Retrieve the encryption key for a specific purpose and entity Includes retry logic for
     * optimistic locking failures
     */
    @Transactional(readOnly = true)
    fun getKey(keyId: String): EncryptionKey {
        var attempts = 0
        var lastException: Exception? = null

        while (attempts < maxRetries) {
            try {
                return encryptionKeyRepository.findById(keyId).orElseThrow {
                    IllegalArgumentException("Encryption key not found: $keyId")
                }
            } catch (e: OptimisticLockingFailureException) {
                lastException = e
                attempts++
                if (attempts < maxRetries) {
                    logger.warn(
                        "Optimistic locking failure when retrieving key, retrying (attempt ${attempts})"
                    )
                    Thread.sleep(100) // Short delay before retrying
                }
            }
        }

        // If we get here, we've exhausted our retries
        logger.error("Failed to retrieve encryption key after $maxRetries attempts")
        throw lastException ?: RuntimeException("Failed to retrieve encryption key")
    }

    /**
     * Get or create encryption key for a specific purpose and entity Uses a more reliable approach
     * to avoid optimistic locking issues
     */
    @Transactional
    fun getOrCreateKey(purpose: KeyPurpose, entityId: IdType): EncryptionKey {
        try {
            // First try to find by purpose and entity ID
            val existingKey =
                encryptionKeyRepository.findByPurposeAndRelatedEntityId(purpose, entityId)
            if (existingKey != null) {
                return existingKey
            }

            // Create a new key if it doesn't exist
            return generateKey(purpose, entityId)
        } catch (e: Exception) {
            logger.error("Error getting or creating encryption key: ${e.message}")
            throw RuntimeException("Failed to get or create encryption key", e)
        }
    }

    /** Encrypt data using the provided encryption key */
    fun encryptData(data: String, keyId: String): String {
        val encryptionKey = getKey(keyId)
        // Decrypt the key first using master key
        val decryptedKey = decrypt(encryptionKey.keyValue, masterKey)
        // Then use the decrypted key to encrypt the data
        return encrypt(data, decryptedKey)
    }

    /** Decrypt data using the provided encryption key */
    fun decryptData(encryptedData: String, keyId: String): String {
        val encryptionKey = getKey(keyId)
        // Decrypt the key first using master key
        val decryptedKey = decrypt(encryptionKey.keyValue, masterKey)
        // Then use the decrypted key to decrypt the data
        return decrypt(encryptedData, decryptedKey)
    }

    /** Encrypt a string using AES encryption */
    private fun encrypt(data: String, key: String): String {
        val secretKey = SecretKeySpec(key.take(16).padEnd(16, '0').toByteArray(), "AES")
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    /** Decrypt a string using AES decryption */
    private fun decrypt(encryptedData: String, key: String): String {
        val secretKey = SecretKeySpec(key.take(16).padEnd(16, '0').toByteArray(), "AES")
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decodedBytes = Base64.getDecoder().decode(encryptedData)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes)
    }
}
