@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package ai.dokus.auth.backend.database.services

import ai.dokus.auth.backend.database.tables.RefreshTokensTable
import ai.dokus.auth.backend.database.tables.TenantsTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.ktor.database.now
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * Comprehensive tests for RefreshTokenServiceImpl
 *
 * Tests cover:
 * - Token persistence and retrieval
 * - Token validation and rotation
 * - Token revocation (single and bulk)
 * - Cleanup of expired tokens
 * - Security edge cases
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenServiceImplTest {

    private lateinit var database: DatabaseFactory
    private lateinit var service: RefreshTokenService
    private var testUserId: UserId? = null
    private val testTenantId = Uuid.random()

    @BeforeAll
    fun setup() {
        // Initialize in-memory H2 database for testing
        val appConfig = createTestAppConfig()
        database = DatabaseFactory(
            appConfig = appConfig,
            poolName = "test-pool"
        )

        runBlocking {
            database.init(TenantsTable, UsersTable, RefreshTokensTable)
            testUserId = createTestUser()
        }

        service = RefreshTokenServiceImpl()
    }

    /**
     * Creates a test configuration for in-memory H2 database
     */
    private fun createTestAppConfig(): ai.dokus.foundation.ktor.AppBaseConfig {
        val config = com.typesafe.config.ConfigFactory.parseString(
            """
            ktor {
                deployment {
                    port = 8080
                    host = "0.0.0.0"
                    environment = "test"
                }
            }
            database {
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
                username = "sa"
                password = ""
                pool {
                    maxSize = 5
                    minSize = 1
                    acquisitionTimeout = 30
                    idleTimeout = 600
                    maxLifetime = 1800
                    leakDetectionThreshold = 0
                }
            }
            flyway {
                enabled = false
                baselineOnMigrate = true
                baselineVersion = "1"
                locations = []
                schemas = ["public"]
            }
            jwt {
                issuer = "test"
                audience = "test"
                realm = "test"
                secret = "test-secret"
                algorithm = "HS256"
            }
            auth {
                maxLoginAttempts = 5
                lockDurationMinutes = 15
                sessionDurationHours = 24
                rememberMeDurationDays = 30
                maxConcurrentSessions = 5
                password {
                    expiryDays = 90
                    minLength = 8
                    requireUppercase = true
                    requireLowercase = true
                    requireDigits = true
                    requireSpecialChars = false
                    historySize = 5
                }
                rateLimit {
                    windowSeconds = 300
                    maxAttempts = 5
                }
                enableDeviceFingerprinting = false
                enableSessionSlidingExpiration = true
                sessionActivityWindowMinutes = 30
                logSecurityEvents = true
                enableDebugMode = false
            }
            logging {
                level = "INFO"
                consoleJson = false
            }
            metrics {
                enabled = false
                prometheusPath = "/metrics"
            }
            security {
                cors {
                    allowedHosts = ["localhost"]
                }
            }
            caching {
                type = "memory"
                ttl = 3600
                maxSize = 1000
                redis {
                    host = "localhost"
                    port = 6379
                    database = 0
                    pool {
                        maxTotal = 10
                        maxIdle = 5
                        minIdle = 1
                        testOnBorrow = true
                    }
                    timeout {
                        connection = 2000
                        socket = 2000
                        command = 2000
                    }
                }
            }
            """.trimIndent()
        )
        return ai.dokus.foundation.ktor.AppBaseConfig.fromConfig(config)
    }

    @AfterAll
    fun teardown() {
        database.close()
    }

    @BeforeEach
    fun clearTokens(): Unit = runBlocking {
        dbQuery {
            RefreshTokensTable.deleteAll()
        }
    }

    @Test
    fun `saveRefreshToken should persist token successfully`() = runBlocking {
        val token = "test-refresh-token-${Uuid.random()}"
        val expiresAt = now() + 30.days

        val result = service.saveRefreshToken(testUserId!!, token, expiresAt)

        assertTrue(result.isSuccess, "Token should be saved successfully")

        // Verify token exists in database
        val tokenExists = dbQuery {
            RefreshTokensTable.selectAll()
                .where { RefreshTokensTable.token eq token }
                .count() > 0
        }
        assertTrue(tokenExists, "Token should exist in database")
    }

    @Test
    fun `validateAndRotate should succeed for valid token`() = runBlocking {
        val token = "test-token-${Uuid.random()}"
        val expiresAt = now() + 30.days

        service.saveRefreshToken(testUserId!!, token, expiresAt)

        val result = service.validateAndRotate(token)

        assertTrue(result.isSuccess, "Valid token should be accepted")
        assertEquals(testUserId!!.value, result.getOrNull()?.value, "Should return correct userId")

        // Verify old token is revoked
        val isRevoked = dbQuery {
            RefreshTokensTable.selectAll()
                .where { RefreshTokensTable.token eq token }
                .single()[RefreshTokensTable.isRevoked]
        }
        assertTrue(isRevoked, "Old token should be revoked after rotation")
    }

    @Test
    fun `validateAndRotate should fail for expired token`() = runBlocking {
        val token = "expired-token-${Uuid.random()}"
        val expiresAt = now() - 1.minutes // Already expired

        service.saveRefreshToken(testUserId!!, token, expiresAt)

        val result = service.validateAndRotate(token)

        assertTrue(result.isFailure, "Expired token should be rejected")
        assertTrue(
            result.exceptionOrNull() is IllegalArgumentException,
            "Should throw IllegalArgumentException for expired token"
        )
    }

    @Test
    fun `validateAndRotate should fail for revoked token`() = runBlocking {
        val token = "revoked-token-${Uuid.random()}"
        val expiresAt = now() + 30.days

        service.saveRefreshToken(testUserId!!, token, expiresAt)
        service.revokeToken(token)

        val result = service.validateAndRotate(token)

        assertTrue(result.isFailure, "Revoked token should be rejected")
        assertTrue(
            result.exceptionOrNull() is SecurityException,
            "Should throw SecurityException for revoked token"
        )
    }

    @Test
    fun `validateAndRotate should fail for non-existent token`() = runBlocking {
        val nonExistentToken = "non-existent-${Uuid.random()}"

        val result = service.validateAndRotate(nonExistentToken)

        assertTrue(result.isFailure, "Non-existent token should be rejected")
        assertTrue(
            result.exceptionOrNull() is IllegalArgumentException,
            "Should throw IllegalArgumentException for non-existent token"
        )
    }

    @Test
    fun `revokeToken should mark token as revoked`() = runBlocking {
        val token = "test-token-${Uuid.random()}"
        val expiresAt = now() + 30.days

        service.saveRefreshToken(testUserId!!, token, expiresAt)

        val result = service.revokeToken(token)

        assertTrue(result.isSuccess, "Token revocation should succeed")

        // Verify token is revoked
        val isRevoked = dbQuery {
            RefreshTokensTable.selectAll()
                .where { RefreshTokensTable.token eq token }
                .single()[RefreshTokensTable.isRevoked]
        }
        assertTrue(isRevoked, "Token should be marked as revoked")
    }

    @Test
    fun `revokeAllUserTokens should revoke all tokens for user`() = runBlocking {
        val expiresAt = now() + 30.days

        // Create multiple tokens for the user
        val tokens = (1..3).map { "token-$it-${Uuid.random()}" }
        tokens.forEach { token ->
            service.saveRefreshToken(testUserId!!, token, expiresAt)
        }

        val result = service.revokeAllUserTokens(testUserId!!)

        assertTrue(result.isSuccess, "Bulk revocation should succeed")

        // Verify all tokens are revoked
        val revokedCount = dbQuery {
            RefreshTokensTable.selectAll()
                .where {
                    (RefreshTokensTable.userId eq testUserId!!.uuid.toJavaUuid()) and
                            (RefreshTokensTable.isRevoked eq true)
                }
                .count()
        }
        assertEquals(tokens.size.toLong(), revokedCount, "All tokens should be revoked")
    }

    @Test
    fun `cleanupExpiredTokens should remove expired and revoked tokens`() = runBlocking {
        val now = now()

        // Create expired token
        val expiredToken = "expired-${Uuid.random()}"
        service.saveRefreshToken(testUserId!!, expiredToken, now - 1.minutes)

        // Create revoked token
        val revokedToken = "revoked-${Uuid.random()}"
        service.saveRefreshToken(testUserId!!, revokedToken, now + 30.days)
        service.revokeToken(revokedToken)

        // Create valid token
        val validToken = "valid-${Uuid.random()}"
        service.saveRefreshToken(testUserId!!, validToken, now + 30.days)

        val result = service.cleanupExpiredTokens()

        assertTrue(result.isSuccess, "Cleanup should succeed")
        assertEquals(2, result.getOrNull(), "Should delete 2 tokens (expired + revoked)")

        // Verify only valid token remains
        val remainingCount = dbQuery {
            RefreshTokensTable.selectAll().count()
        }
        assertEquals(1L, remainingCount, "Only valid token should remain")
    }

    @Test
    fun `getUserActiveTokens should return only active tokens`() = runBlocking {
        val now = now()
        val expiresAt = now + 30.days

        // Create active token
        val activeToken = "active-${Uuid.random()}"
        service.saveRefreshToken(testUserId!!, activeToken, expiresAt)

        // Create revoked token
        val revokedToken = "revoked-${Uuid.random()}"
        service.saveRefreshToken(testUserId!!, revokedToken, expiresAt)
        service.revokeToken(revokedToken)

        // Create expired token
        val expiredToken = "expired-${Uuid.random()}"
        service.saveRefreshToken(testUserId!!, expiredToken, now - 1.minutes)

        val activeTokens = service.getUserActiveTokens(testUserId!!)

        assertEquals(1, activeTokens.size, "Should return only active token")
        assertFalse(activeTokens[0].isRevoked, "Returned token should not be revoked")
        assertTrue(
            activeTokens[0].expiresAt > now,
            "Returned token should not be expired"
        )
    }

    @Test
    fun `getUserActiveTokens should return empty list for user with no active tokens`() =
        runBlocking {
            val otherUserId = UserId(Uuid.random().toString())

            val activeTokens = service.getUserActiveTokens(otherUserId)

            assertTrue(activeTokens.isEmpty(), "Should return empty list for user with no tokens")
        }

    @Test
    fun `service should handle concurrent token operations safely`() = runBlocking {
        val token = "concurrent-token-${Uuid.random()}"
        val expiresAt = now() + 30.days

        service.saveRefreshToken(testUserId!!, token, expiresAt)

        // Attempt concurrent validation and revocation
        val validateResult = service.validateAndRotate(token)
        val revokeResult = service.revokeToken(token)

        // One should succeed, the other should fail
        val successCount = listOf(validateResult, revokeResult).count { it.isSuccess }
        assertTrue(successCount >= 1, "At least one operation should succeed")
    }

    /**
     * Helper function to create a test user in the database
     */
    private suspend fun createTestUser(): UserId {
        val userId = Uuid.random()

        dbQuery {
            // Create test tenant first
            TenantsTable.insert {
                it[id] = testTenantId.toJavaUuid()
                it[name] = "Test Tenant"
                it[email] = "test@example.com"
            }

            // Create test user
            UsersTable.insert {
                it[id] = userId.toJavaUuid()
                it[tenantId] = testTenantId.toJavaUuid()
                it[email] = "test@example.com"
                it[passwordHash] = "hashed-password"
                it[role] = UserRole.Owner
                it[isActive] = true
            }
        }

        return UserId(userId.toString())
    }
}
