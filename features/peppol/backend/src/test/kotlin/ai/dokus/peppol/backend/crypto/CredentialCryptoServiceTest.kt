package ai.dokus.peppol.backend.crypto

import ai.dokus.foundation.ktor.crypto.AesGcmCredentialCryptoService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class CredentialCryptoServiceTest {
    private val masterSecret = "this-is-a-test-secret-key-with-at-least-32-characters"
    private val cryptoService = AesGcmCredentialCryptoService(masterSecret)

    @Test
    fun `encrypt and decrypt returns original value`() {
        val plaintext = "my-super-secret-api-key-12345"

        val encrypted = cryptoService.encrypt(plaintext)
        val decrypted = cryptoService.decrypt(encrypted)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypted value is different from plaintext`() {
        val plaintext = "my-api-key"

        val encrypted = cryptoService.encrypt(plaintext)

        assertNotEquals(plaintext, encrypted)
    }

    @Test
    fun `different plaintexts produce different ciphertexts`() {
        val plaintext1 = "api-key-1"
        val plaintext2 = "api-key-2"

        val encrypted1 = cryptoService.encrypt(plaintext1)
        val encrypted2 = cryptoService.encrypt(plaintext2)

        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `same plaintext produces different ciphertexts due to random IV`() {
        val plaintext = "my-api-key"

        val encrypted1 = cryptoService.encrypt(plaintext)
        val encrypted2 = cryptoService.encrypt(plaintext)

        // Due to random IV, same plaintext should produce different ciphertexts
        assertNotEquals(encrypted1, encrypted2)

        // But both should decrypt to the same value
        assertEquals(plaintext, cryptoService.decrypt(encrypted1))
        assertEquals(plaintext, cryptoService.decrypt(encrypted2))
    }

    @Test
    fun `handles empty string`() {
        val plaintext = ""

        val encrypted = cryptoService.encrypt(plaintext)
        val decrypted = cryptoService.decrypt(encrypted)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `handles unicode characters`() {
        val plaintext = "api-key-with-émojis-🔐-and-ñ"

        val encrypted = cryptoService.encrypt(plaintext)
        val decrypted = cryptoService.decrypt(encrypted)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `handles long strings`() {
        val plaintext = "a".repeat(10000)

        val encrypted = cryptoService.encrypt(plaintext)
        val decrypted = cryptoService.decrypt(encrypted)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `different keys produce different ciphertexts`() {
        val anotherCryptoService = AesGcmCredentialCryptoService(
            "another-secret-key-that-is-also-at-least-32-characters"
        )
        val plaintext = "my-api-key"

        val encrypted1 = cryptoService.encrypt(plaintext)
        val encrypted2 = anotherCryptoService.encrypt(plaintext)

        // Different keys should produce different ciphertexts
        // (technically they could be the same by chance, but probability is negligible)
        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `cannot decrypt with wrong key`() {
        val anotherCryptoService = AesGcmCredentialCryptoService(
            "another-secret-key-that-is-also-at-least-32-characters"
        )
        val plaintext = "my-api-key"

        val encrypted = cryptoService.encrypt(plaintext)

        // Decryption with wrong key should fail (GCM authentication)
        assertThrows<Exception> {
            anotherCryptoService.decrypt(encrypted)
        }
    }

    @Test
    fun `rejects short master secret`() {
        assertThrows<IllegalArgumentException> {
            AesGcmCredentialCryptoService("short-key")
        }
    }

    @Test
    fun `accepts exactly 32 character key`() {
        val key = "a".repeat(32)
        val service = AesGcmCredentialCryptoService(key)

        val encrypted = service.encrypt("test")
        val decrypted = service.decrypt(encrypted)

        assertEquals("test", decrypted)
    }
}
