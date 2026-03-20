@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.services.auth

import com.auth0.jwt.JWT
import kotlinx.datetime.Instant
import tech.dokus.backend.services.avatar.projectUserAvatar
import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.RefreshTokenRepository
import tech.dokus.database.repository.auth.RevokedSessionInfo
import tech.dokus.database.repository.auth.SessionRevocationResult
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.DeviceType
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.FirmMembership
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.User
import tech.dokus.backend.services.avatar.buildVersionedAvatarThumbnail
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.JwtFirmMembershipClaim
import tech.dokus.domain.model.auth.JwtTenantMembershipClaim
import tech.dokus.domain.model.auth.LoginRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.RefreshTokenRequest
import tech.dokus.domain.model.auth.RegisterRequest
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.domain.model.auth.TenantWorkspaceSummary
import tech.dokus.domain.model.auth.UpdateProfileRequest
import tech.dokus.foundation.backend.database.now
import tech.dokus.foundation.backend.security.JwtGenerator
import tech.dokus.foundation.backend.security.TokenBlacklistService
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import java.time.Instant as JavaInstant

data class SessionContext(
    val deviceType: DeviceType = DeviceType.Desktop,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

class AuthService(
    private val userRepository: UserRepository,
    private val firmRepository: FirmRepository,
    private val tenantRepository: TenantRepository,
    private val jwtGenerator: JwtGenerator,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val rateLimitService: RateLimitServiceInterface,
    private val welcomeEmailService: WelcomeEmailService,
    private val emailVerificationService: EmailVerificationService,
    private val passwordResetService: PasswordResetService,
    private val tokenBlacklistService: TokenBlacklistService? = null,
    private val maxConcurrentSessions: Int = DEFAULT_MAX_CONCURRENT_SESSIONS
) {
    private val logger = loggerFor()

    companion object {
        /** Default maximum concurrent sessions per user */
        const val DEFAULT_MAX_CONCURRENT_SESSIONS = 5
    }

    suspend fun getAccountMe(userId: tech.dokus.domain.ids.UserId): AccountMeResponse {
        val user = userRepository.findById(userId)
            ?: throw DokusException.NotAuthenticated("User not found")
        val projectedUser = userRepository.projectUserAvatar(user)
        val tenantMemberships = userRepository.getUserTenants(userId)
            .filter { it.isActive }
        val firmsMemberships = firmRepository.listUserMemberships(userId)
            .filter { it.isActive }
        val surface = SurfaceResolver.resolve(
            tenantMemberships = tenantMemberships,
            firmMemberships = firmsMemberships
        )

        val tenantsById = tenantRepository.findByIds(tenantMemberships.map { it.tenantId })
            .associateBy { it.id }
        val tenantSummaries = buildList {
            for (membership in tenantMemberships) {
                val tenant = tenantsById[membership.tenantId] ?: continue
                add(
                    TenantWorkspaceSummary(
                        id = tenant.id,
                        name = tenant.displayName,
                        vatNumber = tenant.vatNumber,
                        role = membership.role,
                        type = tenant.type,
                        avatar = tenantRepository.getAvatarStorageKey(tenant.id)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { storageKey ->
                                buildVersionedAvatarThumbnail(
                                    basePath = "/api/v1/tenants/${ tenant.id }/avatar",
                                    storageKey = storageKey
                                )
                            }
                    )
                )
            }
        }

        val firmsById = firmRepository.listFirmsByIds(firmsMemberships.map { it.firmId })
            .associateBy { it.id }
        val clientCountByFirmId = firmRepository.countActiveClientsByFirmIds(
            firmsMemberships.map { it.firmId }
        )
        val firmSummaries = firmsMemberships.mapNotNull { membership ->
            val firm = firmsById[membership.firmId] ?: return@mapNotNull null
            FirmWorkspaceSummary(
                id = firm.id,
                name = firm.name,
                vatNumber = firm.vatNumber,
                role = membership.role,
                clientCount = clientCountByFirmId[membership.firmId] ?: 0
            )
        }

        return AccountMeResponse(
            user = projectedUser,
            surface = surface,
            tenants = tenantSummaries,
            firms = firmSummaries
        )
    }

    suspend fun login(
        request: LoginRequest,
        sessionContext: SessionContext = SessionContext(
            deviceType = request.deviceType ?: DeviceType.Desktop
        )
    ): Result<LoginResponse> = try {
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
        val sessionId = SessionId.generate()

        val memberships = userRepository.getUserTenants(userId)
        val selectedTenantId = resolveDefaultTenantId(memberships)

        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            tenantMemberships = memberships.toJwtTenantClaims(),
            firmMemberships = firmRepository.listUserMemberships(userId).toJwtFirmClaims(),
            sessionId = sessionId
        )

        val response = jwtGenerator.generateTokens(claims).copy(
            selectedTenantId = selectedTenantId
        )

        // Enforce concurrent session limit by revoking oldest session if needed
        val activeSessions = refreshTokenRepository.countActiveForUser(userId)
        if (activeSessions >= maxConcurrentSessions) {
            logger.info(
                "User ${userId.value} at session limit " +
                    "($activeSessions/$maxConcurrentSessions), revoking oldest session"
            )
            refreshTokenRepository.revokeOldestForUser(userId).onFailure { error ->
                logger.warn("Failed to revoke oldest session for user: ${userId.value}", error)
            }
        }

        refreshTokenRepository.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = (now() + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days),
            sessionId = sessionId,
            accessTokenJti = claims.jti,
            accessTokenExpiresAt = Instant.fromEpochSeconds(claims.exp),
            deviceType = sessionContext.deviceType,
            ipAddress = sessionContext.ipAddress,
            userAgent = sessionContext.userAgent
        ).onFailure { error ->
            logger.error("Failed to save refresh token for user: ${userId.value}", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        trackAccessToken(userId, claims)

        rateLimitService.resetLoginAttempts(request.email.value)

        val firstSignIn = userRepository.recordSuccessfulLogin(userId, loginTime)
        if (firstSignIn && selectedTenantId != null) {
            welcomeEmailService.scheduleIfEligible(userId, selectedTenantId)
                .onFailure { error ->
                    logger.warn(
                        "Failed to schedule welcome email after first sign-in for user {}",
                        userId,
                        error
                    )
                }
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

    suspend fun register(
        request: RegisterRequest,
        sessionContext: SessionContext = SessionContext(
            deviceType = request.deviceType ?: DeviceType.Desktop
        )
    ): Result<LoginResponse> = try {
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
        val sessionId = SessionId.generate()

        // User starts with no tenants
        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            tenantMemberships = emptyList(),
            firmMemberships = emptyList(),
            sessionId = sessionId
        )

        val response = jwtGenerator.generateTokens(claims).copy(
            selectedTenantId = null
        )

        refreshTokenRepository.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = (now() + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days),
            sessionId = sessionId,
            accessTokenJti = claims.jti,
            accessTokenExpiresAt = Instant.fromEpochSeconds(claims.exp),
            deviceType = sessionContext.deviceType,
            ipAddress = sessionContext.ipAddress,
            userAgent = sessionContext.userAgent
        ).onFailure { error ->
            logger.error("Failed to save refresh token for user: $userId", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        trackAccessToken(userId, claims)

        // Registration auto-logs in the user, so treat it as first successful sign-in.
        userRepository.recordSuccessfulLogin(userId, now())
        emailVerificationService.sendVerificationEmail(userId, user.email.value)
            .onFailure { error ->
                logger.warn("Failed to send verification email after registration for user {}", userId, error)
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

    suspend fun refreshToken(
        request: RefreshTokenRequest,
        sessionContext: SessionContext = SessionContext(
            deviceType = request.deviceType ?: DeviceType.Desktop
        )
    ): Result<LoginResponse> = try {
        logger.debug("Token refresh attempt")

        val validatedRefreshToken = refreshTokenRepository.validateAndRotate(request.refreshToken)
            .getOrElse { error ->
                logger.warn("Token refresh failed: ${error.message}")
                when (error) {
                    is SecurityException -> throw DokusException.RefreshTokenRevoked()
                    is IllegalArgumentException -> throw DokusException.RefreshTokenExpired()
                    else -> throw DokusException.RefreshTokenExpired()
                }
            }
        val userId = validatedRefreshToken.userId

        val user = userRepository.findById(userId)
            ?: run {
                logger.error("User not found for valid refresh token: ${userId.value}")
                throw DokusException.InvalidCredentials("User account no longer exists")
            }

        if (!user.isActive) {
            logger.warn("Token refresh attempt for inactive user: ${userId.value}")
            throw DokusException.AccountInactive()
        }

        val memberships = userRepository.getUserTenants(userId)
        val selectedTenantId = resolveDefaultTenantId(
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
            tenantMemberships = memberships.toJwtTenantClaims(),
            firmMemberships = firmRepository.listUserMemberships(userId).toJwtFirmClaims(),
            sessionId = validatedRefreshToken.sessionId
        )

        val response = jwtGenerator.generateTokens(claims).copy(
            selectedTenantId = selectedTenantId
        )

        refreshTokenRepository.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = (now() + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days),
            sessionId = validatedRefreshToken.sessionId,
            accessTokenJti = claims.jti,
            accessTokenExpiresAt = Instant.fromEpochSeconds(claims.exp),
            deviceType = sessionContext.deviceType,
            ipAddress = sessionContext.ipAddress,
            userAgent = sessionContext.userAgent
        ).onFailure { error ->
            logger.error("Failed to save rotated refresh token for user: ${userId.value}", error)
            throw DokusException.InternalError("Failed to save refresh token")
        }

        trackAccessToken(userId, claims)

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
        tenantId: TenantId,
        currentSessionId: SessionId?,
        sessionContext: SessionContext = SessionContext()
    ): Result<LoginResponse> = try {
        logger.debug("Selecting tenant $tenantId for user $userId")

        val user = userRepository.findById(userId)
            ?: throw DokusException.InvalidCredentials("User account no longer exists")

        if (!user.isActive) {
            throw DokusException.AccountInactive()
        }

        val memberships = userRepository.getUserTenants(userId)
        val selectedTenantId = resolveDefaultTenantId(
            memberships = memberships,
            selectedTenantId = tenantId
        ) ?: throw DokusException.NotAuthorized("User is not a member of tenant $tenantId")
        val activeSessionId = currentSessionId
            ?: throw DokusException.SessionInvalid("Current session identity is missing")

        val claims = jwtGenerator.generateClaims(
            userId = userId,
            email = user.email.value,
            tenantMemberships = memberships.toJwtTenantClaims(),
            firmMemberships = firmRepository.listUserMemberships(userId).toJwtFirmClaims(),
            sessionId = activeSessionId
        )

        val response = jwtGenerator.generateTokens(claims).copy(
            selectedTenantId = selectedTenantId
        )

        refreshTokenRepository.saveRefreshToken(
            userId = userId,
            token = response.refreshToken,
            expiresAt = (now() + JwtClaims.REFRESH_TOKEN_EXPIRY_DAYS.days),
            sessionId = activeSessionId,
            accessTokenJti = claims.jti,
            accessTokenExpiresAt = Instant.fromEpochSeconds(claims.exp),
            deviceType = sessionContext.deviceType,
            ipAddress = sessionContext.ipAddress,
            userAgent = sessionContext.userAgent,
            replaceExistingSessionIdentity = currentSessionId
        ).onFailure { error ->
            logger.error(
                "Failed to save refresh token after tenant selection for user: ${userId.value}",
                error
            )
            throw DokusException.InternalError("Failed to save refresh token")
        }

        trackAccessToken(userId, claims)

        val shouldScheduleWelcome = userRepository.hasFirstSignIn(userId) &&
            !userRepository.hasWelcomeEmailSent(userId)
        if (shouldScheduleWelcome) {
            welcomeEmailService.scheduleIfEligible(userId, tenantId)
                .onFailure { error ->
                    logger.warn(
                        "Failed to schedule welcome email after tenant selection for user {}",
                        userId,
                        error
                    )
                }
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

    suspend fun logout(
        userId: UserId,
        currentSessionId: SessionId?,
        request: LogoutRequest
    ): Result<Unit> = try {
        logger.info("Logout request received")

        // Blacklist the access token (sessionToken) to prevent further use
        blacklistAccessToken(request.sessionToken)

        currentSessionId?.let { sessionId ->
            try {
                when (val result = refreshTokenRepository.revokeCurrentSession(userId, sessionId)) {
                    is SessionRevocationResult.NotFound ->
                        logger.warn("Current session not found during logout for user {}", userId.value)
                    is SessionRevocationResult.Revoked -> {
                        result.sessions.forEach { blacklistRevokedSession(it) }
                        logger.info(
                            "Successfully revoked {} session rows during logout for user {}",
                            result.sessions.size,
                            userId.value
                        )
                    }
                }
            } catch (error: Exception) {
                logger.warn("Failed to revoke current session during logout: ${error.message}")
            }
        } ?: request.refreshToken?.let { token ->
            refreshTokenRepository.revokeToken(token)
                .onFailure { error ->
                    logger.warn("Failed to revoke refresh token during logout: ${error.message}")
                }
                .onSuccess {
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

    private suspend fun trackAccessToken(userId: UserId, claims: JwtClaims) {
        tokenBlacklistService?.let { blacklist ->
            runSuspendCatching {
                blacklist.trackUserToken(
                    userId = userId,
                    jti = claims.jti,
                    expiresAt = JavaInstant.ofEpochSecond(claims.exp)
                )
            }.onFailure { error ->
                logger.warn("Failed to track access token for user {}", userId.value, error)
            }
        }
    }

    private suspend fun blacklistRevokedSession(revoked: RevokedSessionInfo) {
        val jti = revoked.accessTokenJti ?: return
        val expiresAt = revoked.accessTokenExpiresAt ?: return
        tokenBlacklistService?.let { blacklist ->
            runSuspendCatching {
                blacklist.blacklistToken(
                    jti = jti,
                    expiresAt = JavaInstant.ofEpochSecond(
                        expiresAt.epochSeconds,
                        expiresAt.nanosecondsOfSecond.toLong()
                    )
                )
            }.onFailure { error ->
                logger.warn("Failed to blacklist revoked access token {}", jti, error)
            }
        }
    }

    suspend fun verifyEmail(token: String): Result<Unit> {
        logger.debug("Email verification attempt with token")
        return emailVerificationService.verifyEmail(token)
    }

    suspend fun resendVerificationEmail(userId: UserId): Result<Unit> {
        logger.info("Email verification resend requested for user {}", userId.value)

        val rateLimitKey = "email-resend:${userId.value}"
        rateLimitService.checkLoginAttempts(rateLimitKey).getOrElse { error ->
            logger.warn("Email resend blocked by rate limiter for user: {}", userId.value)
            throw error
        }

        return emailVerificationService.resendVerificationEmail(userId).also { result ->
            if (result.isFailure) {
                rateLimitService.recordFailedLogin(rateLimitKey)
            }
        }
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> {
        logger.debug("Password reset requested for email")
        return passwordResetService.requestReset(email)
    }

    suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        logger.debug("Password reset attempt with token")
        return passwordResetService.resetPassword(token, newPassword)
    }

    suspend fun changePassword(
        userId: UserId,
        currentPassword: Password,
        newPassword: Password,
        currentSessionId: SessionId?
    ): Result<Unit> = try {
        logger.info("Change password request for user {}", userId.value)

        val rateLimitKey = "pwd-change:${userId.value}"
        rateLimitService.checkLoginAttempts(rateLimitKey).getOrElse { error ->
            logger.warn("Password change blocked by rate limiter for user: {}", userId.value)
            throw error
        }

        val user = userRepository.findById(userId)
            ?: throw DokusException.NotFound("User account not found")

        if (!user.isActive) {
            throw DokusException.AccountInactive()
        }

        val verified = userRepository.verifyCredentials(user.email.value, currentPassword.value)
        if (verified?.id != userId) {
            rateLimitService.recordFailedLogin(rateLimitKey)
            throw DokusException.InvalidCredentials("Current password is incorrect")
        }

        val activeSessionId = currentSessionId
            ?: throw DokusException.SessionInvalid("Current session identity is missing")

        newPassword.validOrThrows

        // Password update and session revocation are separate transactions.
        // Update password first - if revocation fails, user can manually revoke via sessions UI.
        userRepository.updatePassword(userId, newPassword.value)

        val result = refreshTokenRepository.revokeOtherSessions(userId, activeSessionId)
        if (result is SessionRevocationResult.Revoked) {
            result.sessions.forEach { blacklistRevokedSession(it) }
            logger.info("Password changed and {} sessions revoked for user {}", result.sessions.size, userId.value)
        }

        rateLimitService.resetLoginAttempts(rateLimitKey)
        Result.success(Unit)
    } catch (e: DokusException) {
        logger.warn("Change password failed: {} for user {}", e.errorCode, userId.value)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Change password error for user {}", userId.value, e)
        Result.failure(DokusException.InternalError(e.message ?: "Failed to change password"))
    }

    suspend fun listSessions(
        userId: UserId,
        currentSessionId: SessionId?
    ): Result<List<SessionDto>> = try {
        Result.success(refreshTokenRepository.listActiveSessions(userId, currentSessionId))
    } catch (e: Exception) {
        logger.error("Failed to list sessions for user {}", userId.value, e)
        Result.failure(DokusException.InternalError("Failed to list sessions"))
    }

    suspend fun revokeSession(
        userId: UserId,
        sessionId: SessionId,
        currentSessionId: SessionId? = null
    ): Result<Unit> = try {
        if (currentSessionId != null && sessionId == currentSessionId) {
            throw DokusException.BadRequest("Cannot revoke current session. Use logout instead.")
        }
        when (val result = refreshTokenRepository.revokeSessionById(userId, sessionId)) {
            is SessionRevocationResult.NotFound ->
                throw DokusException.NotFound("Session not found")
            is SessionRevocationResult.Revoked ->
                result.sessions.forEach { blacklistRevokedSession(it) }
        }
        Result.success(Unit)
    } catch (e: DokusException) {
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Failed to revoke session {} for user {}", sessionId.value, userId.value, e)
        Result.failure(DokusException.InternalError("Failed to revoke session"))
    }

    suspend fun revokeOtherSessions(
        userId: UserId,
        currentSessionId: SessionId?
    ): Result<Unit> = try {
        val activeSessionId = currentSessionId
            ?: throw DokusException.SessionInvalid("Current session identity is missing")

        val result = refreshTokenRepository.revokeOtherSessions(userId, activeSessionId)
        if (result is SessionRevocationResult.Revoked) {
            result.sessions.forEach { blacklistRevokedSession(it) }
        }

        Result.success(Unit)
    } catch (e: DokusException) {
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Failed to revoke other sessions for user {}", userId.value, e)
        Result.failure(DokusException.InternalError("Failed to revoke other sessions"))
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
                logger.warn(
                    "Failed to revoke tokens during deactivation for user: ${userId.value}",
                    error
                )
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
        Result.success(userRepository.projectUserAvatar(updatedUser))
    } catch (e: DokusException) {
        logger.error("Profile update failed: ${e.errorCode} for user: ${userId.value}", e)
        Result.failure(e)
    } catch (e: Exception) {
        logger.error("Profile update error for user: ${userId.value}", e)
        Result.failure(DokusException.InternalError(e.message ?: "Profile update failed"))
    }

    private fun resolveDefaultTenantId(
        memberships: List<TenantMembership>,
        selectedTenantId: TenantId? = null
    ): TenantId? {
        val activeMemberships = memberships.filter { it.isActive }
        val targetTenantId = when {
            selectedTenantId != null -> selectedTenantId
            activeMemberships.size == 1 -> activeMemberships.first().tenantId
            else -> return null
        }

        return activeMemberships.firstOrNull { it.tenantId == targetTenantId }?.tenantId
    }

    private fun List<TenantMembership>.toJwtTenantClaims(): List<JwtTenantMembershipClaim> {
        return asSequence()
            .filter { it.isActive }
            .map { membership ->
                JwtTenantMembershipClaim(
                    tenantId = membership.tenantId,
                    role = membership.role
                )
            }
            .toList()
    }

    private fun List<FirmMembership>.toJwtFirmClaims(): List<JwtFirmMembershipClaim> {
        return asSequence()
            .filter { it.isActive }
            .map { membership ->
                JwtFirmMembershipClaim(
                    firmId = membership.firmId,
                    role = membership.role
                )
            }
            .toList()
    }
}
