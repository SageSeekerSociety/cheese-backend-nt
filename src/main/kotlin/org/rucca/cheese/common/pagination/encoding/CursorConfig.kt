package org.rucca.cheese.common.pagination.encoding

/**
 * Global cursor encoding configuration.
 *
 * This singleton configures cursor encoding settings application-wide.
 */
object CursorConfig {
    // Default encoder uses only Base64 without encryption
    private var encoder: CursorEncoder = SerializationCursorEncoder()

    /**
     * Enable encryption for cursor values.
     *
     * Use this for production environments or when cursor values contain sensitive information.
     *
     * @param key Encryption key (should be securely stored in configuration)
     */
    fun enableEncryption(key: String) {
        encoder = EncryptedCursorEncoder(key)
    }

    /**
     * Disable encryption, using only Base64 encoding.
     *
     * Suitable for development environments where cursor debugging is needed.
     */
    fun disableEncryption() {
        encoder = SerializationCursorEncoder()
    }

    /**
     * Set a custom cursor encoder.
     *
     * @param customEncoder Custom encoder implementation
     */
    fun setEncoder(customEncoder: CursorEncoder) {
        encoder = customEncoder
    }

    /**
     * Get the configured cursor encoder.
     *
     * @return The current cursor encoder
     */
    fun getEncoder(): CursorEncoder = encoder
}
