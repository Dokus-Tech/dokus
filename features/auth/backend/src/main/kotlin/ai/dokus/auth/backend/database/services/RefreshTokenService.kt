package ai.dokus.auth.backend.database.services

import ai.dokus.foundation.domain.UserId
import kotlinx.datetime.Instant

/**
 * Service for managing JWT refresh tokens with persistence, rotation, and revocation.
 *
 * Provides secure token lifecycle management including:
 * - Token persistence and validation
 * - Automatic token rotation on refresh
 * - Token revocation for logout
 * - Cleanup of expired tokens
 *
 * Security considerations:
 * - Tokens are revoked on use (rotation)
 * - Expired tokens are rejected
 * - Revoked tokens cannot be used
 * - All user tokens can be revoked simultaneously
 */
interface RefreshTokenService {
    /**
     * Save a refresh token to the database
     *
     * @param userId The user this token belongs to
     * @param token The JWT refresh token string
     * @param expiresAt When this token expires
     * @return Result indicating success or failure
     */
    suspend fun saveRefreshToken(userId: UserId, token: String, expiresAt: Instant): Result<Unit>

    /**
     * Validate a refresh token and rotate it to a new one
     *
     * This implements token rotation security:
     * 1. Validates the old token (not expired, not revoked)
     * 2. Marks the old token as revoked
     * 3. Generates a new token (caller's responsibility)
     * 4. Returns userId for generating new tokens
     *
     * @param oldToken The current refresh token to validate
     * @return Result containing userId and the token ID if successful, or error if invalid
     */
    suspend fun validateAndRotate(oldToken: String): Result<UserId>

    /**
     * Revoke a specific refresh token
     *
     * Used during logout to invalidate the current session.
     *
     * @param token The refresh token to revoke
     * @return Result indicating success or failure
     */
    suspend fun revokeToken(token: String): Result<Unit>

    /**
     * Revoke all refresh tokens for a user
     *
     * Used for security purposes (e.g., password reset, account compromise).
     *
     * @param userId The user whose tokens should be revoked
     * @return Result indicating success or failure
     */
    suspend fun revokeAllUserTokens(userId: UserId): Result<Unit>

    /**
     * Clean up expired and revoked tokens
     *
     * Should be called periodically to maintain database hygiene.
     *
     * @return Result containing count of deleted tokens
     */
    suspend fun cleanupExpiredTokens(): Result<Int>

    /**
     * Get all active tokens for a user
     *
     * Useful for displaying active sessions to the user.
     *
     * @param userId The user to query
     * @return List of active token information
     */
    suspend fun getUserActiveTokens(userId: UserId): List<RefreshTokenInfo>
}

/**
 * Information about a refresh token
 *
 * @property tokenId The database ID of the token (for display purposes)
 * @property createdAt When this token was created
 * @property expiresAt When this token will expire
 * @property isRevoked Whether this token has been revoked
 */
data class RefreshTokenInfo(
    val tokenId: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val isRevoked: Boolean
)
