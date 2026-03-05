@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.worker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import io.mockk.slot
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.backend.services.business.BusinessLogoSelectionService
import tech.dokus.backend.services.business.BusinessWebsiteProbe
import tech.dokus.backend.services.business.BusinessWebsiteRanker
import tech.dokus.backend.services.business.ImageDownloadFailureKind
import tech.dokus.backend.services.business.ImageDownloadResult
import tech.dokus.backend.services.business.RankedWebsiteCandidate
import tech.dokus.backend.services.business.WebsiteRankingDecision
import tech.dokus.backend.services.business.WebsiteRankingEvidence
import tech.dokus.backend.services.business.WebsiteRankingResult
import tech.dokus.backend.services.business.WebsiteSearchResult
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
import tech.dokus.features.ai.agents.BusinessLogoFallbackAgent
import tech.dokus.features.ai.agents.BusinessProfileContentExtractionAgent
import tech.dokus.features.ai.models.BusinessLogoFallbackResult
import tech.dokus.features.ai.models.BusinessProfileContentExtractionResult
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import tech.dokus.foundation.backend.storage.AvatarStorageService
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.time.Duration.Companion.minutes

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
    private val contentExtractionAgent = mockk<BusinessProfileContentExtractionAgent>()
    private val logoFallbackAgent = mockk<BusinessLogoFallbackAgent>()
    private val websiteProbe = mockk<BusinessWebsiteProbe>()
    private val websiteRanker = mockk<BusinessWebsiteRanker>()

    @Test
    fun `low confidence website keeps fields unchanged and stores low confidence error`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        val ranking = rejectedRanking("https://acme.example", 49)

        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)

        coEvery { websiteProbe.buildStrictSearchQuery("Acme Logistics", "BE") } returns "Acme Logistics BE"
        coEvery { websiteProbe.searchWebsiteCandidates("Acme Logistics", "BE", 3) } returns listOf(
            WebsiteSearchResult(
                url = "https://acme.example",
                title = "Acme Logistics",
                snippet = "Acme Logistics website",
                searchRank = 1
            )
        )
        coEvery { websiteProbe.crawl("https://acme.example", config.maxPages) } returns tech.dokus.backend.services.business.BusinessWebsiteCrawlResult(
            pages = listOf(
                tech.dokus.backend.services.business.CrawledBusinessPage(
                    url = "https://acme.example",
                    textContent = "Acme Logistics",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            ),
            blockedByRobots = false
        )
        coEvery { websiteRanker.rank(any(), any(), any()) } returns ranking

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

        coVerify(exactly = 1) { websiteProbe.searchWebsiteCandidates("Acme Logistics", "BE", 3) }
        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Unset,
                evidenceScore = 49,
                evidenceChecksJson = any(),
                websiteUrl = null,
                businessSummary = null,
                businessActivities = null,
                logoStorageKey = null,
                lastErrorCode = "LOW_CONFIDENCE_WEBSITE",
                lastErrorMessage = "Top candidate acme.example scored 49 (requires >= 50)"
            )
        }
        coVerify(exactly = 0) { contentExtractionAgent.extract(any()) }
    }

    @Test
    fun `accepted ranking persists verified website and extracted content`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        val ranking = acceptedRanking("https://acme.example", 86)

        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)

        coEvery { websiteProbe.buildStrictSearchQuery("Acme Logistics", "BE") } returns "Acme Logistics BE"
        coEvery { websiteProbe.searchWebsiteCandidates("Acme Logistics", "BE", 3) } returns listOf(
            WebsiteSearchResult(
                url = "https://acme.example",
                title = "Acme Logistics",
                snippet = "Acme Logistics website",
                searchRank = 1
            )
        )
        coEvery { websiteProbe.crawl("https://acme.example", config.maxPages) } returns tech.dokus.backend.services.business.BusinessWebsiteCrawlResult(
            pages = listOf(
                tech.dokus.backend.services.business.CrawledBusinessPage(
                    url = "https://acme.example",
                    textContent = "Acme Logistics helps finance teams automate AP workflows.",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = listOf("https://acme.example/logo.png")
                )
            ),
            blockedByRobots = false
        )

        coEvery { websiteRanker.rank(any(), any(), any()) } returns ranking
        coEvery { contentExtractionAgent.extract(any()) } returns BusinessProfileContentExtractionResult(
            businessSummary = "Acme Logistics provides AP automation software.",
            activities = listOf("AP automation", "Invoice ingestion"),
            confidence = 0.92
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
                evidenceScore = 86,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = "Acme Logistics provides AP automation software.",
                businessActivities = listOf("AP automation", "Invoice ingestion"),
                logoStorageKey = null,
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
        coVerify(exactly = 0) { avatarStorageService.uploadAvatar(any(), any(), any()) }
    }

    @Test
    fun `suggested ranking persists website with suggested state`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        val ranking = suggestedRanking("https://acme.example", 70)

        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)

        coEvery { websiteProbe.buildStrictSearchQuery("Acme Logistics", "BE") } returns "Acme Logistics BE"
        coEvery { websiteProbe.searchWebsiteCandidates("Acme Logistics", "BE", 3) } returns listOf(
            WebsiteSearchResult(
                url = "https://acme.example",
                title = "Acme Logistics",
                snippet = "Acme Logistics website",
                searchRank = 1
            )
        )
        coEvery { websiteProbe.crawl("https://acme.example", config.maxPages) } returns tech.dokus.backend.services.business.BusinessWebsiteCrawlResult(
            pages = listOf(
                tech.dokus.backend.services.business.CrawledBusinessPage(
                    url = "https://acme.example",
                    textContent = "Acme Logistics helps finance teams automate AP workflows.",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            ),
            blockedByRobots = false
        )

        coEvery { websiteRanker.rank(any(), any(), any()) } returns ranking
        coEvery { contentExtractionAgent.extract(any()) } returns BusinessProfileContentExtractionResult(
            businessSummary = "Acme Logistics provides AP automation software.",
            activities = listOf("AP automation"),
            confidence = 0.88
        )
        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null
        coEvery { logoFallbackAgent.findLogoCandidates(any()) } returns BusinessLogoFallbackResult(emptyList())
        coEvery { websiteProbe.downloadImageDetailed(any(), any(), any()) } returns ImageDownloadResult(
            image = null,
            failureKind = ImageDownloadFailureKind.HttpStatus,
            statusCode = 404
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
                verificationState = BusinessProfileVerificationState.Suggested,
                evidenceScore = 70,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = "Acme Logistics provides AP automation software.",
                businessActivities = listOf("AP automation"),
                logoStorageKey = null,
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
    }

    @Test
    fun `suggested ranking can persist discovered logo`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        val ranking = suggestedRanking("https://acme.example", 62)
        val logoPng = pngBytes(96, 96, Color.BLUE)

        coEvery { jobRepository.recoverStaleProcessing(any(), any(), any()) } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(listOf(job))
        coEvery { jobRepository.markCompleted(job.id) } returns Result.success(true)

        coEvery { tenantRepository.findById(job.tenantId) } returns sampleTenant(job.tenantId)
        coEvery { addressRepository.getCompanyAddress(job.tenantId) } returns sampleAddress(job.tenantId)

        coEvery { websiteProbe.buildStrictSearchQuery("Acme Logistics", "BE") } returns "Acme Logistics BE"
        coEvery { websiteProbe.searchWebsiteCandidates("Acme Logistics", "BE", 3) } returns listOf(
            WebsiteSearchResult(
                url = "https://acme.example",
                title = "Acme Logistics",
                snippet = "Acme Logistics website",
                searchRank = 1
            )
        )
        coEvery { websiteProbe.crawl("https://acme.example", config.maxPages) } returns tech.dokus.backend.services.business.BusinessWebsiteCrawlResult(
            pages = listOf(
                tech.dokus.backend.services.business.CrawledBusinessPage(
                    url = "https://acme.example",
                    textContent = "Acme Logistics helps finance teams automate AP workflows.",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = listOf("https://acme.example/logo.png")
                )
            ),
            blockedByRobots = false
        )

        coEvery { websiteRanker.rank(any(), any(), any()) } returns ranking
        coEvery { contentExtractionAgent.extract(any()) } returns BusinessProfileContentExtractionResult(
            businessSummary = "Acme Logistics provides AP automation software.",
            activities = listOf("AP automation"),
            confidence = 0.88
        )
        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null
        coEvery { websiteProbe.downloadImageDetailed(any(), any(), any()) } returns ImageDownloadResult(
            image = null,
            failureKind = ImageDownloadFailureKind.HttpStatus,
            statusCode = 404
        )
        coEvery { websiteProbe.downloadImageDetailed("https://acme.example/logo.png", any(), any()) } returns ImageDownloadResult(
            image = tech.dokus.backend.services.business.DownloadedBusinessImage(
                bytes = logoPng,
                contentType = "image/png"
            ),
            normalizedUrl = "https://acme.example/logo.png",
            statusCode = 200,
            contentType = "image/png"
        )
        coEvery {
            avatarStorageService.uploadAvatar(
                job.tenantId,
                any(),
                "image/png"
            )
        } returns AvatarStorageService.AvatarUploadResult(
            storageKeyPrefix = "avatars/suggested-logo",
            avatar = Thumbnail(small = "s", medium = "m", large = "l"),
            sizeBytes = logoPng.size.toLong()
        )
        coEvery { tenantRepository.updateAvatarStorageKey(job.tenantId, "avatars/suggested-logo") } returns Unit
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

        coVerify(exactly = 1) { avatarStorageService.uploadAvatar(job.tenantId, any(), "image/png") }
        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Suggested,
                evidenceScore = 62,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = "Acme Logistics provides AP automation software.",
                businessActivities = listOf("AP automation"),
                logoStorageKey = "avatars/suggested-logo",
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

        coEvery { websiteProbe.buildStrictSearchQuery("Acme Logistics", "BE") } throws IllegalStateException("boom")
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

    @Test
    fun `stale recovery lease is extended for long-running logo pipeline`() = runBlocking {
        val staleBefore = slot<LocalDateTime>()
        val retryAt = slot<LocalDateTime>()

        coEvery {
            jobRepository.recoverStaleProcessing(
                staleBefore = capture(staleBefore),
                retryAt = capture(retryAt),
                reason = any()
            )
        } returns Result.success(0)
        coEvery { jobRepository.claimDue(any(), any()) } returns Result.success(emptyList())

        val worker = createWorker()
        worker.processBatchForTest()

        val delta = retryAt.captured.toInstant(TimeZone.UTC) - staleBefore.captured.toInstant(TimeZone.UTC)
        assertTrue(delta >= 20.minutes, "Expected stale lease window >= 20 minutes, got $delta")
    }

    private fun createWorker(): BusinessProfileEnrichmentWorker {
        coEvery { websiteProbe.canonicalizeWebsiteUrl(any()) } answers { invocation.args[0] as String }
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
            contentExtractionAgent = contentExtractionAgent,
            logoFallbackAgent = logoFallbackAgent,
            websiteProbe = websiteProbe,
            websiteRanker = websiteRanker,
            logoSelectionService = BusinessLogoSelectionService(websiteProbe)
        )
    }

    private fun acceptedRanking(url: String, score: Int): WebsiteRankingResult {
        val best = RankedWebsiteCandidate(
            url = url,
            searchRank = 1,
            pathDepth = 0,
            score = score,
            hardIdentityHits = 2,
            signals = emptyList()
        )
        return WebsiteRankingResult(
            decision = WebsiteRankingDecision.VERIFIED,
            accepted = true,
            bestCandidate = best,
            allCandidates = listOf(best),
            evidence = WebsiteRankingEvidence(
                query = "Acme Logistics BE",
                decision = WebsiteRankingDecision.VERIFIED,
                verifiedThreshold = 70,
                suggestedThreshold = 50,
                threshold = 70,
                accepted = true,
                selectedUrl = url,
                selectedScore = score,
                candidates = emptyList()
            )
        )
    }

    private fun rejectedRanking(url: String, score: Int): WebsiteRankingResult {
        val best = RankedWebsiteCandidate(
            url = url,
            searchRank = 1,
            pathDepth = 0,
            score = score,
            hardIdentityHits = 1,
            signals = emptyList()
        )
        return WebsiteRankingResult(
            decision = WebsiteRankingDecision.REJECTED,
            accepted = false,
            bestCandidate = best,
            allCandidates = listOf(best),
            evidence = WebsiteRankingEvidence(
                query = "Acme Logistics BE",
                decision = WebsiteRankingDecision.REJECTED,
                verifiedThreshold = 70,
                suggestedThreshold = 50,
                threshold = 70,
                accepted = false,
                selectedUrl = null,
                selectedScore = score,
                candidates = emptyList()
            )
        )
    }

    private fun suggestedRanking(url: String, score: Int): WebsiteRankingResult {
        val best = RankedWebsiteCandidate(
            url = url,
            searchRank = 1,
            pathDepth = 0,
            score = score,
            hardIdentityHits = 1,
            signals = emptyList()
        )
        return WebsiteRankingResult(
            decision = WebsiteRankingDecision.SUGGESTED,
            accepted = true,
            bestCandidate = best,
            allCandidates = listOf(best),
            evidence = WebsiteRankingEvidence(
                query = "Acme Logistics BE",
                decision = WebsiteRankingDecision.SUGGESTED,
                verifiedThreshold = 70,
                suggestedThreshold = 50,
                threshold = 70,
                accepted = true,
                selectedUrl = url,
                selectedScore = score,
                candidates = emptyList()
            )
        )
    }

    private fun pngBytes(width: Int, height: Int, color: Color): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = color
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
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
