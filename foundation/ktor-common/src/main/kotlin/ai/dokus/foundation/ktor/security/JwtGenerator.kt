@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.ktor.database.now
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

/**
 * JWT token generator for authentication.
 * Generates access and refresh tokens with appropriate claims and expiration times.
 */
class JwtGenerator(
    secret: String,
    private val issuer: String = "dokus-auth"
) {
    private val algorithm = Algorithm.HMAC256(secret)

    /**
     * Generates both access and refresh tokens for authenticated users.
     *
     * @param userId The user's unique identifier
     * @param email The user's email address
     * @param fullName The user's full name
     * @param tenantId The tenant/organization identifier
     * @param roles Set of roles assigned to the user
     * @return LoginResponse containing access token, refresh token, and expiration time
     */
    fun generateTokens(
        userId: UserId,
        email: String,
        fullName: String,
        tenantId: TenantId,
        roles: Set<String>
    ): LoginResponse {
        val nowTime = now()
        val accessExpiry = nowTime + 1.hours
        val refreshExpiry = nowTime + 30.days

        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.value)
            .withClaim("email", email)
            .withClaim("name", fullName)
            .withClaim("tenant_id", tenantId.value.toString())
            .withArrayClaim("groups", roles.toTypedArray())
            .withIssuedAt(Date.from(Instant.ofEpochMilli(nowTime.toEpochMilliseconds())))
            .withExpiresAt(Date.from(Instant.ofEpochMilli(accessExpiry.toEpochMilliseconds())))
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.value)
            .withClaim("type", "refresh")
            .withIssuedAt(Date.from(Instant.ofEpochMilli(nowTime.toEpochMilliseconds())))
            .withExpiresAt(Date.from(Instant.ofEpochMilli(refreshExpiry.toEpochMilliseconds())))
            .sign(algorithm)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600L // 1 hour in seconds
        )
    }
}
