@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.database.repository.RefreshTokenRepository
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.Permission
import ai.dokus.foundation.domain.enums.SubscriptionTier
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.auth.OrganizationScope
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.ktor.database.now
import ai.dokus.foundation.ktor.security.JwtGenerator
import ai.dokus.foundation.ktor.services.OrganizationService
import ai.dokus.foundation.ktor.services.UserService
import org.slf4j.LoggerFactory
import ai.dokus.foundation.domain.model.auth.JwtClaims
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi

class AuthService(
    private val userService: UserService,
    private val organizationService: OrganizationService,
    private val jwtGenerator: JwtGenerator,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val rateLimitService: RateLimitService,
    private val emailVerificationService: EmailVerificationService,
    private val passwordResetService: PasswordResetService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun login(request: LoginRequest): Result<LoginResponse> = try {
        logger.debug("Login attempt for email: ${request.email.value}")

        rateLimitService.checkLoginAttempts(request.email.value).getOrElse { error ->
            logger.warn("Login attempt blocked by rate limiter for email: ${request.email.value}")
            throw error
        }

        val user = userService.verifyCredentials(
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

        val userId = UserId(user.id.value.toString())
        val loginTime = now()
        userService.recordLogin(userId, loginTime)

        val organizationScope = createOrganizationScope(
            organizationId = OrganizationId(user.organizationId.value),
            role = user.role
        )

        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            organizations = listOf(organizationScope)
        )

        val response = jwtGenerator.generateTokens(claims)

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

        val tenantName = buildString {
            append(request.firstName.value)
            if (request.firstName.value.isNotEmpty() && request.lastName.value.isNotEmpty()) append(" ")
            append(request.lastName.value)
        }.ifEmpty { request.email.value }

        logger.debug("Creating new tenant: $tenantName for email: ${request.email.value}")

        val tenant = organizationService.createTenant(
            name = tenantName,
            email = request.email.value,
            plan = OrganizationPlan.Free,
            country = "BE",
            language = Language.En,
            vatNumber = null
        )

        logger.info("Created tenant: ${tenant.id} with trial ending at: ${tenant.trialEndsAt}")

        val user = userService.register(
            organizationId = tenant.id,
            email = request.email.value,
            password = request.password.value,
            firstName = request.firstName.value,
            lastName = request.lastName.value,
            role = UserRole.Owner
        )

        val userId = user.id

        val organizationScope = createOrganizationScope(
            organizationId = OrganizationId(tenant.id.value),
            role = user.role
        )

        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            organizations = listOf(organizationScope)
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

        logger.info("Successful registration and auto-login for user: ${user.id} (email: ${user.email.value}), tenant: ${tenant.id}")
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

        val user = userService.findById(userId)
            ?: run {
                logger.error("User not found for valid refresh token: ${userId.value}")
                throw DokusException.InvalidCredentials("User account no longer exists")
            }

        if (!user.isActive) {
            logger.warn("Token refresh attempt for inactive user: ${userId.value}")
            throw DokusException.AccountInactive()
        }

        val organizationScope = createOrganizationScope(
            organizationId = OrganizationId(user.organizationId.value),
            role = user.role
        )

        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            organizations = listOf(organizationScope)
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

    suspend fun logout(request: LogoutRequest): Result<Unit> = try {
        logger.info("Logout request received")

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

        val user = userService.findById(userId)
            ?: run {
                logger.warn("Deactivation failed - user not found: ${userId.value}")
                throw DokusException.InvalidCredentials("User account not found")
            }

        if (!user.isActive) {
            logger.warn("Deactivation attempt for already inactive user: ${userId.value}")
            throw DokusException.AccountInactive("Account is already deactivated")
        }

        userService.deactivate(userId, reason)
        logger.info("User account marked as inactive: ${userId.value}")

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

    private fun createOrganizationScope(
        organizationId: OrganizationId,
        role: UserRole
    ): OrganizationScope {
        val permissions = getPermissionsForRole(role)
        val tier = SubscriptionTier.CloudFree // TODO: Get from tenant/organization

        return OrganizationScope(
            organizationId = organizationId,
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
