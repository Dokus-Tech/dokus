@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.database.repository.RefreshTokenRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.auth.backend.database.repository.OrganizationRepository
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.ktor.security.JwtGenerator
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Comprehensive tests for AuthService.deactivateAccount()
 *
 * Tests cover:
 * - Successful account deactivation
 * - User not found error handling
 * - Already inactive account handling
 * - Token revocation during deactivation
 * - Audit logging of deactivation reason
 * - Error handling for failed operations
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AuthServiceDeactivateAccountTest {

    private lateinit var authService: AuthService
    private lateinit var userRepository: UserRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var jwtGenerator: JwtGenerator
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var rateLimitService: RateLimitService
    private lateinit var emailVerificationService: EmailVerificationService
    private lateinit var passwordResetService: PasswordResetService

    private val testUserId = UserId(Uuid.random().toString())
    private val testReason = "User requested account deletion"

    @BeforeEach
    fun setup() {
        // Create mocks for all dependencies
        userRepository = mockk(relaxed = true)
        organizationRepository = mockk(relaxed = true)
        jwtGenerator = mockk(relaxed = true)
        refreshTokenRepository = mockk(relaxed = true)
        rateLimitService = mockk(relaxed = true)
        emailVerificationService = mockk(relaxed = true)
        passwordResetService = mockk(relaxed = true)

        authService = AuthService(
            userRepository = userRepository,
            organizationRepository = organizationRepository,
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
    fun `deactivateAccount should successfully deactivate active user`() = runBlocking {
        // Given: An active user exists
        val businessUserId = testUserId
        val activeUser = mockk<User> {
            every { isActive } returns true
            every { id } returns businessUserId
        }
        coEvery { userRepository.findById(testUserId) } returns activeUser
        coEvery { userRepository.deactivate(testUserId, any()) } just Runs
        coEvery { refreshTokenRepository.revokeAllUserTokens(testUserId) } returns Result.success(Unit)

        // When: Deactivate account is called
        val result = authService.deactivateAccount(testUserId, testReason)

        // Then: Account should be deactivated successfully
        assertTrue(result.isSuccess, "Deactivation should succeed")

        // Verify: User service deactivate was called with correct parameters
        coVerify(exactly = 1) {
            userRepository.deactivate(testUserId, testReason)
        }

        // Verify: All refresh tokens were revoked
        coVerify(exactly = 1) {
            refreshTokenRepository.revokeAllUserTokens(testUserId)
        }
    }

    @Test
    fun `deactivateAccount should fail when user not found`() = runBlocking {
        // Given: User does not exist
        coEvery { userRepository.findById(testUserId) } returns null

        // When: Deactivate account is called
        val result = authService.deactivateAccount(testUserId, testReason)

        // Then: Should return failure with appropriate exception
        assertTrue(result.isFailure, "Should fail for non-existent user")
        assertTrue(
            result.exceptionOrNull() is DokusException.InvalidCredentials,
            "Should throw InvalidCredentials exception"
        )

        // Verify: Deactivate was never called
        coVerify(exactly = 0) {
            userRepository.deactivate(any(), any())
        }

        // Verify: Token revocation was never attempted
        coVerify(exactly = 0) {
            refreshTokenRepository.revokeAllUserTokens(any())
        }
    }

    @Test
    fun `deactivateAccount should fail when user already inactive`() = runBlocking {
        // Given: User exists but is already inactive
        val businessUserId = testUserId
        val inactiveUser = mockk<User> {
            every { isActive } returns false
            every { id } returns businessUserId
        }
        coEvery { userRepository.findById(testUserId) } returns inactiveUser

        // When: Deactivate account is called
        val result = authService.deactivateAccount(testUserId, testReason)

        // Then: Should return failure
        assertTrue(result.isFailure, "Should fail for already inactive user")
        assertTrue(
            result.exceptionOrNull() is DokusException.AccountInactive,
            "Should throw AccountInactive exception"
        )

        // Verify: Deactivate was never called
        coVerify(exactly = 0) {
            userRepository.deactivate(any(), any())
        }

        // Verify: Token revocation was never attempted
        coVerify(exactly = 0) {
            refreshTokenRepository.revokeAllUserTokens(any())
        }
    }

    @Test
    fun `deactivateAccount should succeed even if token revocation fails`() = runBlocking {
        // Given: User exists and is active
        val businessUserId = testUserId
        val activeUser = mockk<User> {
            every { isActive } returns true
            every { id } returns businessUserId
        }
        coEvery { userRepository.findById(testUserId) } returns activeUser
        coEvery { userRepository.deactivate(testUserId, any()) } just Runs

        // Token revocation fails
        coEvery { refreshTokenRepository.revokeAllUserTokens(testUserId) } returns
            Result.failure(RuntimeException("Token revocation failed"))

        // When: Deactivate account is called
        val result = authService.deactivateAccount(testUserId, testReason)

        // Then: Should still succeed (token revocation failure is logged but doesn't fail the operation)
        assertTrue(result.isSuccess, "Deactivation should succeed despite token revocation failure")

        // Verify: User was still deactivated
        coVerify(exactly = 1) {
            userRepository.deactivate(testUserId, testReason)
        }
    }

    @Test
    fun `deactivateAccount should propagate reason to user service`() = runBlocking {
        // Given: User exists and is active
        val businessUserId = testUserId
        val activeUser = mockk<User> {
            every { isActive } returns true
            every { id } returns businessUserId
        }
        coEvery { userRepository.findById(testUserId) } returns activeUser
        coEvery { userRepository.deactivate(testUserId, any()) } just Runs
        coEvery { refreshTokenRepository.revokeAllUserTokens(testUserId) } returns Result.success(Unit)

        val customReason = "GDPR data deletion request"

        // When: Deactivate account is called with custom reason
        val result = authService.deactivateAccount(testUserId, customReason)

        // Then: Should succeed
        assertTrue(result.isSuccess)

        // Verify: Reason was passed to user service
        coVerify(exactly = 1) {
            userRepository.deactivate(testUserId, customReason)
        }
    }

    @Test
    fun `deactivateAccount should handle database errors gracefully`() = runBlocking {
        // Given: User exists and is active
        val businessUserId = testUserId
        val activeUser = mockk<User> {
            every { isActive } returns true
            every { id } returns businessUserId
        }
        coEvery { userRepository.findById(testUserId) } returns activeUser

        // Database error occurs during deactivation
        coEvery { userRepository.deactivate(testUserId, any()) } throws
            RuntimeException("Database connection failed")

        // When: Deactivate account is called
        val result = authService.deactivateAccount(testUserId, testReason)

        // Then: Should return failure with InternalError
        assertTrue(result.isFailure, "Should fail on database error")
        assertTrue(
            result.exceptionOrNull() is DokusException.InternalError,
            "Should throw InternalError exception"
        )

        // Verify: Token revocation was never attempted (deactivation failed first)
        coVerify(exactly = 0) {
            refreshTokenRepository.revokeAllUserTokens(any())
        }
    }

    @Test
    fun `deactivateAccount should revoke all tokens after successful deactivation`() = runBlocking {
        // Given: User exists and is active
        val businessUserId = testUserId
        val activeUser = mockk<User> {
            every { isActive } returns true
            every { id } returns businessUserId
        }
        coEvery { userRepository.findById(testUserId) } returns activeUser
        coEvery { userRepository.deactivate(testUserId, any()) } just Runs
        coEvery { refreshTokenRepository.revokeAllUserTokens(testUserId) } returns Result.success(Unit)

        // When: Deactivate account is called
        val result = authService.deactivateAccount(testUserId, testReason)

        // Then: Should succeed
        assertTrue(result.isSuccess)

        // Verify: Operations happened in correct order
        coVerifyOrder {
            userRepository.findById(testUserId)
            userRepository.deactivate(testUserId, testReason)
            refreshTokenRepository.revokeAllUserTokens(testUserId)
        }
    }

    @Test
    fun `deactivateAccount should work with empty reason string`() = runBlocking {
        // Given: User exists and is active
        val businessUserId = testUserId
        val activeUser = mockk<User> {
            every { isActive } returns true
            every { id } returns businessUserId
        }
        coEvery { userRepository.findById(testUserId) } returns activeUser
        coEvery { userRepository.deactivate(testUserId, any()) } just Runs
        coEvery { refreshTokenRepository.revokeAllUserTokens(testUserId) } returns Result.success(Unit)

        // When: Deactivate account is called with empty reason
        val result = authService.deactivateAccount(testUserId, "")

        // Then: Should still succeed
        assertTrue(result.isSuccess)

        // Verify: Empty reason was passed
        coVerify(exactly = 1) {
            userRepository.deactivate(testUserId, "")
        }
    }
}
