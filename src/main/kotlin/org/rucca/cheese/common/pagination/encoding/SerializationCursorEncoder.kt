package org.rucca.cheese.common.pagination.encoding

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.rucca.cheese.common.pagination.model.CursorValue
import org.rucca.cheese.common.pagination.model.toCursorValue
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** Base64 cursor encoder using kotlinx.serialization for JSON handling. */
class SerializationCursorEncoder : CursorEncoder {
    // JSON serializer instance with lenient settings
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // Container for serialized cursor data
    @Serializable private data class CursorData(val values: Map<String, JsonElement>)

    override fun encode(values: Map<String, CursorValue>): String {
        // Convert CursorValue objects to JsonElements
        val jsonValues = values.mapValues { (_, value) -> value.toJsonElement() }

        // Serialize to JSON
        val jsonString = json.encodeToString(CursorData(jsonValues))

        // Encode with Base64 (URL-safe)
        return Base64.getUrlEncoder().encodeToString(jsonString.toByteArray())
    }

    override fun decode(encoded: String): Map<String, CursorValue> {
        try {
            // Decode Base64
            val jsonBytes = Base64.getUrlDecoder().decode(encoded)
            val jsonString = String(jsonBytes)

            // Deserialize from JSON
            val cursorData = json.decodeFromString<CursorData>(jsonString)

            // Convert JsonElements back to CursorValue objects
            return cursorData.values.mapValues { (_, element) -> element.toCursorValue() }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid cursor format", e)
        }
    }
}

/**
 * Encrypted cursor encoder for enhanced security.
 *
 * This encoder adds AES encryption on top of Base64 encoding.
 *
 * @param encryptionKey Secret key for encryption
 */
class EncryptedCursorEncoder(private val encryptionKey: String) : CursorEncoder {
    private val baseEncoder = SerializationCursorEncoder()

    override fun encode(values: Map<String, CursorValue>): String {
        // First encode with base encoder
        val baseEncoded = baseEncoder.encode(values)

        // Encrypt the result
        val encryptedBytes = encrypt(baseEncoded.toByteArray(), encryptionKey)

        // Final Base64 encoding for URL safety
        return Base64.getUrlEncoder().encodeToString(encryptedBytes)
    }

    override fun decode(encoded: String): Map<String, CursorValue> {
        try {
            // First Base64 decode
            val encryptedBytes = Base64.getUrlDecoder().decode(encoded)

            // Decrypt
            val baseEncodedBytes = decrypt(encryptedBytes, encryptionKey)
            val baseEncoded = String(baseEncodedBytes)

            // Use base encoder to finish decoding
            return baseEncoder.decode(baseEncoded)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid or corrupted cursor", e)
        }
    }

    // AES encryption
    private fun encrypt(data: ByteArray, key: String): ByteArray {
        val secretKey = generateKey(key)
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }

    // AES decryption
    private fun decrypt(encryptedData: ByteArray, key: String): ByteArray {
        val secretKey = generateKey(key)
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(encryptedData)
    }

    // Generate AES key from string
    private fun generateKey(key: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(key.toByteArray())
        return SecretKeySpec(keyBytes.copyOf(16), "AES")
    }
}
