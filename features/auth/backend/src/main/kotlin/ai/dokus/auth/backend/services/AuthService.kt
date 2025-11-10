package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.security.JwtGenerator
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.ktor.services.UserService
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Business logic layer for authentication operations.
 * Orchestrates user verification, registration, token generation, and session management.
 */
class AuthService(
    private val userService: UserService,
    private val jwtGenerator: JwtGenerator
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * Authenticates a user with email and password.
     * Verifies credentials, records login time, and generates JWT tokens.
     *
     * @param request Login request containing email, password, and remember me flag
     * @return Result with LoginResponse containing tokens on success, or error on failure
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun login(request: LoginRequest): Result<LoginResponse> = try {
        logger.debug("Login attempt for email: ${request.email.value}")

        // Verify user credentials
        val user = userService.verifyCredentials(
            email = request.email.value,
            password = request.password.value
        )

        if (user == null) {
            logger.warn("Invalid credentials for email: ${request.email.value}")
            Result.failure(Exception("Invalid email or password"))
        } else {
            // Record successful login
            val userId = UserId(user.id.value.toString())
            val loginTime = Clock.System.now()
            userService.recordLogin(userId, loginTime)

            // Generate full name for JWT claims
            val fullName = buildString {
                user.firstName?.let { append(it) }
                if (user.firstName != null && user.lastName != null) append(" ")
                user.lastName?.let { append(it) }
            }.ifEmpty { user.email.value }

            // Generate JWT tokens
            val response = jwtGenerator.generateTokens(
                userId = userId,
                email = user.email.value,
                fullName = fullName,
                tenantId = user.tenantId,
                roles = setOf(user.role.dbValue)
            )

            logger.info("Successful login for user: ${user.id} (email: ${user.email.value})")
            Result.success(response)
        }
    } catch (e: Exception) {
        logger.error("Login error for email: ${request.email.value}", e)
        Result.failure(Exception("Login failed: ${e.message}", e))
    }

    /**
     * Registers a new user account and automatically logs them in.
     * Creates the user, generates tokens, and returns authentication response.
     *
     * @param request Registration request with email, password, and name information
     * @return Result with LoginResponse containing tokens on success, or error on failure
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun register(request: RegisterRequest): Result<LoginResponse> = try {
        logger.debug("Registration attempt for email: ${request.email.value}")

        // FIXME: Implement proper tenant creation flow
        // Currently using a temporary default tenant ID
        // This should be replaced with actual tenant creation logic
        val temporaryTenantId = TenantId(Uuid.parse("00000000-0000-0000-0000-000000000001"))

        // Register new user with Owner role
        val user = userService.register(
            tenantId = temporaryTenantId,
            email = request.email.value,
            password = request.password.value,
            firstName = request.firstName.value,
            lastName = request.lastName.value,
            role = UserRole.Owner
        )

        // Generate full name for JWT claims
        val fullName = buildString {
            user.firstName?.let { append(it) }
            if (user.firstName != null && user.lastName != null) append(" ")
            user.lastName?.let { append(it) }
        }.ifEmpty { user.email.value }

        // Auto-login: generate JWT tokens
        val userId = UserId(user.id.value.toString())
        val response = jwtGenerator.generateTokens(
            userId = userId,
            email = user.email.value,
            fullName = fullName,
            tenantId = user.tenantId,
            roles = setOf(user.role.dbValue)
        )

        logger.info("Successful registration and auto-login for user: ${user.id} (email: ${user.email.value})")
        Result.success(response)
    } catch (e: IllegalArgumentException) {
        // User already exists or validation error
        logger.warn("Registration failed for email: ${request.email.value} - ${e.message}")
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Registration error for email: ${request.email.value}", e)
        Result.failure(Exception("Registration failed: ${e.message}", e))
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     *
     * @param request Refresh token request
     * @return Result with new LoginResponse containing fresh tokens
     */
    suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> {
        logger.debug("Token refresh attempt")
        // TODO: Implement token refresh logic
        // - Verify refresh token validity
        // - Extract user information from refresh token
        // - Generate new access token (and optionally new refresh token)
        // - Return new token pair
        return Result.failure(NotImplementedError("Token refresh not yet implemented"))
    }

    /**
     * Logs out a user and invalidates their current session.
     * Currently only logs the event; token revocation is planned for future implementation.
     *
     * @param request Logout request containing session token
     * @return Result indicating success or failure
     */
    suspend fun logout(request: LogoutRequest): Result<Unit> = try {
        logger.info("Logout request for session token: ${request.sessionToken}")
        // TODO: Implement token revocation
        // - Add token to revocation list/blacklist
        // - Invalidate refresh token if applicable
        // - Clear any cached session data
        logger.info("Logout successful (token revocation not yet implemented)")
        Result.success(Unit)
    } catch (e: Exception) {
        logger.error("Logout error", e)
        Result.failure(Exception("Logout failed: ${e.message}", e))
    }
}
