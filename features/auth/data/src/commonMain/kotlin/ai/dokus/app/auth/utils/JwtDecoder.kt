package ai.dokus.app.auth.utils

import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.TokenStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Utility class for decoding and validating JWT tokens.
 * Handles Base64 decoding and claim extraction.
 */
@OptIn(ExperimentalEncodingApi::class)
class JwtDecoder {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Decodes a JWT token and extracts the claims.
     *
     * @param token The JWT token string
     * @return JwtClaims if valid, null if invalid or malformed
     */
    fun decode(token: String): JwtClaims? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payload = parts[1]
            // Handle URL-safe base64 and add padding if needed
            val normalizedPayload = payload
                .replace('-', '+')
                .replace('_', '/')
            val paddedPayload = when (normalizedPayload.length % 4) {
                2 -> "$normalizedPayload=="
                3 -> "$normalizedPayload="
                else -> normalizedPayload
            }
            val decodedPayload = Base64.decode(paddedPayload).decodeToString()

            // Parse as JsonObject first to handle claim names
            val jsonObject = json.decodeFromString<JsonObject>(decodedPayload)

            JwtClaims(
                userId = jsonObject["sub"]?.jsonPrimitive?.content ?: return null,
                matricule = jsonObject["preferred_username"]?.jsonPrimitive?.content,
                email = jsonObject["email"]?.jsonPrimitive?.content ?: return null,
                fullName = jsonObject["name"]?.jsonPrimitive?.content ?: "",
                roles = jsonObject["groups"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content }?.toSet() ?: emptySet(),
                permissions = jsonObject["permissions"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content }?.toSet()
                    ?: emptySet(),
                unitCode = jsonObject["unit"]?.jsonPrimitive?.content,
                department = jsonObject["department"]?.jsonPrimitive?.content,
                clearanceLevel = jsonObject["clearance"]?.jsonPrimitive?.content ?: "INTERNAL_USE",
                sessionId = jsonObject["session_id"]?.jsonPrimitive?.content,
                deviceFingerprint = jsonObject["device_fingerprint"]?.jsonPrimitive?.content,
                iat = jsonObject["iat"]?.jsonPrimitive?.longOrNull,
                exp = jsonObject["exp"]?.jsonPrimitive?.longOrNull,
                jti = jsonObject["jti"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            println(e)
            null
        }
    }

    /**
     * Validates a JWT token and returns its status.
     *
     * @param token The JWT token to validate
     * @return TokenStatus indicating the token's validity
     */
    fun validateToken(token: String?): TokenStatus {
        if (token.isNullOrBlank()) return TokenStatus.INVALID

        val claims = decode(token) ?: return TokenStatus.INVALID

        val exp = claims.exp ?: return TokenStatus.INVALID
        throw NotImplementedError("Not implemented")
        // TODO
//        val currentTime = Clock.System.now().epochSeconds
//
//        return when {
//            exp < currentTime -> TokenStatus.EXPIRED
//            exp - currentTime < REFRESH_THRESHOLD_SECONDS -> TokenStatus.REFRESH_NEEDED
//            else -> TokenStatus.VALID
//        }
    }

    /**
     * Checks if a token is expired.
     *
     * @param token The JWT token to check
     * @return true if expired, false otherwise
     */
    fun isExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true

        val claims = decode(token) ?: return true
        val exp = claims.exp ?: return true
        throw NotImplementedError("Not implemented")
//        val currentTime = Clock.System.now().epochSeconds
//
//        return exp < currentTime
    }

    /**
     * Checks if a token needs refreshing.
     *
     * @param token The JWT token to check
     * @return true if refresh is needed, false otherwise
     */
    fun needsRefresh(token: String?): Boolean {
        if (token.isNullOrBlank()) return true

        val status = validateToken(token)
        return status == TokenStatus.REFRESH_NEEDED || status == TokenStatus.EXPIRED
    }

    /**
     * Extracts the expiration time from a token.
     *
     * @param token The JWT token
     * @return Expiration time in epoch seconds, or null if invalid
     */
    fun getExpirationTime(token: String?): Long? {
        if (token.isNullOrBlank()) return null
        return decode(token)?.exp
    }

    companion object {
        // Refresh token 5 minutes before expiry
        private const val REFRESH_THRESHOLD_SECONDS = 5 * 60
    }
}

