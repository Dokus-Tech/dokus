@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.foundation.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.TenantScope
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.database.now
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class JwtGenerator(
    private val config: JwtConfig
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

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
        tenant: TenantScope?
    ): JwtClaims {
        val nowTime = now()
        val accessExpiry = nowTime + JwtClaims.ACCESS_TOKEN_EXPIRY_SECONDS.seconds

        return JwtClaims(
            userId = userId,
            email = email,
            tenant = tenant,
            iat = nowTime.epochSeconds,
            exp = accessExpiry.epochSeconds,
            jti = Uuid.random().toString(),
            iss = config.issuer,
            aud = config.audience
        )
    }

    private fun createAccessToken(claims: JwtClaims): String {
        val builder = JWT.create()
            .withIssuer(claims.iss)
            .withAudience(claims.aud)
            .withSubject(claims.userId.value.toString())
            .withJWTId(claims.jti)
            .withClaim(JwtClaims.CLAIM_EMAIL, claims.email)
            .withIssuedAt(Date.from(Instant.ofEpochSecond(claims.iat)))
            .withExpiresAt(Date.from(Instant.ofEpochSecond(claims.exp)))

        claims.tenant?.let { tenant ->
            builder
                .withClaim(JwtClaims.CLAIM_TENANT_ID, tenant.tenantId.value.toString())
                .withArrayClaim(
                    JwtClaims.CLAIM_PERMISSIONS,
                    tenant.permissions.map { it.name }.toTypedArray()
                )
                .withClaim(JwtClaims.CLAIM_SUBSCRIPTION_TIER, tenant.subscriptionTier.name)
                .apply {
                    tenant.role?.name?.let { withClaim(JwtClaims.CLAIM_ROLE, it) }
                }
        }

        return builder.sign(algorithm)
    }

    private fun createRefreshToken(userId: UserId): String {
        val nowTime = now()
        val refreshExpiry = nowTime + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days

        return JWT.create()
            .withIssuer(config.issuer)
            .withSubject(userId.value.toString())
            // Refresh tokens must be unique even when issued in the same second.
            // (Auth0 JWT date claims are second-precision, so iat/exp alone is not enough.)
            .withJWTId(Uuid.random().toString())
            .withClaim(JwtClaims.CLAIM_TYPE, JwtClaims.TOKEN_TYPE_REFRESH)
            .withIssuedAt(Date.from(Instant.ofEpochMilli(nowTime.toEpochMilliseconds())))
            .withExpiresAt(Date.from(Instant.ofEpochMilli(refreshExpiry.toEpochMilliseconds())))
            .sign(algorithm)
    }
}
