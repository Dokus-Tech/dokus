@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.database.repository.TenantRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.AuthenticationInfo
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.withAuthContext
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for TenantRemoteServiceImpl
 *
 * Tests cover:
 * - Creating tenant and auto-adding creator as Owner
 * - Proper authentication context handling
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TenantRemoteServiceImplTest {

    private lateinit var service: TenantRemoteServiceImpl
    private lateinit var tenantRepository: TenantRepository
    private lateinit var userRepository: UserRepository
    private lateinit var authInfoProvider: AuthInfoProvider

    private val testUserId = UserId(Uuid.random().toString())
    private val testTenantId = TenantId(Uuid.random())
    // Placeholder tenant ID for auth context (a user may have an existing tenant for auth purposes)
    private val existingTenantId = TenantId(Uuid.random())

    @BeforeEach
    fun setup() {
        tenantRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        authInfoProvider = mockk(relaxed = true)

        service = TenantRemoteServiceImpl(
            tenantRepository = tenantRepository,
            userRepository = userRepository,
            authInfoProvider = authInfoProvider
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    private fun createMockAuthInfo(): AuthenticationInfo {
        // AuthenticationInfo requires a non-null tenantId, so we use a placeholder
        // In practice, a user without any tenant would need special handling
        return AuthenticationInfo(
            userId = testUserId,
            email = "test@example.com",
            name = "Test User",
            tenantId = existingTenantId,
            roles = setOf("owner")
        )
    }

    private fun createMockTenant(
        id: TenantId = testTenantId,
        type: TenantType = TenantType.Company,
        legalName: String = "Test Legal Name",
        displayName: String = "Test Display Name",
        plan: TenantPlan = TenantPlan.Free
    ): Tenant {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return Tenant(
            id = id,
            type = type,
            legalName = LegalName(legalName),
            displayName = DisplayName(displayName),
            plan = plan,
            status = TenantStatus.Active,
            country = Country.Belgium,
            language = Language.En,
            vatNumber = null,
            trialEndsAt = null,
            subscriptionStartedAt = null,
            createdAt = now,
            updatedAt = now
        )
    }

    @Test
    fun `createTenant should create tenant and add user as Owner`() = runBlocking {
        // Given
        val legalName = "Test Legal Name"
        val displayName = "Test Display Name"
        val mockTenant = createMockTenant(legalName = legalName, displayName = displayName)

        // Mock auth context
        coEvery {
            authInfoProvider.withAuthInfo<Tenant>(any())
        } coAnswers {
            val block = firstArg<suspend () -> Tenant>()
            withAuthContext(createMockAuthInfo()) {
                block.invoke()
            }
        }

        coEvery {
            tenantRepository.create(
                type = TenantType.Company,
                legalName = LegalName(legalName),
                displayName = DisplayName(displayName),
                plan = TenantPlan.Free,
                country = Country.Belgium,
                language = Language.En,
                vatNumber = any()
            )
        } returns testTenantId

        coEvery { userRepository.addToTenant(testUserId, testTenantId, UserRole.Owner) } just Runs

        coEvery { tenantRepository.findById(testTenantId) } returns mockTenant

        // When
        val result = service.createTenant(
            type = TenantType.Company,
            legalName = LegalName(legalName),
            displayName = DisplayName(displayName),
            plan = TenantPlan.Free,
            country = Country.Belgium,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789")
        )

        // Then
        assertNotNull(result)
        assertEquals(testTenantId, result.id)
        assertEquals(LegalName(legalName), result.legalName)
        assertEquals(DisplayName(displayName), result.displayName)

        // Verify tenant was created
        coVerify(exactly = 1) {
            tenantRepository.create(
                type = TenantType.Company,
                legalName = LegalName(legalName),
                displayName = DisplayName(displayName),
                plan = TenantPlan.Free,
                country = Country.Belgium,
                language = Language.En,
                vatNumber = any()
            )
        }

        // Verify user was added as Owner
        coVerify(exactly = 1) {
            userRepository.addToTenant(testUserId, testTenantId, UserRole.Owner)
        }
    }

    @Test
    fun `createTenant should add user as Owner in correct order`() = runBlocking {
        // Given
        val mockTenant = createMockTenant(legalName = "Test")

        coEvery {
            authInfoProvider.withAuthInfo<Tenant>(any())
        } coAnswers {
            val block = firstArg<suspend () -> Tenant>()
            withAuthContext(createMockAuthInfo()) {
                block.invoke()
            }
        }

        coEvery { tenantRepository.create(any(), any(), any(), any(), any(), any(), any()) } returns testTenantId
        coEvery { userRepository.addToTenant(any(), any(), any()) } just Runs
        coEvery { tenantRepository.findById(any()) } returns mockTenant

        // When
        service.createTenant(
            type = TenantType.Freelancer,
            legalName = LegalName("Test"),
            displayName = DisplayName("Test Display"),
            plan = TenantPlan.Free,
            country = Country.Belgium,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789")
        )

        // Then - verify order: create tenant first, then add membership
        coVerifyOrder {
            tenantRepository.create(any(), any(), any(), any(), any(), any(), any())
            userRepository.addToTenant(testUserId, testTenantId, UserRole.Owner)
            tenantRepository.findById(testTenantId)
        }
    }

    @Test
    fun `createTenant should use custom plan when specified`() = runBlocking {
        // Given
        val mockTenant = createMockTenant(
            legalName = "Professional Org",
            plan = TenantPlan.Professional
        )

        coEvery {
            authInfoProvider.withAuthInfo<Tenant>(any())
        } coAnswers {
            val block = firstArg<suspend () -> Tenant>()
            withAuthContext(createMockAuthInfo()) {
                block.invoke()
            }
        }

        val planSlot = slot<TenantPlan>()
        val countrySlot = slot<Country>()
        val languageSlot = slot<Language>()

        coEvery {
            tenantRepository.create(
                type = any(),
                legalName = any(),
                displayName = any(),
                plan = capture(planSlot),
                country = capture(countrySlot),
                language = capture(languageSlot),
                vatNumber = any()
            )
        } returns testTenantId

        coEvery { userRepository.addToTenant(any(), any(), any()) } just Runs
        coEvery { tenantRepository.findById(any()) } returns mockTenant

        // When
        service.createTenant(
            type = TenantType.Company,
            legalName = LegalName("Professional Org"),
            displayName = DisplayName("Pro Display"),
            plan = TenantPlan.Professional,
            country = Country.Netherlands,
            language = Language.Nl,
            vatNumber = VatNumber("NL012345678B01")
        )

        // Then
        assertEquals(TenantPlan.Professional, planSlot.captured)
        assertEquals(Country.Netherlands, countrySlot.captured)
        assertEquals(Language.Nl, languageSlot.captured)
    }

    @Test
    fun `user creating tenant becomes Owner not Admin or other role`() = runBlocking {
        // Given
        val mockTenant = createMockTenant()

        coEvery {
            authInfoProvider.withAuthInfo<Tenant>(any())
        } coAnswers {
            val block = firstArg<suspend () -> Tenant>()
            withAuthContext(createMockAuthInfo()) {
                block.invoke()
            }
        }

        coEvery { tenantRepository.create(any(), any(), any(), any(), any(), any(), any()) } returns testTenantId

        val roleSlot = slot<UserRole>()
        coEvery { userRepository.addToTenant(any(), any(), capture(roleSlot)) } just Runs

        coEvery { tenantRepository.findById(any()) } returns mockTenant

        // When
        service.createTenant(
            type = TenantType.Company,
            legalName = LegalName("Test"),
            displayName = DisplayName("Test Display"),
            plan = TenantPlan.Free,
            country = Country.Belgium,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789")
        )

        // Then - verify user is assigned Owner role, not any other role
        assertEquals(UserRole.Owner, roleSlot.captured, "User should become Owner when creating tenant")
    }
}
