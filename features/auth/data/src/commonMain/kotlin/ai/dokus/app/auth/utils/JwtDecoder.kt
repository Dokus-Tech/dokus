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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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

            val organizationsJson = jsonObject[JwtClaims.CLAIM_ORGANIZATIONS]?.jsonPrimitive?.content
            val organizations = parseOrganizations(organizationsJson)

            JwtClaims(
                userId = UserId(userIdStr),
                email = email,
                organizations = organizations,
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

    private fun parseOrganizations(organizationsJson: String?): List<OrganizationScope> {
        if (organizationsJson.isNullOrEmpty()) return emptyList()

        return try {
            val orgsArray = json.decodeFromString<JsonArray>(organizationsJson)
            orgsArray.mapNotNull { element ->
                val obj = element.jsonObject
                val orgId = obj[JwtClaims.CLAIM_ORGANIZATION_ID]?.jsonPrimitive?.content ?: return@mapNotNull null
                val tierStr = obj[JwtClaims.CLAIM_SUBSCRIPTION_TIER]?.jsonPrimitive?.content ?: return@mapNotNull null
                val roleStr = obj[JwtClaims.CLAIM_ROLE]?.jsonPrimitive?.content

                val permissions = obj[JwtClaims.CLAIM_PERMISSIONS]?.jsonArray?.mapNotNull { perm ->
                    try { Permission.valueOf(perm.jsonPrimitive.content) } catch (_: Exception) { null }
                }?.toSet() ?: emptySet()

                val tier = try { SubscriptionTier.valueOf(tierStr) } catch (_: Exception) { return@mapNotNull null }
                val role = roleStr?.let { try { UserRole.valueOf(it) } catch (_: Exception) { null } }

                OrganizationScope(
                    organizationId = OrganizationId(Uuid.parse(orgId)),
                    permissions = permissions,
                    subscriptionTier = tier,
                    role = role
                )
            }
        } catch (e: Exception) {
            emptyList()
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
