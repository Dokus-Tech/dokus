@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.services

import ai.dokus.foundation.database.repository.auth.RefreshTokenRepository
import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.foundation.domain.enums.Permission
import ai.dokus.foundation.domain.enums.SubscriptionTier
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.TenantMembership
import ai.dokus.foundation.domain.model.auth.TenantScope
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.UpdateProfileRequest
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.ktor.database.now
import ai.dokus.foundation.ktor.security.JwtGenerator
import ai.dokus.foundation.ktor.security.TokenBlacklistService
import com.auth0.jwt.JWT
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import org.slf4j.LoggerFactory

class AuthService(
    private val userRepository: UserRepository,
    private val jwtGenerator: JwtGenerator,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val rateLimitService: RateLimitServiceInterface,
    private val emailVerificationService: EmailVerificationService,
    private val passwordResetService: PasswordResetService,
    private val tokenBlacklistService: TokenBlacklistService? = null,
    private val maxConcurrentSessions: Int = DEFAULT_MAX_CONCURRENT_SESSIONS
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    companion object {
        /** Default maximum concurrent sessions per user */
        const val DEFAULT_MAX_CONCURRENT_SESSIONS = 5
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> = try {
        logger.debug("Login attempt for email: ${request.email.value}")

        rateLimitService.checkLoginAttempts(request.email.value).getOrElse { error ->
            logger.warn("Login attempt blocked by rate limiter for email: ${request.email.value}")
            throw error
        }

        val user = userRepository.verifyCredentials(
            email = request.email.value,
            password = request.password.value
        ) ?: run {
            logger.warn("Invalid credentials for email: ${request.email.value}")
            rateLimitService.recordFailedLogin(request.email.value)
            throw DokusException.InvalidCredentials()
        }

        if (!user.isActive) {
            logger.warn("Inactive account login attempt for email: ${request.email.value}")
            throw DokusException.AccountInactive()
        }

        val userId = user.id
        val loginTime = now()
        userRepository.recordLogin(userId, loginTime)

        // Get all user's tenants and create scopes for each
        val memberships = userRepository.getUserTenants(userId)
        val selectedTenant = resolveTenantScope(memberships)

        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            tenant = selectedTenant
        )

        val response = jwtGenerator.generateTokens(claims)

        // Enforce concurrent session limit by revoking oldest session if needed
        val activeSessions = refreshTokenRepository.countActiveForUser(userId)
        if (activeSessions >= maxConcurrentSessions) {
            logger.info("User ${userId.value} at session limit ($activeSessions/$maxConcurrentSessions), revoking oldest session")
            refreshTokenRepository.revokeOldestForUser(userId).onFailure { error ->
                logger.warn("Failed to revoke oldest session for user: ${userId.value}", error)
            }
        }

        refreshTokenRepository.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = (now() + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days)
        ).onFailure { error ->
            logger.error("Failed to save refresh token for user: ${userId.value}", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        rateLimitService.resetLoginAttempts(request.email.value)

        logger.info("Successful login for user: ${user.id} (email: ${user.email.value})")
        Result.success(response)
    } catch (e: DokusException) {
        logger.error("Login failed: ${e.errorCode} for email: ${request.email.value}", e)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Login error for email: ${request.email.value}", e)
        Result.failure(DokusException.InternalError(e.message ?: "Login failed"))
    }

    suspend fun register(request: RegisterRequest): Result<LoginResponse> = try {
        logger.debug("Registration attempt for email: ${request.email.value}")

        // Register user without any tenant
        // User can create or join tenants after registration
        val user = userRepository.register(
            email = request.email.value,
            password = request.password.value,
            firstName = request.firstName.value,
            lastName = request.lastName.value
        )

        val userId = user.id

        // User starts with no tenants - empty scopes
        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            tenant = null
        )

        val response = jwtGenerator.generateTokens(claims)

        refreshTokenRepository.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = (now() + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days)
        ).onFailure { error ->
            logger.error("Failed to save refresh token for user: $userId", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        emailVerificationService.sendVerificationEmail(userId, user.email.value)
            .onFailure { error ->
                logger.warn("Failed to send verification email during registration: ${error.message}")
            }

        logger.info("Successful registration and auto-login for user: ${user.id} (email: ${user.email.value})")
        Result.success(response)
    } catch (e: IllegalArgumentException) {
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

    suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> = try {
        logger.debug("Token refresh attempt")

        val userId = refreshTokenRepository.validateAndRotate(request.refreshToken)
            .getOrElse { error ->
                logger.warn("Token refresh failed: ${error.message}")
                when (error) {
                    is SecurityException -> throw DokusException.RefreshTokenRevoked()
                    is IllegalArgumentException -> throw DokusException.RefreshTokenExpired()
                    else -> throw DokusException.RefreshTokenExpired()
                }
            }

        val user = userRepository.findById(userId)
            ?: run {
                logger.error("User not found for valid refresh token: ${userId.value}")
                throw DokusException.InvalidCredentials("User account no longer exists")
            }

        if (!user.isActive) {
            logger.warn("Token refresh attempt for inactive user: ${userId.value}")
            throw DokusException.AccountInactive()
        }

        // Get all user's tenants and create scopes for each
        val memberships = userRepository.getUserTenants(userId)
        val selectedTenant = resolveTenantScope(
            memberships = memberships,
            selectedTenantId = request.tenantId
        ) ?: if (request.tenantId != null) {
            throw DokusException.NotAuthorized("User is not a member of tenant ${request.tenantId}")
        } else {
            null
        }

        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            tenant = selectedTenant
        )

        val response = jwtGenerator.generateTokens(claims)

        refreshTokenRepository.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = (now() + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days)
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

    suspend fun selectOrganization(
        userId: UserId,
        tenantId: TenantId
    ): Result<LoginResponse> = try {
        logger.debug("Selecting tenant $tenantId for user $userId")

        val user = userRepository.findById(userId)
            ?: throw DokusException.InvalidCredentials("User account no longer exists")

        if (!user.isActive) {
            throw DokusException.AccountInactive()
        }

        val memberships = userRepository.getUserTenants(userId)
        val selectedTenant = resolveTenantScope(
            memberships = memberships,
            selectedTenantId = tenantId
        ) ?: throw DokusException.NotAuthorized("User is not a member of tenant $tenantId")

        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            tenant = selectedTenant
        )

        val response = jwtGenerator.generateTokens(claims)

        // Enforce concurrent session limit by revoking oldest session if needed
        val activeSessions = refreshTokenRepository.countActiveForUser(userId)
        if (activeSessions >= maxConcurrentSessions) {
            logger.info("User ${userId.value} at session limit during tenant selection, revoking oldest session")
            refreshTokenRepository.revokeOldestForUser(userId).onFailure { error ->
                logger.warn("Failed to revoke oldest session for user: ${userId.value}", error)
            }
        }

        refreshTokenRepository.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = (now() + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days)
        ).onFailure { error ->
            logger.error("Failed to save refresh token after tenant selection for user: ${userId.value}", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        logger.info("Tenant selection successful for user: $userId -> $tenantId")
        Result.success(response)
    } catch (e: DokusException) {
        logger.error("Tenant selection failed: ${e.errorCode} for user: ${userId.value}", e)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Unexpected error selecting tenant for user: ${userId.value}", e)
        Result.failure(DokusException.InternalError(e.message ?: "Tenant selection failed"))
    }

    suspend fun logout(request: LogoutRequest): Result<Unit> = try {
        logger.info("Logout request received")

        // Blacklist the access token (sessionToken) to prevent further use
        blacklistAccessToken(request.sessionToken)

        // Revoke the refresh token
        request.refreshToken?.let { token ->
            refreshTokenRepository.revokeToken(token).onFailure { error ->
                logger.warn("Failed to revoke refresh token during logout: ${error.message}")
            }.onSuccess {
                logger.info("Successfully revoked refresh token during logout")
            }
        }

        logger.info("Logout successful")
        Result.success(Unit)
    } catch (e: Exception) {
        logger.warn("Logout error (non-critical, returning success): ${e.message}", e)
        Result.success(Unit)
    }

    /**
     * Blacklist an access token by extracting its JTI and expiration.
     * Non-critical - failures are logged but don't break the flow.
     */
    private suspend fun blacklistAccessToken(accessToken: String) {
        if (tokenBlacklistService == null) {
            logger.debug("Token blacklist service not configured, skipping blacklist")
            return
        }

        try {
            val decoded = JWT.decode(accessToken)
            val jti = decoded.id
            val expiresAt = decoded.expiresAt?.toInstant()

            if (jti != null && expiresAt != null) {
                tokenBlacklistService.blacklistToken(jti, expiresAt)
                logger.debug("Access token blacklisted: ${jti.take(8)}...")
            } else {
                logger.debug("Cannot blacklist token: missing JTI or expiration")
            }
        } catch (e: Exception) {
            logger.warn("Failed to blacklist access token: ${e.message}")
            // Non-critical - logout still succeeds
        }
    }

    suspend fun verifyEmail(token: String): Result<Unit> {
        logger.debug("Email verification attempt with token")
        return emailVerificationService.verifyEmail(token)
    }

    suspend fun resendVerificationEmail(userId: UserId): Result<Unit> {
        logger.debug("Resend verification email for user: {}", userId.value)
        return emailVerificationService.resendVerificationEmail(userId)
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> {
        logger.debug("Password reset requested for email")
        return passwordResetService.requestReset(email)
    }

    suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        logger.debug("Password reset attempt with token")
        return passwordResetService.resetPassword(token, newPassword)
    }

    suspend fun deactivateAccount(userId: UserId, reason: String): Result<Unit> = try {
        logger.info("Account deactivation request for user: ${userId.value}, reason: $reason")

        val user = userRepository.findById(userId)
            ?: run {
                logger.warn("Deactivation failed - user not found: ${userId.value}")
                throw DokusException.InvalidCredentials("User account not found")
            }

        if (!user.isActive) {
            logger.warn("Deactivation attempt for already inactive user: ${userId.value}")
            throw DokusException.AccountInactive("Account is already deactivated")
        }

        userRepository.deactivate(userId, reason)
        logger.info("User account marked as inactive: ${userId.value}")

        // Blacklist all active access tokens for this user
        tokenBlacklistService?.let {
            it.blacklistAllUserTokens(userId)
            logger.info("All access tokens blacklisted for user: ${userId.value}")
        }

        refreshTokenRepository.revokeAllUserTokens(userId)
            .onSuccess {
                logger.info("All refresh tokens revoked for user: ${userId.value}")
            }
            .onFailure { error ->
                logger.warn("Failed to revoke tokens during deactivation for user: ${userId.value}", error)
            }

        logger.info("Account deactivation completed successfully for user: ${userId.value}, reason: $reason")

        Result.success(Unit)
    } catch (e: DokusException) {
        logger.error("Account deactivation failed: ${e.errorCode} for user: ${userId.value}", e)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Account deactivation error for user: ${userId.value}", e)
        Result.failure(DokusException.InternalError(e.message ?: "Account deactivation failed"))
    }

    suspend fun updateProfile(userId: UserId, request: UpdateProfileRequest): Result<User> = try {
        logger.info("Profile update request for user: ${userId.value}")

        val user = userRepository.findById(userId)
            ?: run {
                logger.warn("Profile update failed - user not found: ${userId.value}")
                throw DokusException.NotFound("User account not found")
            }

        if (!user.isActive) {
            logger.warn("Profile update attempt for inactive user: ${userId.value}")
            throw DokusException.AccountInactive("Account is deactivated")
        }

        userRepository.updateProfile(
            userId = userId,
            firstName = request.firstName?.value,
            lastName = request.lastName?.value
        )

        val updatedUser = userRepository.findById(userId)
            ?: throw DokusException.InternalError("Failed to fetch updated user")

        logger.info("Profile updated successfully for user: ${userId.value}")
        Result.success(updatedUser)
    } catch (e: DokusException) {
        logger.error("Profile update failed: ${e.errorCode} for user: ${userId.value}", e)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Profile update error for user: ${userId.value}", e)
        Result.failure(DokusException.InternalError(e.message ?: "Profile update failed"))
    }

    private fun resolveTenantScope(
        memberships: List<TenantMembership>,
        selectedTenantId: TenantId? = null
    ): TenantScope? {
        val activeMemberships = memberships.filter { it.isActive }
        val targetTenantId = when {
            selectedTenantId != null -> selectedTenantId
            activeMemberships.size == 1 -> activeMemberships.first().tenantId
            else -> return null
        }

        val membership = activeMemberships.firstOrNull { it.tenantId == targetTenantId }
            ?: return null

        return createTenantScope(
            tenantId = membership.tenantId,
            role = membership.role
        )
    }

    private fun createTenantScope(
        tenantId: TenantId,
        role: UserRole
    ): TenantScope {
        val permissions = getPermissionsForRole(role)
        val tier = SubscriptionTier.CloudFree // TODO: Get from tenant

        return TenantScope(
            tenantId = tenantId,
            permissions = permissions,
            subscriptionTier = tier,
            role = role
        )
    }

    private fun getPermissionsForRole(role: UserRole): Set<Permission> = when (role) {
        UserRole.Owner -> Permission.entries.toSet()
        UserRole.Admin -> Permission.entries.toSet() - setOf(Permission.UsersManage)
        UserRole.Accountant -> setOf(
            Permission.InvoicesRead, Permission.InvoicesCreate, Permission.InvoicesEdit,
            Permission.InvoicesSend, Permission.ClientsRead, Permission.ClientsManage,
            Permission.ReportsView, Permission.ExportsCreate
        )
        UserRole.Editor -> setOf(
            Permission.InvoicesRead, Permission.InvoicesCreate, Permission.InvoicesEdit,
            Permission.ClientsRead, Permission.ReportsView
        )
        UserRole.Viewer -> setOf(
            Permission.InvoicesRead, Permission.ClientsRead, Permission.ReportsView
        )
    }
}
