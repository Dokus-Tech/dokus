@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.security.JwtValidator
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.enums.UserStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.UserDto
import ai.dokus.foundation.domain.rpc.AuthValidationRemoteService
import ai.dokus.foundation.ktor.services.UserService
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

/**
 * RPC implementation of AuthValidationRemoteService.
 *
 * This service validates JWT tokens for other microservices and returns complete user data.
 * It acts as the central authentication authority that all other services call via RPC.
 *
 * Flow:
 * 1. Validate JWT token signature and expiration
 * 2. Extract user ID from token
 * 3. Fetch full user data from database
 * 4. Verify user status and roles match allowed criteria
 * 5. Return UserDto.Full with tenant context
 *
 * Error Handling:
 * - Throws DokusException.Unauthorized if token is invalid
 * - Throws DokusException.Forbidden if user doesn't meet access criteria
 */
class AuthValidationRemoteServiceImpl(
    private val jwtValidator: JwtValidator,
    private val userService: UserService
) : AuthValidationRemoteService {

    private val logger = LoggerFactory.getLogger(AuthValidationRemoteServiceImpl::class.java)

    /**
     * Validate JWT session and return complete user data.
     *
     * This is called by other microservices (Cashflow, Payment, Reporting, etc.)
     * to validate incoming requests and obtain authenticated user context.
     *
     * @param token JWT access token (without "Bearer " prefix)
     * @param requestContext Context about the requesting service (for audit logging)
     * @param allowedUserRoles User roles that are allowed (default: all roles)
     * @return UserDto.Full with complete user data including tenant context
     * @throws DokusException.Unauthorized if token is invalid or expired
     * @throws DokusException.Forbidden if user role doesn't match criteria
     */
    override suspend fun validateSession(
        token: String,
        requestContext: AuthValidationRemoteService.Context,
        allowedUserRoles: List<UserRole>
    ): UserDto.Full {
        logger.debug(
            "Validating session for module: ${requestContext.sourceModule}, " +
            "IP: ${requestContext.ipAddress}"
        )

        // Step 1: Validate JWT signature and extract claims
        val authInfo = jwtValidator.validateAndExtract(token)
            ?: run {
                logger.warn(
                    "Invalid JWT token from ${requestContext.sourceModule} " +
                    "(IP: ${requestContext.ipAddress})"
                )
                throw DokusException.TokenInvalid("Invalid or expired token")
            }

        logger.debug("JWT validated for user: ${authInfo.userId}, tenant: ${authInfo.tenantId}")

        // Step 2: Fetch full user data from database
        val user = userService.findById(authInfo.userId)
            ?: run {
                logger.error("User not found in database: ${authInfo.userId}")
                throw DokusException.NotAuthenticated("User not found")
            }

        // Step 3: Check if user account is active
        if (!user.isActive) {
            logger.warn("Inactive user attempted access: ${authInfo.userId}")
            throw DokusException.AccountInactive("Account is not active")
        }

        // Step 4: Verify user role matches allowed roles
        if (!allowedUserRoles.contains(user.role)) {
            logger.warn(
                "User ${authInfo.userId} has role ${user.role}, " +
                "but only $allowedUserRoles are allowed"
            )
            throw DokusException.NotAuthorized(
                "User role '${user.role}' is not allowed for this operation"
            )
        }

        logger.info(
            "Session validated successfully for user ${authInfo.userId} " +
            "from ${requestContext.sourceModule}"
        )

        // Step 5: Return complete user DTO with tenant context
        return UserDto.Full(
            id = user.id,
            tenantId = user.tenantId,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role,
            emailVerified = user.emailVerified,
            isActive = user.isActive,
            lastLoginAt = user.lastLoginAt,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}
