package ai.dokus.auth.backend.utils

import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.auth.LoginResponse
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.Date

/**
 * JWT Token Generator for authentication
 *
 * Generates access and refresh tokens with proper claims and expiry times.
 * Access tokens are short-lived (1 hour) and contain user information and permissions.
 * Refresh tokens are long-lived (30 days) and used to obtain new access tokens.
 */
class JwtGenerator(
    private val secret: String,
    private val issuer: String = "https://dokus.ai",
    private val audience: String = "dokus-api"
) {
    private val logger = LoggerFactory.getLogger(JwtGenerator::class.java)
    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    /**
     * Generate a complete login response with access and refresh tokens
     *
     * @param userId The unique identifier for the user
     * @param email User's email address
     * @param name User's full name
     * @param tenantId The tenant this user belongs to
     * @param roles List of user roles for authorization
     * @return LoginResponse containing accessToken, refreshToken, and expiresIn
     */
    fun generateLoginResponse(
        userId: UserId,
        email: String,
        name: String,
        tenantId: TenantId,
        roles: List<UserRole>
    ): Result<LoginResponse> = runCatching {
        logger.debug("Generating JWT tokens for user: $userId, tenant: $tenantId")

        val now = Instant.now()
        val accessTokenExpiry = now.plusSeconds(3600) // 1 hour
        val refreshTokenExpiry = now.plusSeconds(30 * 24 * 3600) // 30 days

        val accessToken = generateAccessToken(
            userId = userId,
            email = email,
            name = name,
            tenantId = tenantId,
            roles = roles,
            issuedAt = now.toEpochMilli(),
            expiresAt = accessTokenExpiry.toEpochMilli()
        )

        val refreshToken = generateRefreshToken(
            userId = userId,
            issuedAt = now.toEpochMilli(),
            expiresAt = refreshTokenExpiry.toEpochMilli()
        )

        logger.info("Successfully generated JWT tokens for user: $userId")

        LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600L // 1 hour in seconds
        )
    }.onFailure { throwable ->
        logger.error("Failed to generate JWT tokens for user: $userId", throwable)
    }

    /**
     * Generate an access token with user claims
     *
     * Access token claims:
     * - sub: userId (subject)
     * - email: user's email
     * - name: user's full name
     * - tenant_id: tenant identifier
     * - groups: user roles for authorization
     * - iat: issued at timestamp
     * - exp: expiration timestamp
     */
    private fun generateAccessToken(
        userId: UserId,
        email: String,
        name: String,
        tenantId: TenantId,
        roles: List<UserRole>,
        issuedAt: Long,
        expiresAt: Long
    ): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.value)
            .withClaim("email", email)
            .withClaim("name", name)
            .withClaim("tenant_id", tenantId.toString())
            .withArrayClaim("groups", roles.map { it.dbValue }.toTypedArray())
            .withIssuedAt(Date(issuedAt))
            .withExpiresAt(Date(expiresAt))
            .sign(algorithm)
    }

    /**
     * Generate a refresh token with minimal claims
     *
     * Refresh token claims:
     * - sub: userId (subject)
     * - type: "refresh" (token type identifier)
     * - iat: issued at timestamp
     * - exp: expiration timestamp
     */
    private fun generateRefreshToken(
        userId: UserId,
        issuedAt: Long,
        expiresAt: Long
    ): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.value)
            .withClaim("type", "refresh")
            .withIssuedAt(Date(issuedAt))
            .withExpiresAt(Date(expiresAt))
            .sign(algorithm)
    }

    /**
     * Generate a new access token from a refresh token
     *
     * @param userId The user identifier from the validated refresh token
     * @param email User's email address
     * @param name User's full name
     * @param tenantId The tenant this user belongs to
     * @param roles List of user roles for authorization
     * @return New access token
     */
    fun refreshAccessToken(
        userId: UserId,
        email: String,
        name: String,
        tenantId: TenantId,
        roles: List<UserRole>
    ): Result<String> = runCatching {
        logger.debug("Refreshing access token for user: $userId")

        val now = Instant.now()
        val accessTokenExpiry = now.plusSeconds(3600) // 1 hour

        val accessToken = generateAccessToken(
            userId = userId,
            email = email,
            name = name,
            tenantId = tenantId,
            roles = roles,
            issuedAt = now.toEpochMilli(),
            expiresAt = accessTokenExpiry.toEpochMilli()
        )

        logger.info("Successfully refreshed access token for user: $userId")
        accessToken
    }.onFailure { throwable ->
        logger.error("Failed to refresh access token for user: $userId", throwable)
    }

    companion object {
        /**
         * Create a JwtGenerator from environment configuration
         *
         * @param secret JWT secret key (from JWT_SECRET env var or config)
         * @param issuer JWT issuer (from JWT_ISSUER env var or config)
         * @param audience JWT audience (from JWT_AUDIENCE env var or config)
         * @return JwtGenerator instance
         */
        fun fromConfig(
            secret: String = System.getenv("JWT_SECRET") ?: "secret",
            issuer: String = System.getenv("JWT_ISSUER") ?: "https://dokus.ai",
            audience: String = System.getenv("JWT_AUDIENCE") ?: "dokus-api"
        ): JwtGenerator {
            require(secret.isNotBlank()) { "JWT secret cannot be blank" }
            require(issuer.isNotBlank()) { "JWT issuer cannot be blank" }
            require(audience.isNotBlank()) { "JWT audience cannot be blank" }

            if (secret == "secret") {
                LoggerFactory.getLogger(JwtGenerator::class.java)
                    .warn("Using default JWT secret - THIS IS INSECURE IN PRODUCTION!")
            }

            return JwtGenerator(secret, issuer, audience)
        }
    }
}
