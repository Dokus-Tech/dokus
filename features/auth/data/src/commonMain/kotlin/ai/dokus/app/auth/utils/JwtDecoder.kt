package ai.dokus.app.auth.utils

import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.enums.Permission
import ai.dokus.foundation.domain.enums.SubscriptionTier
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.TokenStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Utility class for decoding and validating JWT tokens.
 * Handles Base64 decoding and claim extraction.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalTime::class, ExperimentalUuidApi::class)
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

            // Parse required fields
            val userIdStr = jsonObject["sub"]?.jsonPrimitive?.content ?: return null
            val emailStr = jsonObject["email"]?.jsonPrimitive?.content ?: return null
            val tenantIdStr = jsonObject["tenant_id"]?.jsonPrimitive?.content ?: return null
            val organizationIdStr = jsonObject["organization_id"]?.jsonPrimitive?.content ?: return null
            val organizationName = jsonObject["organization_name"]?.jsonPrimitive?.content ?: return null

            val roleStr = jsonObject["role"]?.jsonPrimitive?.content ?: return null
            val role = try {
                UserRole.valueOf(roleStr)
            } catch (e: Exception) {
                return null
            }

            val permissionsArray = jsonObject["permissions"]?.jsonArray ?: return null
            val permissions = permissionsArray.mapNotNull {
                try {
                    Permission.valueOf(it.jsonPrimitive.content)
                } catch (e: Exception) {
                    null
                }
            }.toSet()

            val subscriptionTierStr = jsonObject["subscription_tier"]?.jsonPrimitive?.content ?: return null
            val subscriptionTier = try {
                SubscriptionTier.valueOf(subscriptionTierStr)
            } catch (e: Exception) {
                return null
            }

            val iat = jsonObject["iat"]?.jsonPrimitive?.longOrNull ?: return null
            val exp = jsonObject["exp"]?.jsonPrimitive?.longOrNull ?: return null
            val jti = jsonObject["jti"]?.jsonPrimitive?.content ?: return null

            // Parse optional fields
            val matricule = jsonObject["matricule"]?.jsonPrimitive?.content
            val locale = jsonObject["locale"]?.jsonPrimitive?.content ?: "nl-BE"
            val featureFlags = jsonObject["feature_flags"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content
            }?.toSet() ?: emptySet()

            val isAccountantAccess = jsonObject["is_accountant_access"]?.jsonPrimitive?.content?.toBoolean() ?: false
            val accountantOrganizationId = jsonObject["accountant_organization_id"]?.jsonPrimitive?.content?.let {
                OrganizationId(Uuid.parse(it))
            }

            val iss = jsonObject["iss"]?.jsonPrimitive?.content ?: "dokus"
            val aud = jsonObject["aud"]?.jsonPrimitive?.content ?: "dokus-api"

            JwtClaims(
                userId = UserId(userIdStr),
                email = emailStr,
                tenantId = TenantId(Uuid.parse(tenantIdStr)),
                organizationId = OrganizationId(Uuid.parse(organizationIdStr)),
                organizationName = organizationName,
                role = role,
                permissions = permissions,
                matricule = matricule,
                locale = locale,
                subscriptionTier = subscriptionTier,
                featureFlags = featureFlags,
                isAccountantAccess = isAccountantAccess,
                accountantOrganizationId = accountantOrganizationId,
                iat = iat,
                exp = exp,
                jti = jti,
                iss = iss,
                aud = aud
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

        val currentTime = Clock.System.now().epochSeconds

        return when {
            claims.exp < currentTime -> TokenStatus.EXPIRED
            claims.exp - currentTime < REFRESH_THRESHOLD_SECONDS -> TokenStatus.REFRESH_NEEDED
            else -> TokenStatus.VALID
        }
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
        val currentTime = Clock.System.now().epochSeconds

        return claims.exp < currentTime
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
