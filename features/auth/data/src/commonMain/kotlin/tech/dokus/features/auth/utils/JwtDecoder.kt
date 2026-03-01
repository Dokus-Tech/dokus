@file:Suppress(
    "ReturnCount", // JWT decoding requires multiple validation returns
    "TooGenericExceptionCaught" // JWT parsing can fail in various ways
)

package tech.dokus.features.auth.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.TokenStatus
import tech.dokus.domain.utils.json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Number of parts in a valid JWT (header.payload.signature) */
private const val JwtPartsCount = 3

/** JWT delimiter character */
private const val JwtDelimiter = "."

/** Base64 padding modulo value indicating 2 padding chars needed */
private const val Base64PaddingMod2 = 2

/** Base64 padding modulo value indicating 1 padding char needed */
private const val Base64PaddingMod3 = 3

/** Base64 block size for padding calculation */
private const val Base64BlockSize = 4

@OptIn(ExperimentalEncodingApi::class, ExperimentalTime::class)
class JwtDecoder {

    fun decode(token: String): JwtClaims? {
        return try {
            val parts = token.split(JwtDelimiter)
            if (parts.size != JwtPartsCount) return null

            val payload = parts[1]
            val normalizedPayload = payload.replace('-', '+').replace('_', '/')
            val paddedPayload = when (normalizedPayload.length % Base64BlockSize) {
                Base64PaddingMod2 -> "$normalizedPayload=="
                Base64PaddingMod3 -> "$normalizedPayload="
                else -> normalizedPayload
            }
            val decodedPayload = Base64.decode(paddedPayload).decodeToString()

            val jsonObject = json.decodeFromString<JsonObject>(decodedPayload)

            val userIdStr = jsonObject[JwtClaims.CLAIM_SUB]?.jsonPrimitive?.content ?: return null
            val email = jsonObject[JwtClaims.CLAIM_EMAIL]?.jsonPrimitive?.content ?: return null
            val iat = jsonObject[JwtClaims.CLAIM_IAT]?.jsonPrimitive?.longOrNull ?: return null
            val exp = jsonObject[JwtClaims.CLAIM_EXP]?.jsonPrimitive?.longOrNull ?: return null
            val jti = jsonObject[JwtClaims.CLAIM_JTI]?.jsonPrimitive?.content ?: return null
            val iss = jsonObject[JwtClaims.CLAIM_ISS]?.jsonPrimitive?.content ?: JwtClaims.ISS_DEFAULT
            val aud = jsonObject[JwtClaims.CLAIM_AUD]?.jsonPrimitive?.content ?: JwtClaims.AUD_DEFAULT

            JwtClaims(
                userId = UserId(userIdStr),
                email = email,
                iat = iat,
                exp = exp,
                jti = jti,
                iss = iss,
                aud = aud
            )
        } catch (e: Exception) {
            println("JWT decode error: $e")
            null
        }
    }

    fun validateToken(token: String?): TokenStatus {
        if (token.isNullOrBlank()) return TokenStatus.INVALID

        val claims = decode(token) ?: return TokenStatus.INVALID
        val currentTime = Clock.System.now().epochSeconds

        return when {
            claims.exp < currentTime -> TokenStatus.EXPIRED
            claims.exp - currentTime < JwtClaims.REFRESH_THRESHOLD_SECONDS -> TokenStatus.REFRESH_NEEDED
            else -> TokenStatus.VALID
        }
    }

    fun isExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        val claims = decode(token) ?: return true
        return claims.exp < Clock.System.now().epochSeconds
    }

    fun needsRefresh(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        val status = validateToken(token)
        return status == TokenStatus.REFRESH_NEEDED || status == TokenStatus.EXPIRED
    }

    fun getExpirationTime(token: String?): Long? {
        if (token.isNullOrBlank()) return null
        return decode(token)?.exp
    }
}
