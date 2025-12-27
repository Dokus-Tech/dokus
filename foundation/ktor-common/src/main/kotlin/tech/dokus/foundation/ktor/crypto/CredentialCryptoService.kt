package tech.dokus.foundation.ktor.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service for encrypting/decrypting sensitive credentials like API keys.
 * Uses AES-256-GCM for authenticated encryption.
 */
interface CredentialCryptoService {
    /**
     * Encrypt a plaintext credential.
     * @param plaintext The plaintext value to encrypt
     * @return Base64-encoded encrypted value with IV prepended
     */
    fun encrypt(plaintext: String): String

    /**
     * Decrypt an encrypted credential.
     * @param ciphertext Base64-encoded encrypted value with IV prepended
     * @return The decrypted plaintext value
     */
    fun decrypt(ciphertext: String): String
}

/**
 * AES-256-GCM implementation of CredentialCryptoService.
 *
 * The encryption key is derived from a master secret using PBKDF2.
 * Each encryption uses a random 12-byte IV (nonce) for GCM mode.
 * The IV is prepended to the ciphertext for storage.
 *
 * @param masterSecret The master secret used to derive the encryption key.
 *                     Should be at least 32 characters and stored securely (e.g., environment variable).
 */
class AesGcmCredentialCryptoService(masterSecret: String) : CredentialCryptoService {
    private val secretKey: SecretKey
    private val random = SecureRandom()

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val GCM_IV_LENGTH = 12  // 96 bits recommended for GCM
        private const val GCM_TAG_LENGTH = 128  // 128-bit authentication tag
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH = 256  // AES-256
        private val SALT = "dokus-peppol-credential-salt".toByteArray()  // Static salt (secret is unique per deployment)
    }

    init {
        require(masterSecret.length >= 32) {
            "Master secret must be at least 32 characters for secure key derivation"
        }
        secretKey = deriveKey(masterSecret)
    }

    private fun deriveKey(secret: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), SALT, PBKDF2_ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, KEY_ALGORITHM)
    }

    override fun encrypt(plaintext: String): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Prepend IV to ciphertext
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    override fun decrypt(ciphertext: String): String {
        val combined = Base64.getDecoder().decode(ciphertext)

        // Extract IV from beginning of ciphertext
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val plaintext = cipher.doFinal(encrypted)
        return String(plaintext, Charsets.UTF_8)
    }
}
