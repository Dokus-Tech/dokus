@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.OrganizationScope
import ai.dokus.foundation.ktor.database.now
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class JwtGenerator(
    secret: String,
    private val issuer: String = JwtClaims.ISS_DEFAULT
) {
    private val algorithm = Algorithm.HMAC256(secret)
    private val json = Json { encodeDefaults = true }

    fun generateTokens(claims: JwtClaims): LoginResponse {
        val accessToken = createAccessToken(claims)
        val refreshToken = createRefreshToken(claims.userId)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = JwtClaims.ACCESS_TOKEN_EXPIRY_SECONDS
        )
    }

    fun generateClaims(
        userId: UserId,
        email: String,
        organizations: List<OrganizationScope>
    ): JwtClaims {
        val nowTime = now()
        val accessExpiry = nowTime + 1.hours

        return JwtClaims(
            userId = userId,
            email = email,
            organizations = organizations,
            iat = nowTime.epochSeconds,
            exp = accessExpiry.epochSeconds,
            jti = Uuid.random().toString(),
            iss = issuer,
            aud = JwtClaims.AUD_DEFAULT
        )
    }

    private fun createAccessToken(claims: JwtClaims): String {
        val organizationsJson = json.encodeToString(claims.organizations.map { org ->
            mapOf(
                JwtClaims.CLAIM_ORGANIZATION_ID to org.organizationId.value.toString(),
                JwtClaims.CLAIM_PERMISSIONS to org.permissions.map { it.name },
                JwtClaims.CLAIM_SUBSCRIPTION_TIER to org.subscriptionTier.name,
                JwtClaims.CLAIM_ROLE to org.role?.name
            )
        })

        return JWT.create()
            .withIssuer(claims.iss)
            .withAudience(claims.aud)
            .withSubject(claims.userId.value.toString())
            .withJWTId(claims.jti)
            .withClaim(JwtClaims.CLAIM_EMAIL, claims.email)
            .withClaim(JwtClaims.CLAIM_ORGANIZATIONS, organizationsJson)
            .withIssuedAt(Date.from(Instant.ofEpochSecond(claims.iat)))
            .withExpiresAt(Date.from(Instant.ofEpochSecond(claims.exp)))
            .sign(algorithm)
    }

    private fun createRefreshToken(userId: UserId): String {
        val nowTime = now()
        val refreshExpiry = nowTime + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days

        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.value.toString())
            .withClaim("type", "refresh")
            .withIssuedAt(Date.from(Instant.ofEpochMilli(nowTime.toEpochMilliseconds())))
            .withExpiresAt(Date.from(Instant.ofEpochMilli(refreshExpiry.toEpochMilliseconds())))
            .sign(algorithm)
    }
}
