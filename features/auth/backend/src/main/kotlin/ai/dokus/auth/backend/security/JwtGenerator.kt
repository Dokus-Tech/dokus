package ai.dokus.auth.backend.security

import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.model.auth.LoginResponse
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * JWT token generator for authentication.
 * Generates access and refresh tokens with appropriate claims and expiration times.
 */
class JwtGenerator(
    private val secret: String,
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
        val now = Clock.System.now()
        val accessExpiry = now + 1.hours
        val refreshExpiry = now + 30.days

        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.value)
            .withClaim("email", email)
            .withClaim("name", fullName)
            .withClaim("tenant_id", tenantId.value.toString())
            .withArrayClaim("groups", roles.toTypedArray())
            .withIssuedAt(now.toEpochMilliseconds())
            .withExpiresAt(accessExpiry.toEpochMilliseconds())
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.value)
            .withClaim("type", "refresh")
            .withIssuedAt(now.toEpochMilliseconds())
            .withExpiresAt(refreshExpiry.toEpochMilliseconds())
            .sign(algorithm)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600L // 1 hour in seconds
        )
    }
}
