package tech.dokus.backend.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.auth.AuthService
import tech.dokus.backend.services.auth.EmailVerificationService
import tech.dokus.backend.services.auth.PasswordResetService
import tech.dokus.backend.services.auth.RateLimitServiceInterface
import tech.dokus.backend.services.auth.WelcomeEmailService
import tech.dokus.database.repository.auth.RefreshTokenRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.LoginRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.RegisterRequest
import tech.dokus.foundation.backend.security.JwtGenerator
import kotlin.test.assertTrue

class AuthServiceWelcomeFlowTest {

    private val userRepository = mockk<UserRepository>()
    private val jwtGenerator = mockk<JwtGenerator>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val rateLimitService = mockk<RateLimitServiceInterface>()
    private val welcomeEmailService = mockk<WelcomeEmailService>()
    private val emailVerificationService = mockk<EmailVerificationService>()
    private val passwordResetService = mockk<PasswordResetService>()

    private val authService = AuthService(
        userRepository = userRepository,
        jwtGenerator = jwtGenerator,
        refreshTokenRepository = refreshTokenRepository,
        rateLimitService = rateLimitService,
        welcomeEmailService = welcomeEmailService,
        emailVerificationService = emailVerificationService,
        passwordResetService = passwordResetService
    )

    @Test
    fun `register does not send verification email or welcome email`() {
        val user = testUser()

        coEvery { userRepository.register(any(), any(), any(), any()) } returns user
        coEvery { jwtGenerator.generateClaims(any(), any(), any()) } returns testClaims(user.id, null)
        coEvery { jwtGenerator.generateTokens(any()) } returns LoginResponse(
            accessToken = "access",
            refreshToken = "refresh",
            expiresIn = 3600
        )
        coEvery { refreshTokenRepository.saveRefreshToken(any(), any(), any()) } returns Result.success(Unit)
        coEvery { userRepository.recordSuccessfulLogin(any(), any()) } returns true

        val result = runBlocking {
            authService.register(
                RegisterRequest(
                    email = Email("welcome.register@test.dokus"),
                    password = Password("P@ssword123!"),
                    firstName = Name("Welcome"),
                    lastName = Name("Tester")
                )
            )
        }

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { userRepository.recordSuccessfulLogin(user.id, any()) }
        coVerify(exactly = 0) { emailVerificationService.sendVerificationEmail(any(), any()) }
        coVerify(exactly = 0) { welcomeEmailService.scheduleIfEligible(any(), any()) }
    }

    @Test
    fun `first successful login schedules welcome email when tenant is resolved`() {
        val tenantId = TenantId.generate()
        val user = testUser()

        coEvery { rateLimitService.checkLoginAttempts(any()) } returns Result.success(Unit)
        coEvery { userRepository.verifyCredentials(any(), any()) } returns user
        coEvery { userRepository.getUserTenants(user.id) } returns listOf(
            TenantMembership(
                userId = user.id,
                tenantId = tenantId,
                role = UserRole.Owner,
                isActive = true,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            )
        )
        coEvery { jwtGenerator.generateClaims(any(), any(), any()) } returns testClaims(user.id, tenantId)
        coEvery { jwtGenerator.generateTokens(any()) } returns LoginResponse(
            accessToken = "access",
            refreshToken = "refresh",
            expiresIn = 3600
        )
        coEvery { refreshTokenRepository.countActiveForUser(user.id) } returns 0
        coEvery { refreshTokenRepository.saveRefreshToken(any(), any(), any()) } returns Result.success(Unit)
        coEvery { rateLimitService.resetLoginAttempts(any()) } returns Unit
        coEvery { userRepository.recordSuccessfulLogin(any(), any()) } returns true
        coEvery { welcomeEmailService.scheduleIfEligible(user.id, tenantId) } returns Result.success(Unit)

        val result = runBlocking {
            authService.login(
                LoginRequest(
                    email = Email("welcome.login@test.dokus"),
                    password = Password("P@ssword123!")
                )
            )
        }

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { welcomeEmailService.scheduleIfEligible(user.id, tenantId) }
    }

    @Test
    fun `resend verification email is a no-op success`() {
        val userId = UserId.generate()

        val result = runBlocking {
            authService.resendVerificationEmail(userId)
        }

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { emailVerificationService.resendVerificationEmail(any()) }
    }

    @Test
    fun `tenant selection schedules welcome when first sign in recorded and welcome not sent`() {
        val tenantId = TenantId.generate()
        val user = testUser()

        coEvery { userRepository.findById(user.id) } returns user
        coEvery { userRepository.getUserTenants(user.id) } returns listOf(
            TenantMembership(
                userId = user.id,
                tenantId = tenantId,
                role = UserRole.Owner,
                isActive = true,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            )
        )
        coEvery { jwtGenerator.generateClaims(any(), any(), any()) } returns testClaims(user.id, tenantId)
        coEvery { jwtGenerator.generateTokens(any()) } returns LoginResponse(
            accessToken = "access",
            refreshToken = "refresh",
            expiresIn = 3600
        )
        coEvery { refreshTokenRepository.countActiveForUser(user.id) } returns 0
        coEvery { refreshTokenRepository.saveRefreshToken(any(), any(), any()) } returns Result.success(Unit)
        coEvery { userRepository.hasFirstSignIn(user.id) } returns true
        coEvery { userRepository.hasWelcomeEmailSent(user.id) } returns false
        coEvery { welcomeEmailService.scheduleIfEligible(user.id, tenantId) } returns Result.success(Unit)

        val result = runBlocking {
            authService.selectOrganization(user.id, tenantId)
        }

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { welcomeEmailService.scheduleIfEligible(user.id, tenantId) }
    }

    private fun testUser(): User {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return User(
            id = UserId.generate(),
            email = Email("welcome@test.dokus"),
            firstName = Name("Welcome"),
            lastName = Name("Tester"),
            emailVerified = true,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun testClaims(userId: UserId, tenantId: TenantId?): JwtClaims {
        val nowSeconds = Clock.System.now().epochSeconds
        return JwtClaims(
            userId = userId,
            email = "welcome@test.dokus",
            tenant = tenantId?.let {
                tech.dokus.domain.model.auth.TenantScope(
                    tenantId = it,
                    permissions = emptySet(),
                    subscriptionTier = tech.dokus.domain.enums.SubscriptionTier.Core,
                    role = UserRole.Owner
                )
            },
            iat = nowSeconds,
            exp = nowSeconds + 3600,
            jti = "test-jti"
        )
    }
}
