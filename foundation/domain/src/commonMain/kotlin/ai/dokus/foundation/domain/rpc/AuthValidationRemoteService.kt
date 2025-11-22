package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.enums.UserStatus
import ai.dokus.foundation.domain.model.UserDto
import kotlinx.datetime.Instant
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Auth Validation Remote Service
 *
 * This RPC interface is used by other microservices (Cashflow, Payment, Reporting, etc.)
 * to validate JWT tokens against the Auth service.
 *
 * The Auth service implements this interface and other services call it via RPC
 * to validate user sessions and extract user context.
 *
 * Usage in other services:
 * ```kotlin
 * withUser(validationService) { user ->
 *     // user: UserDto.Full with complete user data
 *     // user.organizationId is available for multi-tenant queries
 * }
 * ```
 */
@Rpc
interface AuthValidationRemoteService {

    /**
     * Validate a JWT session and return complete user data
     *
     * This method performs comprehensive validation:
     * 1. Verifies JWT signature with shared secret
     * 2. Checks token is not blacklisted (revoked)
     * 3. Validates session is active and not expired
     * 4. Retrieves full user data from database
     * 5. Validates user has required roles (if specified)
     *
     * @param token JWT access token (without "Bearer " prefix)
     * @param requestContext Context information about the requesting service
     * @param allowedUserRoles User roles that are allowed (default: all roles)
     * @return Full user data if validation succeeds
     * @throws Unauthorized if token is invalid, expired, or blacklisted
     * @throws SessionExpired if session has expired
     * @throws AccountNotVerified if user account is not verified (when required)
     * @throws UserLocked if user account is locked
     */
    suspend fun validateSession(
        token: String,
        requestContext: Context,
        allowedUserRoles: List<UserRole> = UserRole.all,
    ): UserDto.Full

    /**
     * Get user by ID.
     * Used by withUser() to fetch full user details after local JWT validation.
     *
     * @param userId The user ID to fetch
     * @return Full user data
     * @throws NotFound if user doesn't exist
     */
    suspend fun getUserById(userId: UserId): UserDto.Full

    /**
     * Context information about the request
     * Used for audit logging and security tracking
     */
    @Serializable
    data class Context @OptIn(ExperimentalTime::class) constructor(
        val sourceModule: String,          // e.g., "Cashflow", "Payment", "Reporting"
        val ipAddress: String,             // Client IP address
        val userAgent: String? = null,     // Client user agent
        val timestamp: Instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
    )
}
