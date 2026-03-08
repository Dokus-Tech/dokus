@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.services.business

import io.mockk.coEvery
import kotlin.uuid.ExperimentalUuidApi
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.database.repository.business.BusinessProfileRecord
import tech.dokus.database.repository.business.BusinessProfileRepository
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.Name
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.utils.json
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class BusinessProfileServiceProjectionTest {
    private val profileRepository = mockk<BusinessProfileRepository>()
    private val jobRepository = mockk<BusinessProfileEnrichmentJobRepository>(relaxed = true)
    private val tenantRepository = mockk<TenantRepository>(relaxed = true)

    private val service = BusinessProfileService(
        profileRepository = profileRepository,
        jobRepository = jobRepository,
        tenantRepository = tenantRepository
    )

    @Test
    fun `project tenant includes business profile fields`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val tenant = Tenant(
            id = tenantId,
            type = TenantType.Company,
            legalName = LegalName("Acme NV"),
            displayName = DisplayName("Acme"),
            subscription = SubscriptionTier.Core,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789"),
            createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            updatedAt = LocalDateTime.parse("2026-01-01T00:00:00")
        )
        val profile = BusinessProfileRecord(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Tenant,
            subjectId = tenantId.value,
            websiteUrl = "https://acme.example",
            businessSummary = "Acme summary",
            businessActivitiesJson = json.encodeToString(listOf("logistics")),
            verificationState = BusinessProfileVerificationState.Verified
        )
        coEvery {
            profileRepository.getBySubject(tenantId, BusinessProfileSubjectType.Tenant, tenantId.value)
        } returns profile

        val projected = service.projectTenant(tenant)
        assertEquals("https://acme.example", projected.websiteUrl)
        assertEquals("Acme summary", projected.businessSummary)
        assertEquals(listOf("logistics"), projected.businessActivities)
        assertEquals(true, projected.businessProfileVerified)
    }

    @Test
    fun `project contacts includes business fields and avatar`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val contactId = ContactId.generate()
        val now = LocalDateTime.parse("2026-01-01T00:00:00")
        val contact = ContactDto(
            id = contactId,
            tenantId = tenantId,
            name = Name("Beta SRL"),
            createdAt = now,
            updatedAt = now
        )

        val profile = BusinessProfileRecord(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = contactId.value,
            websiteUrl = "https://beta.example",
            businessSummary = "Beta summary",
            businessActivitiesJson = json.encodeToString(listOf("consulting")),
            verificationState = BusinessProfileVerificationState.Verified,
            logoStorageKey = "logo/contact-beta"
        )
        coEvery {
            profileRepository.getBySubjects(
                tenantId = tenantId,
                subjectType = BusinessProfileSubjectType.Contact,
                subjectIds = listOf(contactId.value)
            )
        } returns mapOf(contactId.value to profile)

        val projected = service.projectContacts(tenantId, listOf(contact)).single()
        assertEquals("https://beta.example", projected.websiteUrl)
        assertEquals("Beta summary", projected.businessSummary)
        assertEquals(listOf("consulting"), projected.businessActivities)
        assertEquals(true, projected.businessProfileVerified)
        assertEquals("/api/v1/contacts/${contactId.value}/avatar/small.webp?v=contact-beta", projected.avatar?.small)
        assertEquals("/api/v1/contacts/${contactId.value}/avatar/medium.webp?v=contact-beta", projected.avatar?.medium)
        assertEquals("/api/v1/contacts/${contactId.value}/avatar/large.webp?v=contact-beta", projected.avatar?.large)
    }

    @Test
    fun `build tenant avatar thumbnail includes version query from storage key`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        coEvery { tenantRepository.getAvatarStorageKey(tenantId) } returns "avatars/tenants/$tenantId/abc"

        val thumbnail = service.buildTenantAvatarThumbnail(tenantId)

        assertEquals("/api/v1/tenants/$tenantId/avatar/small.webp?v=abc", thumbnail?.small)
        assertEquals("/api/v1/tenants/$tenantId/avatar/medium.webp?v=abc", thumbnail?.medium)
        assertEquals("/api/v1/tenants/$tenantId/avatar/large.webp?v=abc", thumbnail?.large)
    }
}
