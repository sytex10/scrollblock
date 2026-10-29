package com.example.scrollblock.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN'i asla düz metin olarak saklamayan, Android'den bağımsız (unit test edilebilir) hash yardımcısı.
 *
 * - PBKDF2WithHmacSHA256, 120.000 iterasyon, cihaza özel 16 bayt rastgele salt.
 * - 4 haneli bir PIN'in olası kombinasyonu yalnızca 10.000 olduğundan asıl koruma
 *   [PinManager] içindeki deneme sınırı/kilitlenmedir; hash, dosya ele geçirilse bile
 *   PIN'in doğrudan okunmasını engeller.
 */
object PinHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun isValidFormat(pin: String): Boolean = pin.length == 4 && pin.all { it in '0'..'9' }

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Sabit zamanlı karşılaştırma (timing attack'e karşı). */
    fun verify(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        if (!isValidFormat(pin)) return false
        return MessageDigest.isEqual(hash(pin, salt), expectedHash)
    }

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun fromHex(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex" }
        return ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
