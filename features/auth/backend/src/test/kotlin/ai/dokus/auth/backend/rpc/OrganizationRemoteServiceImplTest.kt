@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.database.repository.OrganizationRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.AuthenticationInfo
import ai.dokus.foundation.domain.model.Organization
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
 * Tests for OrganizationRemoteServiceImpl
 *
 * Tests cover:
 * - Creating organization and auto-adding creator as Owner
 * - Proper authentication context handling
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class OrganizationRemoteServiceImplTest {

    private lateinit var service: OrganizationRemoteServiceImpl
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var authInfoProvider: AuthInfoProvider

    private val testUserId = UserId(Uuid.random().toString())
    private val testOrganizationId = OrganizationId(Uuid.random())
    // Placeholder org ID for auth context (a user may have an existing org for auth purposes)
    private val existingOrgId = OrganizationId(Uuid.random())

    @BeforeEach
    fun setup() {
        organizationRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        authInfoProvider = mockk(relaxed = true)

        service = OrganizationRemoteServiceImpl(
            organizationService = organizationRepository,
            userRepository = userRepository,
            authInfoProvider = authInfoProvider
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    private fun createMockAuthInfo(): AuthenticationInfo {
        // AuthenticationInfo requires a non-null organizationId, so we use a placeholder
        // In practice, a user without any organization would need special handling
        return AuthenticationInfo(
            userId = testUserId,
            email = "test@example.com",
            name = "Test User",
            organizationId = existingOrgId,
            roles = setOf("owner")
        )
    }

    private fun createMockOrganization(
        id: OrganizationId = testOrganizationId,
        name: String = "Test Organization",
        email: String = "org@example.com",
        plan: OrganizationPlan = OrganizationPlan.Free
    ): Organization {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return Organization(
            id = id,
            legalName = LegalName(name),
            email = Email(email),
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
    fun `createOrganization should create organization and add user as Owner`() = runBlocking {
        // Given
        val orgName = "Test Organization"
        val orgEmail = "org@example.com"
        val mockOrganization = createMockOrganization(name = orgName, email = orgEmail)

        // Mock auth context
        coEvery {
            authInfoProvider.withAuthInfo<Organization>(any())
        } coAnswers {
            val block = firstArg<suspend () -> Organization>()
            withAuthContext(createMockAuthInfo()) {
                block.invoke()
            }
        }

        coEvery {
            organizationRepository.create(
                name = LegalName(orgName),
                email = Email(orgEmail),
                plan = OrganizationPlan.Free,
                country = Country.Belgium,
                language = Language.En,
                vatNumber = any()
            )
        } returns testOrganizationId

        coEvery { userRepository.addToOrganization(testUserId, testOrganizationId, UserRole.Owner) } just Runs

        coEvery { organizationRepository.findById(testOrganizationId) } returns mockOrganization

        // When
        val result = service.createOrganization(
            legalName = LegalName(orgName),
            email = Email(orgEmail),
            plan = OrganizationPlan.Free,
            country = Country.Belgium,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789")
        )

        // Then
        assertNotNull(result)
        assertEquals(testOrganizationId, result.id)
        assertEquals(LegalName(orgName), result.legalName)

        // Verify organization was created
        coVerify(exactly = 1) {
            organizationRepository.create(
                name = LegalName(orgName),
                email = Email(orgEmail),
                plan = OrganizationPlan.Free,
                country = Country.Belgium,
                language = Language.En,
                vatNumber = any()
            )
        }

        // Verify user was added as Owner
        coVerify(exactly = 1) {
            userRepository.addToOrganization(testUserId, testOrganizationId, UserRole.Owner)
        }
    }

    @Test
    fun `createOrganization should add user as Owner in correct order`() = runBlocking {
        // Given
        val mockOrganization = createMockOrganization(name = "Test")

        coEvery {
            authInfoProvider.withAuthInfo<Organization>(any())
        } coAnswers {
            val block = firstArg<suspend () -> Organization>()
            withAuthContext(createMockAuthInfo()) {
                block.invoke()
            }
        }

        coEvery { organizationRepository.create(any(), any(), any(), any(), any(), any()) } returns testOrganizationId
        coEvery { userRepository.addToOrganization(any(), any(), any()) } just Runs
        coEvery { organizationRepository.findById(any()) } returns mockOrganization

        // When
        service.createOrganization(
            legalName = LegalName("Test"),
            email = Email("test@example.com"),
            plan = OrganizationPlan.Free,
            country = Country.Belgium,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789")
        )

        // Then - verify order: create org first, then add membership
        coVerifyOrder {
            organizationRepository.create(any(), any(), any(), any(), any(), any())
            userRepository.addToOrganization(testUserId, testOrganizationId, UserRole.Owner)
            organizationRepository.findById(testOrganizationId)
        }
    }

    @Test
    fun `createOrganization should use custom plan when specified`() = runBlocking {
        // Given
        val mockOrganization = createMockOrganization(
            name = "Professional Org",
            plan = OrganizationPlan.Professional
        )

        coEvery {
            authInfoProvider.withAuthInfo<Organization>(any())
        } coAnswers {
            val block = firstArg<suspend () -> Organization>()
            withAuthContext(createMockAuthInfo()) {
                block.invoke()
            }
        }

        val planSlot = slot<OrganizationPlan>()
        val countrySlot = slot<Country>()
        val languageSlot = slot<Language>()

        coEvery {
            organizationRepository.create(
                name = any(),
                email = any(),
                plan = capture(planSlot),
                country = capture(countrySlot),
                language = capture(languageSlot),
                vatNumber = any()
            )
        } returns testOrganizationId

        coEvery { userRepository.addToOrganization(any(), any(), any()) } just Runs
        coEvery { organizationRepository.findById(any()) } returns mockOrganization

        // When
        service.createOrganization(
            legalName = LegalName("Professional Org"),
            email = Email("pro@example.com"),
            plan = OrganizationPlan.Professional,
            country = Country.Netherlands,
            language = Language.Nl,
            vatNumber = VatNumber("NL012345678B01")
        )

        // Then
        assertEquals(OrganizationPlan.Professional, planSlot.captured)
        assertEquals(Country.Netherlands, countrySlot.captured)
        assertEquals(Language.Nl, languageSlot.captured)
    }

    @Test
    fun `user creating organization becomes Owner not Admin or other role`() = runBlocking {
        // Given
        val mockOrganization = createMockOrganization()

        coEvery {
            authInfoProvider.withAuthInfo<Organization>(any())
        } coAnswers {
            val block = firstArg<suspend () -> Organization>()
            withAuthContext(createMockAuthInfo()) {
                block.invoke()
            }
        }

        coEvery { organizationRepository.create(any(), any(), any(), any(), any(), any()) } returns testOrganizationId

        val roleSlot = slot<UserRole>()
        coEvery { userRepository.addToOrganization(any(), any(), capture(roleSlot)) } just Runs

        coEvery { organizationRepository.findById(any()) } returns mockOrganization

        // When
        service.createOrganization(
            legalName = LegalName("Test"),
            email = Email("test@example.com"),
            plan = OrganizationPlan.Free,
            country = Country.Belgium,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789")
        )

        // Then - verify user is assigned Owner role, not any other role
        assertEquals(UserRole.Owner, roleSlot.captured, "User should become Owner when creating organization")
    }
}
