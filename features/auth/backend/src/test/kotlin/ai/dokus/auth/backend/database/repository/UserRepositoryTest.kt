@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.backend.database.tables.OrganizationMembersTable
import ai.dokus.auth.backend.database.tables.OrganizationTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.OrganizationId
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
 * - User registration without organization
 * - User registration with organization
 * - Adding users to organizations
 * - Credential verification
 * - User profile management
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {

    private lateinit var database: DatabaseFactory
    private lateinit var repository: UserRepository
    private var testOrganizationId: OrganizationId? = null

    @BeforeAll
    fun setup() {
        val appConfig = createTestAppConfig()
        database = DatabaseFactory(
            appConfig = appConfig,
            poolName = "test-pool"
        )

        runBlocking {
            database.init(OrganizationTable, UsersTable, OrganizationMembersTable)
            testOrganizationId = createTestOrganization()
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
            OrganizationMembersTable.deleteAll()
            UsersTable.deleteAll()
        }
    }

    private suspend fun createTestOrganization(): OrganizationId {
        val orgId = Uuid.random()
        dbQuery {
            OrganizationTable.insert {
                it[id] = orgId.toJavaUuid()
                it[name] = "Test Organization"
                it[email] = "org@example.com"
                it[plan] = OrganizationPlan.Free
            }
        }
        return OrganizationId(orgId)
    }

    // ===========================================
    // Tests for register() - without organization
    // ===========================================

    @Test
    fun `register should create user without organization`() = runBlocking {
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
        assertEquals("John", user.firstName, "First name should match")
        assertEquals("Doe", user.lastName, "Last name should match")
        assertTrue(user.isActive, "User should be active by default")

        // Verify no organization membership exists
        val memberships = repository.getUserOrganizations(user.id)
        assertTrue(memberships.isEmpty(), "User should have no organization memberships")
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
    fun `register should handle null first and last name`() = runBlocking {
        val email = "nullname-${Uuid.random()}@example.com"
        val password = "SecurePass123!"

        val user = repository.register(
            email = email,
            password = password,
            firstName = null,
            lastName = null
        )

        assertNotNull(user.id)
        assertNull(user.firstName, "First name should be null")
        assertNull(user.lastName, "Last name should be null")
    }

    // ===========================================
    // Tests for registerWithOrganization()
    // ===========================================

    @Test
    fun `registerWithOrganization should create user with membership`() = runBlocking {
        val email = "withorg-${Uuid.random()}@example.com"
        val password = "SecurePass123!"

        val user = repository.registerWithOrganization(
            organizationId = testOrganizationId!!,
            email = email,
            password = password,
            firstName = "Jane",
            lastName = "Smith",
            role = UserRole.Admin
        )

        assertNotNull(user.id)
        assertEquals(email, user.email.value)

        // Verify organization membership exists
        val memberships = repository.getUserOrganizations(user.id)
        assertEquals(1, memberships.size, "User should have one membership")
        assertEquals(testOrganizationId!!, memberships[0].organizationId)
        assertEquals(UserRole.Admin, memberships[0].role)
    }

    @Test
    fun `registerWithOrganization should fail for duplicate email`() = runBlocking {
        val email = "duporg-${Uuid.random()}@example.com"

        repository.registerWithOrganization(
            organizationId = testOrganizationId!!,
            email = email,
            password = "SecurePass123!",
            firstName = "First",
            lastName = "User",
            role = UserRole.Viewer
        )

        assertThrows<IllegalArgumentException> {
            runBlocking {
                repository.registerWithOrganization(
                    organizationId = testOrganizationId!!,
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
    // Tests for addToOrganization()
    // ===========================================

    @Test
    fun `addToOrganization should add existing user to organization`() = runBlocking {
        val email = "addto-${Uuid.random()}@example.com"

        // Register user without organization
        val user = repository.register(
            email = email,
            password = "SecurePass123!",
            firstName = "Add",
            lastName = "ToOrg"
        )

        // Verify no memberships initially
        val initialMemberships = repository.getUserOrganizations(user.id)
        assertTrue(initialMemberships.isEmpty())

        // Add to organization
        repository.addToOrganization(user.id, testOrganizationId!!, UserRole.Editor)

        // Verify membership was created
        val memberships = repository.getUserOrganizations(user.id)
        assertEquals(1, memberships.size)
        assertEquals(testOrganizationId!!, memberships[0].organizationId)
        assertEquals(UserRole.Editor, memberships[0].role)
    }

    @Test
    fun `user can be added to multiple organizations`() = runBlocking {
        // Create another organization
        val secondOrgId = Uuid.random()
        dbQuery {
            OrganizationTable.insert {
                it[id] = secondOrgId.toJavaUuid()
                it[name] = "Second Organization"
                it[email] = "second@example.com"
                it[plan] = OrganizationPlan.Free
            }
        }
        val secondOrg = OrganizationId(secondOrgId)

        // Register user without organization
        val user = repository.register(
            email = "multiorg-${Uuid.random()}@example.com",
            password = "SecurePass123!",
            firstName = "Multi",
            lastName = "Org"
        )

        // Add to both organizations
        repository.addToOrganization(user.id, testOrganizationId!!, UserRole.Owner)
        repository.addToOrganization(user.id, secondOrg, UserRole.Viewer)

        // Verify memberships
        val memberships = repository.getUserOrganizations(user.id)
        assertEquals(2, memberships.size, "User should belong to two organizations")

        val roles = memberships.associate { it.organizationId to it.role }
        assertEquals(UserRole.Owner, roles[testOrganizationId!!])
        assertEquals(UserRole.Viewer, roles[secondOrg])
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
    fun `getMembership should return membership for valid user and organization`() = runBlocking {
        val user = repository.registerWithOrganization(
            organizationId = testOrganizationId!!,
            email = "getmember-${Uuid.random()}@example.com",
            password = "SecurePass123!",
            firstName = "Get",
            lastName = "Member",
            role = UserRole.Accountant
        )

        val membership = repository.getMembership(user.id, testOrganizationId!!)

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

        val membership = repository.getMembership(user.id, testOrganizationId!!)

        assertNull(membership, "Should return null for non-member")
    }

    // ===========================================
    // Tests for updateRole()
    // ===========================================

    @Test
    fun `updateRole should change user role in organization`() = runBlocking {
        val user = repository.registerWithOrganization(
            organizationId = testOrganizationId!!,
            email = "updaterole-${Uuid.random()}@example.com",
            password = "SecurePass123!",
            firstName = "Update",
            lastName = "Role",
            role = UserRole.Viewer
        )

        // Update role
        repository.updateRole(user.id, testOrganizationId!!, UserRole.Admin)

        val membership = repository.getMembership(user.id, testOrganizationId!!)
        assertEquals(UserRole.Admin, membership?.role, "Role should be updated")
    }
}
