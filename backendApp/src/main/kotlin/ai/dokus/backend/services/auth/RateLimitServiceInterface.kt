package ai.dokus.backend.services.auth

/**
 * Interface for rate limiting login attempts to prevent brute force attacks.
 *
 * Implementations can use different storage backends:
 * - InMemoryRateLimitService: Single-instance deployments
 * - RedisRateLimitService: Multi-instance deployments
 */
interface RateLimitServiceInterface {
    /**
     * Checks if a login attempt is allowed for the given email address.
     * This method should be called BEFORE attempting to verify credentials.
     *
     * @param email Email address attempting to log in
     * @return Result.success if login attempt is allowed, Result.failure with TooManyLoginAttempts if blocked
     */
    suspend fun checkLoginAttempts(email: String): Result<Unit>

    /**
     * Records a failed login attempt for the given email address.
     * This method should be called AFTER a failed credential verification.
     *
     * @param email Email address that failed to log in
     */
    suspend fun recordFailedLogin(email: String)

    /**
     * Resets the login attempt counter for the given email address.
     * This method should be called AFTER a successful login.
     *
     * @param email Email address to reset
     */
    suspend fun resetLoginAttempts(email: String)

    /**
     * Removes expired entries from the storage.
     * Should be called periodically to prevent storage leaks.
     */
    suspend fun cleanupExpiredEntries()

    /**
     * Gets the current attempt count for the given email address.
     *
     * @param email Email address to check
     * @return Number of failed attempts (0 if no attempts recorded)
     */
    suspend fun getAttemptCount(email: String): Int

    /**
     * Checks if the given email address is currently locked out.
     *
     * @param email Email address to check
     * @return true if the account is locked, false otherwise
     */
    suspend fun isLocked(email: String): Boolean
}
