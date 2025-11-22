@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.OrganizationClaimDto
import ai.dokus.foundation.domain.model.auth.OrganizationScope
import ai.dokus.foundation.ktor.database.now
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
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
        val accessExpiry = nowTime + JwtClaims.ACCESS_TOKEN_EXPIRY_SECONDS.seconds

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
        val organizationsDto = claims.organizations.map { org ->
            OrganizationClaimDto(
                organizationId = org.organizationId.value.toString(),
                permissions = org.permissions.map { it.name },
                subscriptionTier = org.subscriptionTier.name,
                role = org.role?.name
            )
        }

        val organizationsJson = json.encodeToString(organizationsDto)

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
            .withClaim(JwtClaims.CLAIM_TYPE, JwtClaims.TOKEN_TYPE_REFRESH)
            .withIssuedAt(Date.from(Instant.ofEpochMilli(nowTime.toEpochMilliseconds())))
            .withExpiresAt(Date.from(Instant.ofEpochMilli(refreshExpiry.toEpochMilliseconds())))
            .sign(algorithm)
    }
}
