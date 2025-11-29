@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.backend.database.tables.TenantMembersTable
import ai.dokus.auth.backend.database.tables.TenantTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService4j
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * Comprehensive tests for UserRepository
 *
 * Tests cover:
 * - User registration without tenant
 * - User registration with tenant
 * - Adding users to tenants
 * - Credential verification
 * - User profile management
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {

    private lateinit var database: DatabaseFactory
    private lateinit var repository: UserRepository
    private var testTenantId: TenantId? = null

    @BeforeAll
    fun setup() {
        val appConfig = createTestAppConfig()
        database = DatabaseFactory(
            appConfig = appConfig,
            poolName = "test-pool"
        )

        runBlocking {
            database.init(TenantTable, UsersTable, TenantMembersTable)
            testTenantId = createTestTenant()
        }

        repository = UserRepository(PasswordCryptoService4j())
    }

    private fun createTestAppConfig(): AppBaseConfig {
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
                url = "jdbc:h2:mem:user-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
                driver = "org.h2.Driver"
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
            rabbitmq {
                host = "localhost"
                port = 5672
                username = "guest"
                password = "guest"
                virtualHost = "/"
            }
            """.trimIndent()
        )
        return AppBaseConfig.fromConfig(config)
    }

    @AfterAll
    fun teardown() {
        database.close()
    }

    @BeforeEach
    fun clearData(): Unit = runBlocking {
        dbQuery {
            TenantMembersTable.deleteAll()
            UsersTable.deleteAll()
            TenantTable.deleteAll()
        }
        testTenantId = createTestTenant()
    }

    private suspend fun createTestTenant(): TenantId {
        val tenantId = Uuid.random()
        dbQuery {
            TenantTable.insert {
                it[id] = tenantId.toJavaUuid()
                it[type] = TenantType.Company
                it[legalName] = "Test Tenant"
                it[displayName] = "Test Display"
                it[plan] = TenantPlan.Free
                it[status] = TenantStatus.Active
                it[language] = Language.En
            }
        }
        return TenantId(tenantId)
    }

    // ===========================================
    // Tests for register() - without tenant
    // ===========================================

    @Test
    fun `register should create user without tenant`() = runBlocking {
        val email = "test-${Uuid.random()}@example.com"
        val password = "SecurePass123!"

        val user = repository.register(
            email = email,
            password = password,
            firstName = "John",
            lastName = "Doe"
        )

        assertNotNull(user.id, "User should have an ID")
        assertEquals(email, user.email.value, "Email should match")
        assertEquals(Name("John"), user.firstName, "First name should match")
        assertEquals(Name("Doe"), user.lastName, "Last name should match")
        assertTrue(user.isActive, "User should be active by default")

        // Verify no tenant membership exists
        val memberships = repository.getUserTenants(user.id)
        assertTrue(memberships.isEmpty(), "User should have no tenant memberships")
    }

    @Test
    fun `register should fail for duplicate email`() = runBlocking {
        val email = "duplicate-${Uuid.random()}@example.com"
        val password = "SecurePass123!"

        // First registration should succeed
        repository.register(
            email = email,
            password = password,
            firstName = "First",
            lastName = "User"
        )

        // Second registration with same email should fail
        val exception = assertThrows<IllegalArgumentException> {
            runBlocking {
                repository.register(
                    email = email,
                    password = password,
                    firstName = "Second",
                    lastName = "User"
                )
            }
        }

        assertTrue(exception.message?.contains("already exists") == true)
    }

    @Test
    fun `register should handle empty first and last name`() = runBlocking {
        val email = "nullname-${Uuid.random()}@example.com"
        val password = "SecurePass123!"

        val user = repository.register(
            email = email,
            password = password,
            firstName = "",
            lastName = ""
        )

        assertNotNull(user.id)
        assertEquals(Name(""), user.firstName, "First name should be empty")
        assertEquals(Name(""), user.lastName, "Last name should be empty")
    }

    // ===========================================
    // Tests for registerWithTenant()
    // ===========================================

    @Test
    fun `registerWithTenant should create user with membership`() = runBlocking {
        val email = "withtenant-${Uuid.random()}@example.com"
        val password = "SecurePass123!"

        val user = repository.registerWithTenant(
            tenantId = testTenantId!!,
            email = email,
            password = password,
            firstName = "Jane",
            lastName = "Smith",
            role = UserRole.Admin
        )

        assertNotNull(user.id)
        assertEquals(email, user.email.value)

        // Verify tenant membership exists
        val memberships = repository.getUserTenants(user.id)
        assertEquals(1, memberships.size, "User should have one membership")
        assertEquals(testTenantId!!, memberships[0].tenantId)
        assertEquals(UserRole.Admin, memberships[0].role)
    }

    @Test
    fun `registerWithTenant should fail for duplicate email`() = runBlocking {
        val email = "duptenant-${Uuid.random()}@example.com"

        repository.registerWithTenant(
            tenantId = testTenantId!!,
            email = email,
            password = "SecurePass123!",
            firstName = "First",
            lastName = "User",
            role = UserRole.Viewer
        )

        assertThrows<IllegalArgumentException> {
            runBlocking {
                repository.registerWithTenant(
                    tenantId = testTenantId!!,
                    email = email,
                    password = "AnotherPass123!",
                    firstName = "Second",
                    lastName = "User",
                    role = UserRole.Viewer
                )
            }
        }
    }

    // ===========================================
    // Tests for addToTenant()
    // ===========================================

    @Test
    fun `addToTenant should add existing user to tenant`() = runBlocking {
        val email = "addto-${Uuid.random()}@example.com"

        // Register user without tenant
        val user = repository.register(
            email = email,
            password = "SecurePass123!",
            firstName = "Add",
            lastName = "ToTenant"
        )

        // Verify no memberships initially
        val initialMemberships = repository.getUserTenants(user.id)
        assertTrue(initialMemberships.isEmpty())

        // Add to tenant
        repository.addToTenant(user.id, testTenantId!!, UserRole.Editor)

        // Verify membership was created
        val memberships = repository.getUserTenants(user.id)
        assertEquals(1, memberships.size)
        assertEquals(testTenantId!!, memberships[0].tenantId)
        assertEquals(UserRole.Editor, memberships[0].role)
    }

    @Test
    fun `user can be added to multiple tenants`() = runBlocking {
        // Create another tenant
        val secondTenantUuid = Uuid.random()
        dbQuery {
            TenantTable.insert {
                it[id] = secondTenantUuid.toJavaUuid()
                it[type] = TenantType.Freelancer
                it[legalName] = "Second Tenant"
                it[displayName] = "Second Display"
                it[plan] = TenantPlan.Free
                it[status] = TenantStatus.Active
                it[language] = Language.En
            }
        }
        val secondTenantId = TenantId(secondTenantUuid)

        // Register user without tenant
        val user = repository.register(
            email = "multitenant-${Uuid.random()}@example.com",
            password = "SecurePass123!",
            firstName = "Multi",
            lastName = "Tenant"
        )

        // Add to both tenants
        repository.addToTenant(user.id, testTenantId!!, UserRole.Owner)
        repository.addToTenant(user.id, secondTenantId, UserRole.Viewer)

        // Verify memberships
        val memberships = repository.getUserTenants(user.id)
        assertEquals(2, memberships.size, "User should belong to two tenants")

        val roles = memberships.associate { it.tenantId to it.role }
        assertEquals(UserRole.Owner, roles[testTenantId!!])
        assertEquals(UserRole.Viewer, roles[secondTenantId])
    }

    // ===========================================
    // Tests for verifyCredentials()
    // ===========================================

    @Test
    fun `verifyCredentials should return user for valid credentials`() = runBlocking {
        val email = "verify-${Uuid.random()}@example.com"
        val password = "SecurePass123!"

        repository.register(
            email = email,
            password = password,
            firstName = "Verify",
            lastName = "User"
        )

        val verified = repository.verifyCredentials(email, password)

        assertNotNull(verified, "Should return user for valid credentials")
        assertEquals(email, verified?.email?.value)
    }

    @Test
    fun `verifyCredentials should return null for wrong password`() = runBlocking {
        val email = "wrongpass-${Uuid.random()}@example.com"

        repository.register(
            email = email,
            password = "CorrectPass123!",
            firstName = "Wrong",
            lastName = "Pass"
        )

        val verified = repository.verifyCredentials(email, "WrongPass123!")

        assertNull(verified, "Should return null for wrong password")
    }

    @Test
    fun `verifyCredentials should return null for non-existent email`() = runBlocking {
        val verified = repository.verifyCredentials("nonexistent@example.com", "AnyPass123!")
        assertNull(verified, "Should return null for non-existent user")
    }

    @Test
    fun `verifyCredentials should return null for inactive user`() = runBlocking {
        val email = "inactive-${Uuid.random()}@example.com"

        val user = repository.register(
            email = email,
            password = "SecurePass123!",
            firstName = "Inactive",
            lastName = "User"
        )

        // Deactivate user
        repository.deactivate(user.id, "Test deactivation")

        val verified = repository.verifyCredentials(email, "SecurePass123!")
        assertNull(verified, "Should return null for inactive user")
    }

    // ===========================================
    // Tests for getMembership()
    // ===========================================

    @Test
    fun `getMembership should return membership for valid user and tenant`() = runBlocking {
        val user = repository.registerWithTenant(
            tenantId = testTenantId!!,
            email = "getmember-${Uuid.random()}@example.com",
            password = "SecurePass123!",
            firstName = "Get",
            lastName = "Member",
            role = UserRole.Accountant
        )

        val membership = repository.getMembership(user.id, testTenantId!!)

        assertNotNull(membership)
        assertEquals(UserRole.Accountant, membership?.role)
    }

    @Test
    fun `getMembership should return null for non-member`() = runBlocking {
        val user = repository.register(
            email = "nonmember-${Uuid.random()}@example.com",
            password = "SecurePass123!",
            firstName = "Non",
            lastName = "Member"
        )

        val membership = repository.getMembership(user.id, testTenantId!!)

        assertNull(membership, "Should return null for non-member")
    }

    // ===========================================
    // Tests for updateRole()
    // ===========================================

    @Test
    fun `updateRole should change user role in tenant`() = runBlocking {
        val user = repository.registerWithTenant(
            tenantId = testTenantId!!,
            email = "updaterole-${Uuid.random()}@example.com",
            password = "SecurePass123!",
            firstName = "Update",
            lastName = "Role",
            role = UserRole.Viewer
        )

        // Update role
        repository.updateRole(user.id, testTenantId!!, UserRole.Admin)

        val membership = repository.getMembership(user.id, testTenantId!!)
        assertEquals(UserRole.Admin, membership?.role, "Role should be updated")
    }
}
