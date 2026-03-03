@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package tech.dokus.backend.services.business

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.database.repository.business.BusinessProfileRecord
import tech.dokus.database.repository.business.BusinessProfileRepository
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.storage.AvatarStorageService
import kotlin.uuid.Uuid

class BusinessProfileServiceMergeTest {
    private val profileRepository = mockk<BusinessProfileRepository>()
    private val jobRepository = mockk<BusinessProfileEnrichmentJobRepository>()
    private val tenantRepository = mockk<TenantRepository>(relaxed = true)
    private val avatarStorageService = mockk<AvatarStorageService>(relaxed = true)

    private val service = BusinessProfileService(
        profileRepository = profileRepository,
        jobRepository = jobRepository,
        tenantRepository = tenantRepository,
        avatarStorageService = avatarStorageService
    )

    @Test
    fun `pinned website is not overwritten by enrichment`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val subjectId = Uuid.random()
        val existing = BusinessProfileRecord(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            websiteUrl = "https://locked.example",
            websitePinned = true
        )
        val captured = slot<BusinessProfileRecord>()

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
    fun `suggested state never auto applies logo`() = kotlinx.coroutines.runBlocking {
        val tenantId = TenantId.generate()
        val subjectId = Uuid.random()
        val existing = BusinessProfileRecord(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = subjectId,
            logoStorageKey = null
        )
        val captured = slot<BusinessProfileRecord>()

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

        assertEquals(null, captured.captured.logoStorageKey)
    }
}
