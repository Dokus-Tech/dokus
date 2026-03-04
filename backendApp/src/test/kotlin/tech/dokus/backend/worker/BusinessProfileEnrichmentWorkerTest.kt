@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.worker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.business.BusinessProfileEvidenceGate
import tech.dokus.backend.services.business.BusinessProfileEvidenceResult
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.backend.services.business.BusinessWebsiteCrawlResult
import tech.dokus.backend.services.business.BusinessWebsiteProbe
import tech.dokus.backend.services.business.CrawledBusinessPage
import tech.dokus.backend.services.business.EvidenceCheckDecision
import tech.dokus.backend.services.business.EvidenceGateOutcome
import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJob
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.database.repository.business.BusinessProfileRecord
import tech.dokus.database.repository.business.BusinessProfileRepository
import tech.dokus.database.repository.contacts.ContactAddressRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.features.ai.agents.BusinessProfileEnrichmentAgent
import tech.dokus.features.ai.models.BusinessDiscoveryStatus
import tech.dokus.features.ai.models.BusinessProfileDiscoveryResult
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import tech.dokus.foundation.backend.storage.AvatarStorageService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class BusinessProfileEnrichmentWorkerTest {
    private val config = BusinessProfileEnrichmentConfig(
        enabled = true,
        pollingIntervalMs = 10_000,
        batchSize = 2,
        maxAttempts = 5,
        staleLeaseMinutes = 15,
        maxPages = 5,
        serperApiKey = "test",
        serperBaseUrl = "https://google.serper.dev/search",
        ignoreRobots = false
    )

    private val jobRepository = mockk<BusinessProfileEnrichmentJobRepository>()
    private val profileRepository = mockk<BusinessProfileRepository>()
    private val businessProfileService = mockk<BusinessProfileService>()
    private val tenantRepository = mockk<TenantRepository>()
    private val addressRepository = mockk<AddressRepository>()
    private val contactRepository = mockk<ContactRepository>(relaxed = true)
    private val contactAddressRepository = mockk<ContactAddressRepository>(relaxed = true)
    private val avatarStorageService = mockk<AvatarStorageService>(relaxed = true)
    private val enrichmentAgent = mockk<BusinessProfileEnrichmentAgent>()
    private val websiteProbe = mockk<BusinessWebsiteProbe>()
    private val evidenceGate = mockk<BusinessProfileEvidenceGate>()

    @Test
    fun `low evidence stores error and does not persist candidate fields`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)

        coEvery {
            enrichmentAgent.enrich(any())
        } returns BusinessProfileDiscoveryResult(
            status = BusinessDiscoveryStatus.Found,
            candidateWebsiteUrl = "https://acme.example",
            businessSummary = "Acme summary",
            activities = listOf("Logistics"),
            confidence = 0.7
        )
        coEvery { websiteProbe.crawl("https://acme.example", config.maxPages) } returns BusinessWebsiteCrawlResult(
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://acme.example",
                    textContent = "Acme",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            ),
            blockedByRobots = false
        )
        coEvery { evidenceGate.evaluate(any()) } returns BusinessProfileEvidenceResult(
            outcome = EvidenceGateOutcome.SKIP,
            verificationState = BusinessProfileVerificationState.Unset,
            evidenceScore = 20,
            checks = emptyList()
        )
        coEvery {
            businessProfileService.applyEnrichment(
                tenantId = any(),
                subjectType = any(),
                subjectId = any(),
                verificationState = any(),
                evidenceScore = any(),
                evidenceChecksJson = any(),
                websiteUrl = any(),
                businessSummary = any(),
                businessActivities = any(),
                logoStorageKey = any(),
                lastErrorCode = any(),
                lastErrorMessage = any()
            )
        } returns BusinessProfileRecord(
            tenantId = job.tenantId,
            subjectType = job.subjectType,
            subjectId = job.subjectId
        )

        val worker = createWorker()
        worker.processBatchForTest()

        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Unset,
                evidenceScore = 20,
                evidenceChecksJson = any(),
                websiteUrl = null,
                businessSummary = null,
                businessActivities = null,
                logoStorageKey = null,
                lastErrorCode = "LOW_EVIDENCE",
                lastErrorMessage = "Evidence score below threshold"
            )
        }
    }

    @Test
    fun `verified enrichment never overwrites existing logo`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)

        coEvery {
            enrichmentAgent.enrich(any())
        } returns BusinessProfileDiscoveryResult(
            status = BusinessDiscoveryStatus.Found,
            candidateWebsiteUrl = "https://acme.example",
            businessSummary = "Acme summary",
            activities = listOf("Logistics"),
            logoUrl = "https://acme.example/logo.png",
            confidence = 0.95
        )
        coEvery { websiteProbe.crawl("https://acme.example", config.maxPages) } returns BusinessWebsiteCrawlResult(
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://acme.example",
                    textContent = "Acme",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = listOf("https://acme.example/logo.png")
                )
            ),
            blockedByRobots = false
        )
        coEvery { evidenceGate.evaluate(any()) } returns BusinessProfileEvidenceResult(
            outcome = EvidenceGateOutcome.AUTO_PERSIST,
            verificationState = BusinessProfileVerificationState.Verified,
            evidenceScore = 75,
            checks = listOf(
                EvidenceCheckDecision(
                    check = tech.dokus.domain.enums.BusinessProfileEvidenceCheck.DomainContainsCompanyName,
                    passed = true,
                    score = 30
                )
            )
        )
        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns BusinessProfileRecord(
            tenantId = job.tenantId,
            subjectType = job.subjectType,
            subjectId = job.subjectId,
            logoStorageKey = "avatars/existing"
        )
        coEvery {
            businessProfileService.applyEnrichment(
                tenantId = any(),
                subjectType = any(),
                subjectId = any(),
                verificationState = any(),
                evidenceScore = any(),
                evidenceChecksJson = any(),
                websiteUrl = any(),
                businessSummary = any(),
                businessActivities = any(),
                logoStorageKey = any(),
                lastErrorCode = any(),
                lastErrorMessage = any()
            )
        } returns BusinessProfileRecord(
            tenantId = job.tenantId,
            subjectType = job.subjectType,
            subjectId = job.subjectId
        )

        val worker = createWorker()
        worker.processBatchForTest()

        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Verified,
                evidenceScore = 75,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = "Acme summary",
                businessActivities = listOf("Logistics"),
                logoStorageKey = null,
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
        coVerify(exactly = 0) { avatarStorageService.uploadAvatar(any(), any(), any()) }
    }

    @Test
    fun `suggested enrichment uploads and persists logo when available`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null

        coEvery { enrichmentAgent.enrich(any()) } returns BusinessProfileDiscoveryResult(
            status = BusinessDiscoveryStatus.Found,
            candidateWebsiteUrl = "https://acme.example",
            businessSummary = "Acme summary",
            activities = listOf("Logistics"),
            logoUrl = "https://acme.example/logo.png",
            confidence = 0.9
        )
        coEvery { websiteProbe.crawl("https://acme.example", config.maxPages) } returns BusinessWebsiteCrawlResult(
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://acme.example",
                    textContent = "Acme",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = listOf("https://acme.example/logo.png")
                )
            ),
            blockedByRobots = false
        )
        coEvery { evidenceGate.evaluate(any()) } returns BusinessProfileEvidenceResult(
            outcome = EvidenceGateOutcome.PERSIST_AS_SUGGESTED,
            verificationState = BusinessProfileVerificationState.Suggested,
            evidenceScore = 45,
            checks = emptyList()
        )
        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { websiteProbe.downloadImage("https://acme.example/logo.png") } returns
            tech.dokus.backend.services.business.DownloadedBusinessImage(
                bytes = byteArrayOf(1, 2, 3),
                contentType = "image/png"
            )
        coEvery {
            avatarStorageService.uploadAvatar(job.tenantId, any(), "image/png")
        } returns AvatarStorageService.AvatarUploadResult(
            storageKeyPrefix = "avatars/new-logo",
            avatar = Thumbnail(small = "s", medium = "m", large = "l"),
            sizeBytes = 3
        )
        coEvery { tenantRepository.updateAvatarStorageKey(job.tenantId, "avatars/new-logo") } returns Unit
        coEvery {
            businessProfileService.applyEnrichment(
                tenantId = any(),
                subjectType = any(),
                subjectId = any(),
                verificationState = any(),
                evidenceScore = any(),
                evidenceChecksJson = any(),
                websiteUrl = any(),
                businessSummary = any(),
                businessActivities = any(),
                logoStorageKey = any(),
                lastErrorCode = any(),
                lastErrorMessage = any()
            )
        } returns BusinessProfileRecord(
            tenantId = job.tenantId,
            subjectType = job.subjectType,
            subjectId = job.subjectId
        )

        val worker = createWorker()
        worker.processBatchForTest()

        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Suggested,
                evidenceScore = 45,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = "Acme summary",
                businessActivities = listOf("Logistics"),
                logoStorageKey = "avatars/new-logo",
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
    }

    @Test
    fun `uses non-aggregator search result fallback when discovery is not found`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)
        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null

        coEvery { enrichmentAgent.enrich(any()) } returns BusinessProfileDiscoveryResult(
            status = BusinessDiscoveryStatus.NotFound,
            candidateWebsiteUrl = null,
            businessSummary = null,
            activities = emptyList(),
            logoUrl = null,
            confidence = 0.4,
            searchResultUrls = listOf(
                "https://www.linkedin.com/company/invoid-vision/",
                "https://invoid.vision/",
                "https://www.facebook.com/invoidvision"
            )
        )
        coEvery { websiteProbe.crawl("https://invoid.vision/", config.maxPages) } returns BusinessWebsiteCrawlResult(
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://invoid.vision/",
                    textContent = "Invoid Vision",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            ),
            blockedByRobots = false
        )
        coEvery { evidenceGate.evaluate(any()) } returns BusinessProfileEvidenceResult(
            outcome = EvidenceGateOutcome.PERSIST_AS_SUGGESTED,
            verificationState = BusinessProfileVerificationState.Suggested,
            evidenceScore = 35,
            checks = emptyList()
        )
        coEvery {
            businessProfileService.applyEnrichment(
                tenantId = any(),
                subjectType = any(),
                subjectId = any(),
                verificationState = any(),
                evidenceScore = any(),
                evidenceChecksJson = any(),
                websiteUrl = any(),
                businessSummary = any(),
                businessActivities = any(),
                logoStorageKey = any(),
                lastErrorCode = any(),
                lastErrorMessage = any()
            )
        } returns BusinessProfileRecord(
            tenantId = job.tenantId,
            subjectType = job.subjectType,
            subjectId = job.subjectId
        )

        val worker = createWorker()
        worker.processBatchForTest()

        coVerify(exactly = 1) { websiteProbe.crawl("https://invoid.vision/", config.maxPages) }
        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Suggested,
                evidenceScore = 35,
                evidenceChecksJson = any(),
                websiteUrl = "https://invoid.vision/",
                businessSummary = null,
                businessActivities = emptyList(),
                logoStorageKey = null,
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
    }

    @Test
    fun `uses deterministic serper fallback when discovery has no candidates`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)
        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null

        coEvery { enrichmentAgent.enrich(any()) } returns BusinessProfileDiscoveryResult(
            status = BusinessDiscoveryStatus.NotFound,
            candidateWebsiteUrl = null,
            businessSummary = null,
            activities = emptyList(),
            logoUrl = null,
            confidence = 0.35,
            searchResultUrls = emptyList()
        )
        coEvery {
            websiteProbe.searchWebsiteCandidates(
                companyName = "Acme Logistics",
                vatNumber = "BE0123456789",
                country = "BE",
                maxResults = 5
            )
        } returns listOf(
            "https://www.linkedin.com/company/acme-logistics/",
            "https://invoid.vision/"
        )
        coEvery { websiteProbe.crawl("https://invoid.vision/", config.maxPages) } returns BusinessWebsiteCrawlResult(
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://invoid.vision/",
                    textContent = "Invoid Vision",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            ),
            blockedByRobots = false
        )
        coEvery { evidenceGate.evaluate(any()) } returns BusinessProfileEvidenceResult(
            outcome = EvidenceGateOutcome.PERSIST_AS_SUGGESTED,
            verificationState = BusinessProfileVerificationState.Suggested,
            evidenceScore = 40,
            checks = emptyList()
        )
        coEvery {
            businessProfileService.applyEnrichment(
                tenantId = any(),
                subjectType = any(),
                subjectId = any(),
                verificationState = any(),
                evidenceScore = any(),
                evidenceChecksJson = any(),
                websiteUrl = any(),
                businessSummary = any(),
                businessActivities = any(),
                logoStorageKey = any(),
                lastErrorCode = any(),
                lastErrorMessage = any()
            )
        } returns BusinessProfileRecord(
            tenantId = job.tenantId,
            subjectType = job.subjectType,
            subjectId = job.subjectId
        )

        val worker = createWorker()
        worker.processBatchForTest()

        coVerify(exactly = 1) {
            websiteProbe.searchWebsiteCandidates(
                companyName = "Acme Logistics",
                vatNumber = "BE0123456789",
                country = "BE",
                maxResults = 5
            )
        }
        coVerify(exactly = 1) { websiteProbe.crawl("https://invoid.vision/", config.maxPages) }
        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Suggested,
                evidenceScore = 40,
                evidenceChecksJson = any(),
                websiteUrl = "https://invoid.vision/",
                businessSummary = null,
                businessActivities = emptyList(),
                logoStorageKey = null,
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
    }

    @Test
    fun `failed processing schedules retry with incremented attempt count`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 2)
        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)

        coEvery { enrichmentAgent.enrich(any()) } throws IllegalStateException("boom")
        coEvery {
            jobRepository.scheduleRetry(
                jobId = job.id,
                attemptCount = 3,
                nextAttemptAt = any(),
                error = "boom"
            )
        } returns Result.success(true)

        val worker = createWorker()
        worker.processBatchForTest()

        coVerify(exactly = 1) {
            jobRepository.scheduleRetry(
                jobId = job.id,
                attemptCount = 3,
                nextAttemptAt = any(),
                error = "boom"
            )
        }
    }

    private fun createWorker(): BusinessProfileEnrichmentWorker {
        return BusinessProfileEnrichmentWorker(
            config = config,
            jobRepository = jobRepository,
            profileRepository = profileRepository,
            businessProfileService = businessProfileService,
            tenantRepository = tenantRepository,
            addressRepository = addressRepository,
            contactRepository = contactRepository,
            contactAddressRepository = contactAddressRepository,
            avatarStorageService = avatarStorageService,
            enrichmentAgent = enrichmentAgent,
            websiteProbe = websiteProbe,
            evidenceGate = evidenceGate
        )
    }

    private fun sampleTenant(tenantId: tech.dokus.domain.ids.TenantId): Tenant {
        return Tenant(
            id = tenantId,
            type = TenantType.Company,
            legalName = LegalName("Acme Logistics"),
            displayName = DisplayName("Acme"),
            subscription = SubscriptionTier.Core,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789"),
            createdAt = LocalDateTime(2026, 1, 1, 10, 0),
            updatedAt = LocalDateTime(2026, 1, 1, 10, 0)
        )
    }

    private fun sampleAddress(tenantId: tech.dokus.domain.ids.TenantId): Address {
        return Address(
            id = tech.dokus.domain.ids.AddressId.generate(),
            tenantId = tenantId,
            city = "Brussels",
            postalCode = "1000",
            country = "BE",
            createdAt = LocalDateTime(2026, 1, 1, 10, 0),
            updatedAt = LocalDateTime(2026, 1, 1, 10, 0)
        )
    }

    private fun sampleJob(subjectType: BusinessProfileSubjectType, attemptCount: Int): BusinessProfileEnrichmentJob {
        val tenantId = tech.dokus.domain.ids.TenantId.generate()
        val now = LocalDateTime(2026, 1, 1, 10, 0)
        return BusinessProfileEnrichmentJob(
            id = Uuid.random(),
            tenantId = tenantId,
            subjectType = subjectType,
            subjectId = tenantId.value,
            status = tech.dokus.domain.enums.BusinessProfileEnrichmentJobStatus.Processing,
            triggerReason = "TEST",
            scheduledAt = now,
            nextAttemptAt = now,
            attemptCount = attemptCount,
            lastError = null,
            processingStartedAt = now,
            createdAt = now,
            updatedAt = now
        )
    }
}
