@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.database.repository.OrganizationRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.ktor.security.AuthInfo
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
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
 * - Organization retrieval and settings
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class OrganizationRemoteServiceImplTest {

    private lateinit var service: OrganizationRemoteServiceImpl
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var authInfoProvider: AuthInfoProvider

    private val testUserId = UserId(Uuid.random().toString())
    private val testOrganizationId = OrganizationId(Uuid.random())

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

    @Test
    fun `createOrganization should create organization and add user as Owner`() = runBlocking {
        // Given
        val orgName = "Test Organization"
        val orgEmail = "org@example.com"
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val mockOrganization = Organization(
            id = testOrganizationId,
            name = orgName,
            email = orgEmail,
            plan = OrganizationPlan.Free,
            status = TenantStatus.Active,
            country = "BE",
            language = Language.En,
            vatNumber = null,
            trialEndsAt = null,
            subscriptionStartedAt = null,
            createdAt = now,
            updatedAt = now
        )

        // Mock auth context
        coEvery {
            authInfoProvider.withAuthInfo<Organization>(any())
        } coAnswers {
            val block = firstArg<suspend AuthInfo.() -> Organization>()
            val mockAuthInfo = mockk<AuthInfo> {
                every { userId } returns testUserId
                every { organizationId } returns null  // User has no org yet
            }
            block.invoke(mockAuthInfo)
        }

        coEvery {
            organizationRepository.create(
                name = orgName,
                email = orgEmail,
                plan = OrganizationPlan.Free,
                country = "BE",
                language = Language.En,
                vatNumber = null
            )
        } returns testOrganizationId

        coEvery { userRepository.addToOrganization(testUserId, testOrganizationId, UserRole.Owner) } just Runs

        coEvery { organizationRepository.findById(testOrganizationId) } returns mockOrganization

        // When
        val result = service.createOrganization(
            name = orgName,
            email = orgEmail,
            plan = OrganizationPlan.Free,
            country = "BE",
            language = Language.En,
            vatNumber = null
        )

        // Then
        assertNotNull(result)
        assertEquals(testOrganizationId, result.id)
        assertEquals(orgName, result.name)

        // Verify organization was created
        coVerify(exactly = 1) {
            organizationRepository.create(
                name = orgName,
                email = orgEmail,
                plan = OrganizationPlan.Free,
                country = "BE",
                language = Language.En,
                vatNumber = null
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
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val mockOrganization = Organization(
            id = testOrganizationId,
            name = "Test",
            email = "test@example.com",
            plan = OrganizationPlan.Free,
            status = TenantStatus.Active,
            country = "BE",
            language = Language.En,
            vatNumber = null,
            trialEndsAt = null,
            subscriptionStartedAt = null,
            createdAt = now,
            updatedAt = now
        )

        coEvery {
            authInfoProvider.withAuthInfo<Organization>(any())
        } coAnswers {
            val block = firstArg<suspend AuthInfo.() -> Organization>()
            val mockAuthInfo = mockk<AuthInfo> {
                every { userId } returns testUserId
                every { organizationId } returns null
            }
            block.invoke(mockAuthInfo)
        }

        coEvery { organizationRepository.create(any(), any(), any(), any(), any(), any()) } returns testOrganizationId
        coEvery { userRepository.addToOrganization(any(), any(), any()) } just Runs
        coEvery { organizationRepository.findById(any()) } returns mockOrganization

        // When
        service.createOrganization(
            name = "Test",
            email = "test@example.com",
            plan = OrganizationPlan.Free,
            country = "BE",
            language = Language.En,
            vatNumber = null
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
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val mockOrganization = Organization(
            id = testOrganizationId,
            name = "Premium Org",
            email = "premium@example.com",
            plan = OrganizationPlan.Premium,
            status = TenantStatus.Active,
            country = "NL",
            language = Language.Nl,
            vatNumber = null,
            trialEndsAt = null,
            subscriptionStartedAt = null,
            createdAt = now,
            updatedAt = now
        )

        coEvery {
            authInfoProvider.withAuthInfo<Organization>(any())
        } coAnswers {
            val block = firstArg<suspend AuthInfo.() -> Organization>()
            val mockAuthInfo = mockk<AuthInfo> {
                every { userId } returns testUserId
                every { organizationId } returns null
            }
            block.invoke(mockAuthInfo)
        }

        val planSlot = slot<OrganizationPlan>()
        val countrySlot = slot<String>()
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
            name = "Premium Org",
            email = "premium@example.com",
            plan = OrganizationPlan.Premium,
            country = "NL",
            language = Language.Nl,
            vatNumber = null
        )

        // Then
        assertEquals(OrganizationPlan.Premium, planSlot.captured)
        assertEquals("NL", countrySlot.captured)
        assertEquals(Language.Nl, languageSlot.captured)
    }

    @Test
    fun `user creating organization becomes Owner not Admin or other role`() = runBlocking {
        // Given
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val mockOrganization = Organization(
            id = testOrganizationId,
            name = "Test",
            email = "test@example.com",
            plan = OrganizationPlan.Free,
            status = TenantStatus.Active,
            country = "BE",
            language = Language.En,
            vatNumber = null,
            trialEndsAt = null,
            subscriptionStartedAt = null,
            createdAt = now,
            updatedAt = now
        )

        coEvery {
            authInfoProvider.withAuthInfo<Organization>(any())
        } coAnswers {
            val block = firstArg<suspend AuthInfo.() -> Organization>()
            val mockAuthInfo = mockk<AuthInfo> {
                every { userId } returns testUserId
                every { organizationId } returns null
            }
            block.invoke(mockAuthInfo)
        }

        coEvery { organizationRepository.create(any(), any(), any(), any(), any(), any()) } returns testOrganizationId

        val roleSlot = slot<UserRole>()
        coEvery { userRepository.addToOrganization(any(), any(), capture(roleSlot)) } just Runs

        coEvery { organizationRepository.findById(any()) } returns mockOrganization

        // When
        service.createOrganization(
            name = "Test",
            email = "test@example.com",
            plan = OrganizationPlan.Free,
            country = "BE",
            language = Language.En,
            vatNumber = null
        )

        // Then - verify user is assigned Owner role, not any other role
        assertEquals(UserRole.Owner, roleSlot.captured, "User should become Owner when creating organization")
    }
}
