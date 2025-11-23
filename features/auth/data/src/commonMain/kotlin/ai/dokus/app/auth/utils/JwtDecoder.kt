package ai.dokus.app.auth.utils

import ai.dokus.foundation.domain.enums.Permission
import ai.dokus.foundation.domain.enums.SubscriptionTier
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.OrganizationScope
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

@OptIn(ExperimentalEncodingApi::class, ExperimentalTime::class, ExperimentalUuidApi::class)
class JwtDecoder {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun decode(token: String): JwtClaims? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payload = parts[1]
            val normalizedPayload = payload.replace('-', '+').replace('_', '/')
            val paddedPayload = when (normalizedPayload.length % 4) {
                2 -> "$normalizedPayload=="
                3 -> "$normalizedPayload="
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

            val organization = parseOrganization(jsonObject)

            JwtClaims(
                userId = UserId(userIdStr),
                email = email,
                organization = organization,
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

    private fun parseOrganization(jsonObject: JsonObject): OrganizationScope? {
        val orgIdStr = jsonObject[JwtClaims.CLAIM_ORGANIZATION_ID]?.jsonPrimitive?.content
        val tierStr = jsonObject[JwtClaims.CLAIM_SUBSCRIPTION_TIER]?.jsonPrimitive?.content

        if (!orgIdStr.isNullOrBlank() && !tierStr.isNullOrBlank()) {
            return runCatching {
                val permissions = jsonObject[JwtClaims.CLAIM_PERMISSIONS]
                    ?.jsonArray
                    ?.mapNotNull { perm ->
                        runCatching { Permission.valueOf(perm.jsonPrimitive.content) }.getOrNull()
                    }
                    ?.toSet() ?: emptySet()

                val role = jsonObject[JwtClaims.CLAIM_ROLE]
                    ?.jsonPrimitive
                    ?.content
                    ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

                OrganizationScope(
                    organizationId = OrganizationId(Uuid.parse(orgIdStr)),
                    permissions = permissions,
                    subscriptionTier = SubscriptionTier.valueOf(tierStr),
                    role = role
                )
            }.getOrNull()
        }

        // Legacy fallback: parse first organization from old organizations array claim
        val organizationsJson = jsonObject[JwtClaims.CLAIM_ORGANIZATIONS]?.jsonPrimitive?.content
        if (organizationsJson.isNullOrEmpty()) return null

        return runCatching {
            val orgsArray = json.decodeFromString<List<JsonObject>>(organizationsJson)
            val first = orgsArray.firstOrNull() ?: return null
            val legacyOrgId = first[JwtClaims.CLAIM_ORGANIZATION_ID]?.jsonPrimitive?.content ?: return null
            val legacyTier = first[JwtClaims.CLAIM_SUBSCRIPTION_TIER]?.jsonPrimitive?.content ?: return null
            val legacyPermissions = first[JwtClaims.CLAIM_PERMISSIONS]
                ?.jsonArray
                ?.mapNotNull { perm ->
                    runCatching { Permission.valueOf(perm.jsonPrimitive.content) }.getOrNull()
                }
                ?.toSet() ?: emptySet()
            val legacyRole = first[JwtClaims.CLAIM_ROLE]
                ?.jsonPrimitive
                ?.content
                ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

            OrganizationScope(
                organizationId = OrganizationId(Uuid.parse(legacyOrgId)),
                permissions = legacyPermissions,
                subscriptionTier = SubscriptionTier.valueOf(legacyTier),
                role = legacyRole
            )
        }.getOrNull()
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
