@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.services.business

import io.mockk.coEvery
import kotlin.uuid.ExperimentalUuidApi
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.database.entity.BusinessProfileEntity
import tech.dokus.database.repository.business.BusinessProfileRepository
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.Uuid

class BusinessProfileServiceMergeTest {
    private val profileRepository = mockk<BusinessProfileRepository>()
    private val jobRepository = mockk<BusinessProfileEnrichmentJobRepository>()
    private val tenantRepository = mockk<TenantRepository>(relaxed = true)

    private val service = BusinessProfileService(
        profileRepository = profileRepository,
        jobRepository = jobRepository,
        tenantRepository = tenantRepository
    )

    @Test
    fun `pinned website is not overwritten by enrichment`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val subjectId = Uuid.random()
        val existing = BusinessProfileEntity(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            websiteUrl = "https://locked.example",
            websitePinned = true
        )
        val captured = slot<BusinessProfileEntity>()

        coEvery {
            profileRepository.getBySubject(tenantId, BusinessProfileSubjectType.Contact, subjectId)
        } returns existing
        coJustRun { profileRepository.upsert(capture(captured)) }

        service.applyEnrichment(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            verificationState = BusinessProfileVerificationState.Verified,
            evidenceScore = 80,
            evidenceChecksJson = "[]",
            websiteUrl = "https://new.example",
            businessSummary = "summary",
            businessActivities = listOf("activity"),
            logoStorageKey = "logo-key",
            lastErrorCode = null,
            lastErrorMessage = null
        )

        assertEquals("https://locked.example", captured.captured.websiteUrl)
    }

    @Test
    fun `suggested state applies logo when no existing logo and not pinned`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val subjectId = Uuid.random()
        val existing = BusinessProfileEntity(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            logoStorageKey = null
        )
        val captured = slot<BusinessProfileEntity>()

        coEvery {
            profileRepository.getBySubject(tenantId, BusinessProfileSubjectType.Contact, subjectId)
        } returns existing
        coJustRun { profileRepository.upsert(capture(captured)) }

        service.applyEnrichment(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            verificationState = BusinessProfileVerificationState.Suggested,
            evidenceScore = 45,
            evidenceChecksJson = "[]",
            websiteUrl = "https://suggested.example",
            businessSummary = "summary",
            businessActivities = listOf("activity"),
            logoStorageKey = "auto-logo",
            lastErrorCode = null,
            lastErrorMessage = null
        )

        assertEquals("auto-logo", captured.captured.logoStorageKey)
    }

    @Test
    fun `pinned logo is preserved during suggested enrichment`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val subjectId = Uuid.random()
        val existing = BusinessProfileEntity(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            logoStorageKey = "existing-logo",
            logoPinned = true
        )
        val captured = slot<BusinessProfileEntity>()

        coEvery {
            profileRepository.getBySubject(tenantId, BusinessProfileSubjectType.Contact, subjectId)
        } returns existing
        coJustRun { profileRepository.upsert(capture(captured)) }

        service.applyEnrichment(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            verificationState = BusinessProfileVerificationState.Suggested,
            evidenceScore = 42,
            evidenceChecksJson = "[]",
            websiteUrl = null,
            businessSummary = null,
            businessActivities = null,
            logoStorageKey = "new-logo",
            lastErrorCode = null,
            lastErrorMessage = null
        )

        assertEquals("existing-logo", captured.captured.logoStorageKey)
    }

    @Test
    fun `low confidence keeps fields but stores latest ranking evidence`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val subjectId = Uuid.random()
        val existing = BusinessProfileEntity(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            websiteUrl = "https://kept.example",
            businessSummary = "existing summary",
            businessActivitiesJson = "[\"existing\"]",
            verificationState = BusinessProfileVerificationState.Verified,
            evidenceScore = 88,
            evidenceChecksJson = "{\"old\":true}"
        )
        val captured = slot<BusinessProfileEntity>()

        coEvery {
            profileRepository.getBySubject(tenantId, BusinessProfileSubjectType.Contact, subjectId)
        } returns existing
        coJustRun { profileRepository.upsert(capture(captured)) }

        service.applyEnrichment(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            verificationState = BusinessProfileVerificationState.Unset,
            evidenceScore = 55,
            evidenceChecksJson = "{\"new\":true}",
            websiteUrl = null,
            businessSummary = null,
            businessActivities = null,
            logoStorageKey = null,
            lastErrorCode = "LOW_CONFIDENCE_WEBSITE",
            lastErrorMessage = "Top candidate scored 55"
        )

        assertEquals("https://kept.example", captured.captured.websiteUrl)
        assertEquals("existing summary", captured.captured.businessSummary)
        assertEquals(BusinessProfileVerificationState.Verified, captured.captured.verificationState)
        assertEquals(55, captured.captured.evidenceScore)
        assertEquals("{\"new\":true}", captured.captured.evidenceChecksJson)
    }

    @Test
    fun `null enrichment text does not erase existing unpinned summary and activities`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val subjectId = Uuid.random()
        val existing = BusinessProfileEntity(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            websiteUrl = "https://kept.example",
            businessSummary = "Existing business summary",
            businessActivitiesJson = "[\"existing activity\"]",
            verificationState = BusinessProfileVerificationState.Verified,
            summaryPinned = false,
            activitiesPinned = false
        )
        val captured = slot<BusinessProfileEntity>()

        coEvery {
            profileRepository.getBySubject(tenantId, BusinessProfileSubjectType.Contact, subjectId)
        } returns existing
        coJustRun { profileRepository.upsert(capture(captured)) }

        service.applyEnrichment(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            verificationState = BusinessProfileVerificationState.Suggested,
            evidenceScore = 62,
            evidenceChecksJson = "{\"new\":true}",
            websiteUrl = "https://new.example",
            businessSummary = null,
            businessActivities = null,
            logoStorageKey = null,
            lastErrorCode = null,
            lastErrorMessage = null
        )

        assertEquals("Existing business summary", captured.captured.businessSummary)
        assertEquals("[\"existing activity\"]", captured.captured.businessActivitiesJson)
        assertEquals("https://new.example", captured.captured.websiteUrl)
        assertNull(captured.captured.lastErrorCode)
    }
}
