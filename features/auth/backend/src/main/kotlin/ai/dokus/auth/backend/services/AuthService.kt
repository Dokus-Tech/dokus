@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.database.services.RefreshTokenService
import ai.dokus.auth.backend.security.JwtGenerator
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.ktor.services.TenantService
import ai.dokus.foundation.ktor.services.UserService
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.time.ExperimentalTime

/**
 * Business logic layer for authentication operations.
 * Orchestrates user verification, registration, token generation, and session management.
 */
class AuthService(
    private val userService: UserService,
    private val tenantService: TenantService,
    private val jwtGenerator: JwtGenerator,
    private val refreshTokenService: RefreshTokenService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * Authenticates a user with email and password.
     * Verifies credentials, records login time, and generates JWT tokens.
     *
     * @param request Login request containing email, password, and remember me flag
     * @return Result with LoginResponse containing tokens on success, or error on failure
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse> = try {
        logger.debug("Login attempt for email: ${request.email.value}")

        // Verify user credentials
        val user = userService.verifyCredentials(
            email = request.email.value,
            password = request.password.value
        ) ?: run {
            logger.warn("Invalid credentials for email: ${request.email.value}")
            throw DokusException.InvalidCredentials()
        }

        // Check if account is active
        if (!user.isActive) {
            logger.warn("Inactive account login attempt for email: ${request.email.value}")
            throw DokusException.AccountInactive()
        }

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

        // Save refresh token to database
        refreshTokenService.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = Clock.System.now() + 30.days
        ).onFailure { error ->
            logger.error("Failed to save refresh token for user: ${userId.value}", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        logger.info("Successful login for user: ${user.id} (email: ${user.email.value})")
        Result.success(response)
    } catch (e: DokusException) {
        logger.error("Login failed: ${e.errorCode} for email: ${request.email.value}", e)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Login error for email: ${request.email.value}", e)
        Result.failure(DokusException.InternalError(e.message ?: "Login failed"))
    }

    /**
     * Registers a new user account and automatically logs them in.
     * Creates a new tenant for the user, generates tokens, and returns authentication response.
     * The first user becomes the tenant owner with a Trial plan.
     *
     * @param request Registration request with email, password, and name information
     * @return Result with LoginResponse containing tokens on success, or error on failure
     */
    suspend fun register(request: RegisterRequest): Result<LoginResponse> = try {
        logger.debug("Registration attempt for email: ${request.email.value}")

        // Create tenant for the new user
        // Tenant name defaults to user's full name (can be changed later in settings)
        val tenantName = buildString {
            append(request.firstName.value)
            if (request.firstName.value.isNotEmpty() && request.lastName.value.isNotEmpty()) append(" ")
            append(request.lastName.value)
        }.ifEmpty { request.email.value }

        logger.debug("Creating new tenant: $tenantName for email: ${request.email.value}")

        val tenant = tenantService.createTenant(
            name = tenantName,
            email = request.email.value,
            plan = TenantPlan.Free,
            country = "BE", // TODO: Get from request or use IP-based detection
            language = Language.En, // TODO: Get from request or use browser locale
            vatNumber = null // User can add VAT number later in settings
        )

        logger.info("Created tenant: ${tenant.id} with trial ending at: ${tenant.trialEndsAt}")

        // Register new user with Owner role for the newly created tenant
        val user = userService.register(
            tenantId = tenant.id,
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

        // Save refresh token to database
        refreshTokenService.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = Clock.System.now() + 30.days
        ).onFailure { error ->
            logger.error("Failed to save refresh token for user: ${userId.value}", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        logger.info("Successful registration and auto-login for user: ${user.id} (email: ${user.email.value}), tenant: ${tenant.id}")
        Result.success(response)
    } catch (e: IllegalArgumentException) {
        // User already exists - map to proper DokusException
        logger.warn("Registration failed for email: ${request.email.value} - ${e.message}")
        if (e.message?.contains("already exists") == true) {
            Result.failure(DokusException.UserAlreadyExists())
        } else {
            Result.failure(DokusException.InternalError(e.message ?: "Registration failed"))
        }
    } catch (e: DokusException) {
        logger.error("Registration failed: ${e.errorCode} for email: ${request.email.value}", e)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Registration error for email: ${request.email.value}", e)
        Result.failure(DokusException.InternalError(e.message ?: "Registration failed"))
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * Validates the refresh token, rotates it, and generates new tokens.
     *
     * @param request Refresh token request containing the current refresh token
     * @return Result with new LoginResponse containing fresh tokens
     */
    suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> = try {
        logger.debug("Token refresh attempt")

        // Validate and rotate token (old token is automatically revoked)
        val userId = refreshTokenService.validateAndRotate(request.refreshToken)
            .getOrElse { error ->
                logger.warn("Token refresh failed: ${error.message}")
                when (error) {
                    is SecurityException -> throw DokusException.RefreshTokenRevoked()
                    is IllegalArgumentException -> throw DokusException.RefreshTokenExpired()
                    else -> throw DokusException.RefreshTokenExpired()
                }
            }

        // Get user details for token generation
        val user = userService.findById(userId)
            ?: run {
                logger.error("User not found for valid refresh token: ${userId.value}")
                throw DokusException.InvalidCredentials("User account no longer exists")
            }

        // Check if account is still active
        if (!user.isActive) {
            logger.warn("Token refresh attempt for inactive user: ${userId.value}")
            throw DokusException.AccountInactive()
        }

        // Generate full name for JWT claims
        val fullName = buildString {
            user.firstName?.let { append(it) }
            if (user.firstName != null && user.lastName != null) append(" ")
            user.lastName?.let { append(it) }
        }.ifEmpty { user.email.value }

        // Generate new access and refresh tokens
        val response = jwtGenerator.generateTokens(
            userId = userId,
            email = user.email.value,
            fullName = fullName,
            tenantId = user.tenantId,
            roles = setOf(user.role.dbValue)
        )

        // Save the new refresh token (rotated)
        refreshTokenService.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = Clock.System.now() + 30.days
        ).onFailure { error ->
            logger.error("Failed to save rotated refresh token for user: ${userId.value}", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        logger.info("Successfully refreshed tokens for user: ${userId.value}")
        Result.success(response)
    } catch (e: DokusException) {
        logger.error("Token refresh failed: ${e.errorCode}", e)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Token refresh error", e)
        Result.failure(DokusException.RefreshTokenExpired())
    }

    /**
     * Logs out a user and invalidates their current session.
     * Revokes the refresh token to prevent future token refreshes.
     *
     * Note: Always returns success even if token revocation fails, as the client
     * will have cleared tokens locally. This prevents blocking the logout flow.
     *
     * @param request Logout request containing session token and optional refresh token
     * @return Result indicating success (always succeeds)
     */
    suspend fun logout(request: LogoutRequest): Result<Unit> = try {
        logger.info("Logout request received")

        // Revoke refresh token if provided
        request.refreshToken?.let { token ->
            refreshTokenService.revokeToken(token).onFailure { error ->
                // Log but don't fail - token may already be revoked or expired
                logger.warn("Failed to revoke refresh token during logout: ${error.message}")
            }.onSuccess {
                logger.info("Successfully revoked refresh token during logout")
            }
        }

        // TODO: Consider adding access token to blacklist/revocation cache
        // For now, access tokens are short-lived (1 hour) so they'll expire naturally

        logger.info("Logout successful")
        Result.success(Unit)
    } catch (e: Exception) {
        // Never fail logout - client has already cleared tokens
        logger.warn("Logout error (non-critical, returning success): ${e.message}", e)
        Result.success(Unit)
    }
}
