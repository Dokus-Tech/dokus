package tech.dokus.backend.routes.cashflow

import io.ktor.http.Headers
import kotlinx.datetime.Clock
import tech.dokus.peppol.config.WebhookSecurityConfig
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Stateless verifier for PEPPOL webhook signatures.
 */
class PeppolWebhookSignatureVerifier(
    private val config: WebhookSecurityConfig
) {

    fun verify(headers: Headers, rawBody: String): Boolean {
        if (!config.enabled) {
            return true
        }

        val timestampRaw = headers[config.timestampHeader]?.trim()
            ?: return false
        val providedSignature = headers[config.signatureHeader]?.trim()
            ?: return false

        val timestampEpochSeconds = parseTimestampSeconds(timestampRaw) ?: return false
        val nowEpochSeconds = Clock.System.now().epochSeconds
        val drift = kotlin.math.abs(nowEpochSeconds - timestampEpochSeconds)
        if (drift > config.maxSkewSeconds) {
            return false
        }

        val payload = "$timestampRaw.$rawBody"
        val expected = hmacSha256Hex(config.sharedSecret, payload)
        val normalizedProvided = providedSignature.removePrefix("sha256=")

        return constantTimeEqualsHex(normalizedProvided, expected)
    }

    private fun parseTimestampSeconds(raw: String): Long? {
        raw.toLongOrNull()?.let { numeric ->
            // Assume millisecond precision when value is too large for epoch seconds.
            return if (numeric > 9_999_999_999L) numeric / 1000 else numeric
        }

        return runCatching { Instant.parse(raw).epochSecond }.getOrNull()
    }

    private fun hmacSha256Hex(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun constantTimeEqualsHex(providedHex: String, expectedHex: String): Boolean {
        val providedBytes = providedHex.hexToBytes() ?: return false
        val expectedBytes = expectedHex.hexToBytes() ?: return false
        return java.security.MessageDigest.isEqual(providedBytes, expectedBytes)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching {
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }.getOrNull()
    }
}
