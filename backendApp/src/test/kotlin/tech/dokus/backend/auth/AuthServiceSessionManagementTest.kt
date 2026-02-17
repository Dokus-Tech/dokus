package tech.dokus.backend.auth

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.auth.AuthService
import tech.dokus.backend.services.auth.EmailVerificationService
import tech.dokus.backend.services.auth.PasswordResetService
import tech.dokus.backend.services.auth.RateLimitServiceInterface
import tech.dokus.backend.services.auth.WelcomeEmailService
import tech.dokus.database.repository.auth.RefreshTokenRepository
import tech.dokus.database.repository.auth.RevokedSessionInfo
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.foundation.backend.security.JwtGenerator
import tech.dokus.foundation.backend.security.TokenBlacklistService
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthServiceSessionManagementTest {

    private val userRepository = mockk<UserRepository>()
    private val jwtGenerator = mockk<JwtGenerator>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val rateLimitService = mockk<RateLimitServiceInterface>()
    private val welcomeEmailService = mockk<WelcomeEmailService>()
    private val emailVerificationService = mockk<EmailVerificationService>()
    private val passwordResetService = mockk<PasswordResetService>()
    private val tokenBlacklistService = mockk<TokenBlacklistService>()

    private val authService = AuthService(
        userRepository = userRepository,
        jwtGenerator = jwtGenerator,
        refreshTokenRepository = refreshTokenRepository,
        rateLimitService = rateLimitService,
        welcomeEmailService = welcomeEmailService,
        emailVerificationService = emailVerificationService,
        passwordResetService = passwordResetService,
        tokenBlacklistService = tokenBlacklistService
    )

    @Test
    fun `change password with valid current password revokes other sessions`() {
        val user = testUser()
        val currentSessionJti = "11111111-1111-1111-1111-111111111111"
        val revokedSession = RevokedSessionInfo(
            sessionId = SessionId("22222222-2222-2222-2222-222222222222"),
            accessTokenJti = "33333333-3333-3333-3333-333333333333",
            accessTokenExpiresAt = Instant.fromEpochSeconds(1_900_000_000)
        )

        coEvery { rateLimitService.checkLoginAttempts("pwd-change:${user.id}") } returns Result.success(Unit)
        coJustRun { rateLimitService.resetLoginAttempts("pwd-change:${user.id}") }
        coEvery { userRepository.findById(user.id) } returns user
        coEvery { userRepository.verifyCredentials(user.email.value, "CurrentPass123!") } returns user
        coJustRun { userRepository.updatePassword(user.id, "NewPass123!") }
        coEvery { refreshTokenRepository.revokeOtherSessions(user.id, currentSessionJti) } returns
            Result.success(listOf(revokedSession))
        coJustRun { tokenBlacklistService.blacklistToken(any(), any()) }

        val result = runBlocking {
            authService.changePassword(
                userId = user.id,
                currentPassword = Password("CurrentPass123!"),
                newPassword = Password("NewPass123!"),
                currentSessionJti = currentSessionJti
            )
        }

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { userRepository.updatePassword(user.id, "NewPass123!") }
        coVerify(exactly = 1) { refreshTokenRepository.revokeOtherSessions(user.id, currentSessionJti) }
        coVerify(exactly = 1) { tokenBlacklistService.blacklistToken(revokedSession.accessTokenJti!!, any()) }
    }

    @Test
    fun `change password with invalid current password fails`() {
        val user = testUser()

        coEvery { rateLimitService.checkLoginAttempts("pwd-change:${user.id}") } returns Result.success(Unit)
        coJustRun { rateLimitService.recordFailedLogin("pwd-change:${user.id}") }
        coEvery { userRepository.findById(user.id) } returns user
        coEvery { userRepository.verifyCredentials(user.email.value, "WrongPassword123!") } returns null

        val result = runBlocking {
            authService.changePassword(
                userId = user.id,
                currentPassword = Password("WrongPassword123!"),
                newPassword = Password("NewPass123!"),
                currentSessionJti = "11111111-1111-1111-1111-111111111111"
            )
        }

        assertTrue(result.isFailure)
        assertIs<DokusException.InvalidCredentials>(result.exceptionOrNull())
        coVerify(exactly = 0) { userRepository.updatePassword(any(), any()) }
        coVerify(exactly = 0) { refreshTokenRepository.revokeOtherSessions(any(), any()) }
    }

    @Test
    fun `change password without current session identity fails before password update`() {
        val user = testUser()

        coEvery { rateLimitService.checkLoginAttempts("pwd-change:${user.id}") } returns Result.success(Unit)
        coEvery { userRepository.findById(user.id) } returns user
        coEvery { userRepository.verifyCredentials(user.email.value, "CurrentPass123!") } returns user

        val result = runBlocking {
            authService.changePassword(
                userId = user.id,
                currentPassword = Password("CurrentPass123!"),
                newPassword = Password("NewPass123!"),
                currentSessionJti = null
            )
        }

        assertTrue(result.isFailure)
        assertIs<DokusException.SessionInvalid>(result.exceptionOrNull())
        coVerify(exactly = 0) { userRepository.updatePassword(any(), any()) }
        coVerify(exactly = 0) { refreshTokenRepository.revokeOtherSessions(any(), any()) }
    }

    @Test
    fun `list sessions forwards current-session identity`() {
        val userId = UserId.generate()
        val currentSessionJti = "11111111-1111-1111-1111-111111111111"
        val expectedSessions = listOf(
            SessionDto(
                id = SessionId(currentSessionJti),
                isCurrent = true,
                deviceType = tech.dokus.domain.DeviceType.Desktop
            )
        )

        coEvery { refreshTokenRepository.listActiveSessions(userId, currentSessionJti) } returns expectedSessions

        val result = runBlocking {
            authService.listSessions(userId, currentSessionJti)
        }

        assertTrue(result.isSuccess)
        assertEquals(expectedSessions, result.getOrNull())
        coVerify(exactly = 1) { refreshTokenRepository.listActiveSessions(userId, currentSessionJti) }
    }

    @Test
    fun `revoke single session blacklists access token`() {
        val userId = UserId.generate()
        val sessionId = SessionId("44444444-4444-4444-4444-444444444444")
        val revoked = RevokedSessionInfo(
            sessionId = sessionId,
            accessTokenJti = "55555555-5555-5555-5555-555555555555",
            accessTokenExpiresAt = Instant.fromEpochSeconds(1_900_000_000)
        )

        coEvery { refreshTokenRepository.revokeSessionById(userId, sessionId) } returns Result.success(revoked)
        coJustRun { tokenBlacklistService.blacklistToken(any(), any()) }

        val result = runBlocking {
            authService.revokeSession(userId, sessionId)
        }

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { refreshTokenRepository.revokeSessionById(userId, sessionId) }
        coVerify(exactly = 1) { tokenBlacklistService.blacklistToken(revoked.accessTokenJti!!, any()) }
    }

    @Test
    fun `revoke others keeps current session by using current jti filter`() {
        val userId = UserId.generate()
        val currentSessionJti = "66666666-6666-6666-6666-666666666666"
        val revoked = RevokedSessionInfo(
            sessionId = SessionId("77777777-7777-7777-7777-777777777777"),
            accessTokenJti = "88888888-8888-8888-8888-888888888888",
            accessTokenExpiresAt = Instant.fromEpochSeconds(1_900_000_000)
        )

        coEvery { refreshTokenRepository.revokeOtherSessions(userId, currentSessionJti) } returns
            Result.success(listOf(revoked))
        coJustRun { tokenBlacklistService.blacklistToken(any(), any()) }

        val result = runBlocking {
            authService.revokeOtherSessions(userId, currentSessionJti)
        }

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { refreshTokenRepository.revokeOtherSessions(userId, currentSessionJti) }
        coVerify(exactly = 1) { tokenBlacklistService.blacklistToken(revoked.accessTokenJti!!, any()) }
    }

    private fun testUser(): User {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return User(
            id = UserId.generate(),
            email = Email("session@test.dokus"),
            firstName = Name("Session"),
            lastName = Name("Tester"),
            emailVerified = true,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
    }
}
