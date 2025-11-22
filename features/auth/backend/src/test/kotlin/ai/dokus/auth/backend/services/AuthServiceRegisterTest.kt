@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.database.repository.RefreshTokenRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.ktor.security.JwtGenerator
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Comprehensive tests for AuthService.register()
 *
 * Tests cover:
 * - Successful registration without organization
 * - JWT token generation with empty organization scopes
 * - Refresh token persistence
 * - Email verification email sending
 * - Duplicate email handling
 * - Error handling
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AuthServiceRegisterTest {

    private lateinit var authService: AuthService
    private lateinit var userRepository: UserRepository
    private lateinit var jwtGenerator: JwtGenerator
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var rateLimitService: RateLimitService
    private lateinit var emailVerificationService: EmailVerificationService
    private lateinit var passwordResetService: PasswordResetService

    private val testUserId = UserId(Uuid.random().toString())
    private val testEmail = "test@example.com"
    private val testPassword = "SecurePass123!"
    private val testFirstName = "John"
    private val testLastName = "Doe"

    @BeforeEach
    fun setup() {
        userRepository = mockk(relaxed = true)
        jwtGenerator = mockk(relaxed = true)
        refreshTokenRepository = mockk(relaxed = true)
        rateLimitService = mockk(relaxed = true)
        emailVerificationService = mockk(relaxed = true)
        passwordResetService = mockk(relaxed = true)

        authService = AuthService(
            userRepository = userRepository,
            jwtGenerator = jwtGenerator,
            refreshTokenRepository = refreshTokenRepository,
            rateLimitService = rateLimitService,
            emailVerificationService = emailVerificationService,
            passwordResetService = passwordResetService
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `register should create user without organization`() = runBlocking {
        // Given
        val request = RegisterRequest(
            email = Email(testEmail),
            password = Password(testPassword),
            firstName = Name(testFirstName),
            lastName = Name(testLastName)
        )

        val mockUser = mockk<User> {
            every { id } returns testUserId
            every { email } returns Email(testEmail)
            every { firstName } returns testFirstName
            every { lastName } returns testLastName
            every { isActive } returns true
        }

        val mockClaims = mockk<JwtClaims>()
        val mockResponse = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 3600
        )

        coEvery {
            userRepository.register(
                email = testEmail,
                password = testPassword,
                firstName = testFirstName,
                lastName = testLastName
            )
        } returns mockUser

        every {
            jwtGenerator.generateClaims(
                userId = testUserId,
                email = testEmail,
                organizations = emptyList()
            )
        } returns mockClaims

        every { jwtGenerator.generateTokens(mockClaims) } returns mockResponse

        coEvery {
            refreshTokenRepository.saveRefreshToken(any(), any(), any())
        } returns Result.success(Unit)

        coEvery {
            emailVerificationService.sendVerificationEmail(any(), any())
        } returns Result.success(Unit)

        // When
        val result = authService.register(request)

        // Then
        assertTrue(result.isSuccess, "Registration should succeed")
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("access-token", response?.accessToken)
        assertEquals("refresh-token", response?.refreshToken)

        // Verify user was registered without organization
        coVerify(exactly = 1) {
            userRepository.register(
                email = testEmail,
                password = testPassword,
                firstName = testFirstName,
                lastName = testLastName
            )
        }

        // Verify JWT was generated with empty organization scopes
        coVerify(exactly = 1) {
            jwtGenerator.generateClaims(
                userId = testUserId,
                email = testEmail,
                organizations = emptyList()
            )
        }
    }

    @Test
    fun `register should save refresh token`() = runBlocking {
        // Given
        val request = RegisterRequest(
            email = Email(testEmail),
            password = Password(testPassword),
            firstName = Name(testFirstName),
            lastName = Name(testLastName)
        )

        val mockUser = mockk<User> {
            every { id } returns testUserId
            every { email } returns Email(testEmail)
            every { firstName } returns testFirstName
            every { lastName } returns testLastName
            every { isActive } returns true
        }

        val mockClaims = mockk<JwtClaims>()
        val mockResponse = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 3600
        )

        coEvery { userRepository.register(any(), any(), any(), any()) } returns mockUser
        every { jwtGenerator.generateClaims(any(), any(), any()) } returns mockClaims
        every { jwtGenerator.generateTokens(any()) } returns mockResponse

        val tokenSlot = slot<String>()
        coEvery {
            refreshTokenRepository.saveRefreshToken(testUserId, capture(tokenSlot), any())
        } returns Result.success(Unit)

        coEvery { emailVerificationService.sendVerificationEmail(any(), any()) } returns Result.success(Unit)

        // When
        val result = authService.register(request)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("refresh-token", tokenSlot.captured, "Refresh token should be saved")

        coVerify(exactly = 1) {
            refreshTokenRepository.saveRefreshToken(testUserId, "refresh-token", any())
        }
    }

    @Test
    fun `register should send verification email`() = runBlocking {
        // Given
        val request = RegisterRequest(
            email = Email(testEmail),
            password = Password(testPassword),
            firstName = Name(testFirstName),
            lastName = Name(testLastName)
        )

        val mockUser = mockk<User> {
            every { id } returns testUserId
            every { email } returns Email(testEmail)
            every { firstName } returns testFirstName
            every { lastName } returns testLastName
            every { isActive } returns true
        }

        val mockClaims = mockk<JwtClaims>()
        val mockResponse = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 3600
        )

        coEvery { userRepository.register(any(), any(), any(), any()) } returns mockUser
        every { jwtGenerator.generateClaims(any(), any(), any()) } returns mockClaims
        every { jwtGenerator.generateTokens(any()) } returns mockResponse
        coEvery { refreshTokenRepository.saveRefreshToken(any(), any(), any()) } returns Result.success(Unit)
        coEvery { emailVerificationService.sendVerificationEmail(any(), any()) } returns Result.success(Unit)

        // When
        val result = authService.register(request)

        // Then
        assertTrue(result.isSuccess)

        coVerify(exactly = 1) {
            emailVerificationService.sendVerificationEmail(testUserId, testEmail)
        }
    }

    @Test
    fun `register should fail for duplicate email`() = runBlocking {
        // Given
        val request = RegisterRequest(
            email = Email(testEmail),
            password = Password(testPassword),
            firstName = Name(testFirstName),
            lastName = Name(testLastName)
        )

        coEvery {
            userRepository.register(any(), any(), any(), any())
        } throws IllegalArgumentException("User with email $testEmail already exists")

        // When
        val result = authService.register(request)

        // Then
        assertTrue(result.isFailure, "Registration should fail for duplicate email")
        assertTrue(
            result.exceptionOrNull() is DokusException.UserAlreadyExists,
            "Should throw UserAlreadyExists exception"
        )
    }

    @Test
    fun `register should succeed even if email verification fails`() = runBlocking {
        // Given
        val request = RegisterRequest(
            email = Email(testEmail),
            password = Password(testPassword),
            firstName = Name(testFirstName),
            lastName = Name(testLastName)
        )

        val mockUser = mockk<User> {
            every { id } returns testUserId
            every { email } returns Email(testEmail)
            every { firstName } returns testFirstName
            every { lastName } returns testLastName
            every { isActive } returns true
        }

        val mockClaims = mockk<JwtClaims>()
        val mockResponse = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 3600
        )

        coEvery { userRepository.register(any(), any(), any(), any()) } returns mockUser
        every { jwtGenerator.generateClaims(any(), any(), any()) } returns mockClaims
        every { jwtGenerator.generateTokens(any()) } returns mockResponse
        coEvery { refreshTokenRepository.saveRefreshToken(any(), any(), any()) } returns Result.success(Unit)

        // Email verification fails
        coEvery {
            emailVerificationService.sendVerificationEmail(any(), any())
        } returns Result.failure(RuntimeException("Email service unavailable"))

        // When
        val result = authService.register(request)

        // Then - registration should still succeed
        assertTrue(result.isSuccess, "Registration should succeed even if verification email fails")
    }

    @Test
    fun `register should fail if refresh token save fails`() = runBlocking {
        // Given
        val request = RegisterRequest(
            email = Email(testEmail),
            password = Password(testPassword),
            firstName = Name(testFirstName),
            lastName = Name(testLastName)
        )

        val mockUser = mockk<User> {
            every { id } returns testUserId
            every { email } returns Email(testEmail)
            every { firstName } returns testFirstName
            every { lastName } returns testLastName
            every { isActive } returns true
        }

        val mockClaims = mockk<JwtClaims>()
        val mockResponse = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 3600
        )

        coEvery { userRepository.register(any(), any(), any(), any()) } returns mockUser
        every { jwtGenerator.generateClaims(any(), any(), any()) } returns mockClaims
        every { jwtGenerator.generateTokens(any()) } returns mockResponse

        // Refresh token save fails
        coEvery {
            refreshTokenRepository.saveRefreshToken(any(), any(), any())
        } returns Result.failure(RuntimeException("Database error"))

        // When
        val result = authService.register(request)

        // Then
        assertTrue(result.isFailure, "Registration should fail if refresh token cannot be saved")
        assertTrue(
            result.exceptionOrNull() is DokusException.InternalError,
            "Should throw InternalError"
        )
    }

    @Test
    fun `register should handle empty first and last names`() = runBlocking {
        // Given
        val request = RegisterRequest(
            email = Email(testEmail),
            password = Password(testPassword),
            firstName = Name(""),
            lastName = Name("")
        )

        val mockUser = mockk<User> {
            every { id } returns testUserId
            every { email } returns Email(testEmail)
            every { firstName } returns ""
            every { lastName } returns ""
            every { isActive } returns true
        }

        val mockClaims = mockk<JwtClaims>()
        val mockResponse = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 3600
        )

        coEvery { userRepository.register(testEmail, testPassword, "", "") } returns mockUser
        every { jwtGenerator.generateClaims(any(), any(), any()) } returns mockClaims
        every { jwtGenerator.generateTokens(any()) } returns mockResponse
        coEvery { refreshTokenRepository.saveRefreshToken(any(), any(), any()) } returns Result.success(Unit)
        coEvery { emailVerificationService.sendVerificationEmail(any(), any()) } returns Result.success(Unit)

        // When
        val result = authService.register(request)

        // Then
        assertTrue(result.isSuccess, "Registration should succeed with empty names")

        coVerify {
            userRepository.register(
                email = testEmail,
                password = testPassword,
                firstName = "",
                lastName = ""
            )
        }
    }
}
