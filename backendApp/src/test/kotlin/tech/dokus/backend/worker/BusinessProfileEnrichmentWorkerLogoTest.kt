@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.worker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.backend.services.business.BusinessLogoSelectionService
import tech.dokus.backend.services.business.BusinessWebsiteProbe
import tech.dokus.backend.services.business.BusinessWebsiteRanker
import tech.dokus.backend.services.business.DownloadedBusinessImage
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
import tech.dokus.features.ai.models.BusinessLogoFallbackCandidate
import tech.dokus.features.ai.models.BusinessLogoFallbackResult
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import tech.dokus.foundation.backend.storage.AvatarStorageService
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class BusinessProfileEnrichmentWorkerLogoTest {
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
    fun `logo selector prefers largest png and uploads png bytes`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        val ranking = acceptedRanking("https://acme.example", 92)
        val smallPng = pngBytes(32, 32, Color.RED)
        val largePng = pngBytes(160, 160, Color.BLUE)
        val svg = """
            <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"48\" height=\"48\">
              <rect width=\"48\" height=\"48\" fill=\"#000\" />
            </svg>
        """.trimIndent().toByteArray()

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
                snippet = "Acme website",
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
                    logoCandidates = listOf(
                        "https://acme.example/logo-small.png",
                        "https://acme.example/logo.svg",
                        "https://acme.example/logo-large.png"
                    )
                )
            ),
            blockedByRobots = false
        )
        coEvery { websiteRanker.rank(any(), any(), any()) } returns ranking
        coEvery { contentExtractionAgent.extract(any()) } throws IllegalStateException("simulate extract failure")

        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null

        coEvery { websiteProbe.downloadImageDetailed(any(), any(), any()) } returns ImageDownloadResult(
            image = null,
            failureKind = ImageDownloadFailureKind.HttpStatus,
            statusCode = 404
        )
        coEvery { websiteProbe.downloadImageDetailed("https://acme.example/logo-small.png", any(), any()) } returns ImageDownloadResult(
            image = DownloadedBusinessImage(
            bytes = smallPng,
            contentType = "image/png"
            ),
            normalizedUrl = "https://acme.example/logo-small.png",
            statusCode = 200,
            contentType = "image/png"
        )
        coEvery { websiteProbe.downloadImageDetailed("https://acme.example/logo-large.png", any(), any()) } returns ImageDownloadResult(
            image = DownloadedBusinessImage(
            bytes = largePng,
            contentType = "image/png"
            ),
            normalizedUrl = "https://acme.example/logo-large.png",
            statusCode = 200,
            contentType = "image/png"
        )
        coEvery { websiteProbe.downloadImageDetailed("https://acme.example/logo.svg", any(), any()) } returns ImageDownloadResult(
            image = DownloadedBusinessImage(
            bytes = svg,
            contentType = "image/svg+xml"
            ),
            normalizedUrl = "https://acme.example/logo.svg",
            statusCode = 200,
            contentType = "image/svg+xml"
        )

        val uploadedBytes = slot<ByteArray>()
        coEvery {
            avatarStorageService.uploadAvatar(
                job.tenantId,
                capture(uploadedBytes),
                "image/png"
            )
        } returns AvatarStorageService.AvatarUploadResult(
            storageKeyPrefix = "avatars/new-logo",
            avatar = Thumbnail(small = "s", medium = "m", large = "l"),
            sizeBytes = largePng.size.toLong()
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

        createWorker().processBatchForTest()

        assertTrue(uploadedBytes.captured.contentEquals(largePng))
        coVerify(exactly = 1) {
            avatarStorageService.uploadAvatar(
                job.tenantId,
                any(),
                "image/png"
            )
        }
        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Verified,
                evidenceScore = 92,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = null,
                businessActivities = null,
                logoStorageKey = "avatars/new-logo",
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
    }

    @Test
    fun `favicon fallback is used when no page logo candidates are present`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        val ranking = acceptedRanking("https://acme.example", 91)
        val faviconPng = pngBytes(96, 96, Color.GREEN)

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
                snippet = "Acme website",
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
        coEvery { contentExtractionAgent.extract(any()) } throws IllegalStateException("simulate extract failure")

        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null

        coEvery { websiteProbe.downloadImageDetailed(any(), any(), any()) } returns ImageDownloadResult(
            image = null,
            failureKind = ImageDownloadFailureKind.HttpStatus,
            statusCode = 404
        )
        coEvery { websiteProbe.downloadImageDetailed("https://acme.example/favicon.ico", any(), any()) } returns ImageDownloadResult(
            image = DownloadedBusinessImage(
            bytes = faviconPng,
            contentType = "image/png"
            ),
            normalizedUrl = "https://acme.example/favicon.ico",
            statusCode = 200,
            contentType = "image/png"
        )

        val uploadedBytes = slot<ByteArray>()
        coEvery {
            avatarStorageService.uploadAvatar(
                job.tenantId,
                capture(uploadedBytes),
                "image/png"
            )
        } returns AvatarStorageService.AvatarUploadResult(
            storageKeyPrefix = "avatars/fallback-logo",
            avatar = Thumbnail(small = "s", medium = "m", large = "l"),
            sizeBytes = faviconPng.size.toLong()
        )
        coEvery { tenantRepository.updateAvatarStorageKey(job.tenantId, "avatars/fallback-logo") } returns Unit
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

        createWorker().processBatchForTest()

        assertTrue(uploadedBytes.captured.contentEquals(faviconPng))
        coVerify(exactly = 1) { websiteProbe.downloadImageDetailed("https://acme.example/favicon.ico", any(), any()) }
        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Verified,
                evidenceScore = 91,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = null,
                businessActivities = null,
                logoStorageKey = "avatars/fallback-logo",
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
    }

    @Test
    fun `ai fallback candidate is used when deterministic candidates are missing`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        val ranking = acceptedRanking("https://acme.example", 90)
        val aiLogoPng = pngBytes(128, 128, Color.ORANGE)
        val aiLogoUrl = "https://acme.example/assets/brand/logo.png"

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
                snippet = "Acme website",
                searchRank = 1
            )
        )
        coEvery { websiteProbe.crawl("https://acme.example", config.maxPages) } returns tech.dokus.backend.services.business.BusinessWebsiteCrawlResult(
            pages = listOf(
                tech.dokus.backend.services.business.CrawledBusinessPage(
                    url = "https://acme.example",
                    title = "Acme Logistics",
                    headHtmlSnippet = "<link rel=\"icon\" href=\"/favicon.ico\">",
                    logoRelevantHtmlSnippet = "<img src=\"/assets/brand/logo.png\">",
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
        coEvery { contentExtractionAgent.extract(any()) } throws IllegalStateException("simulate extract failure")
        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null
        coEvery {
            logoFallbackAgent.findLogoCandidates(any())
        } returns BusinessLogoFallbackResult(
            candidates = listOf(
                BusinessLogoFallbackCandidate(
                    url = aiLogoUrl,
                    confidence = 0.88,
                    reason = "Logo asset path from header snippet"
                )
            )
        )
        coEvery { websiteProbe.downloadImageDetailed(any(), any(), any()) } returns ImageDownloadResult(
            image = null,
            failureKind = ImageDownloadFailureKind.HttpStatus,
            statusCode = 404
        )
        coEvery { websiteProbe.downloadImageDetailed(aiLogoUrl, any(), any()) } returns ImageDownloadResult(
            image = DownloadedBusinessImage(bytes = aiLogoPng, contentType = "image/png"),
            normalizedUrl = aiLogoUrl,
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
            storageKeyPrefix = "avatars/ai-fallback-logo",
            avatar = Thumbnail(small = "s", medium = "m", large = "l"),
            sizeBytes = aiLogoPng.size.toLong()
        )
        coEvery { tenantRepository.updateAvatarStorageKey(job.tenantId, "avatars/ai-fallback-logo") } returns Unit
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

        createWorker().processBatchForTest()

        coVerify(exactly = 1) { logoFallbackAgent.findLogoCandidates(any()) }
        coVerify(exactly = 1) { avatarStorageService.uploadAvatar(job.tenantId, any(), "image/png") }
        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Verified,
                evidenceScore = 90,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = null,
                businessActivities = null,
                logoStorageKey = "avatars/ai-fallback-logo",
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
    }

    @Test
    fun `off-domain ai fallback candidates are rejected and no logo is persisted`() = runBlocking {
        val job = sampleJob(subjectType = BusinessProfileSubjectType.Tenant, attemptCount = 0)
        val ranking = acceptedRanking("https://acme.example", 88)
        val rejectedUrl = "https://cdn.evil.example/share/social-banner.png"

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
                snippet = "Acme website",
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
        coEvery { contentExtractionAgent.extract(any()) } throws IllegalStateException("simulate extract failure")
        coEvery { profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId) } returns null
        coEvery { tenantRepository.getAvatarStorageKey(job.tenantId) } returns null
        coEvery {
            logoFallbackAgent.findLogoCandidates(any())
        } returns BusinessLogoFallbackResult(
            candidates = listOf(
                BusinessLogoFallbackCandidate(url = rejectedUrl, confidence = 0.95, reason = "Banner")
            )
        )
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

        createWorker().processBatchForTest()

        coVerify(exactly = 1) { logoFallbackAgent.findLogoCandidates(any()) }
        coVerify(exactly = 0) { websiteProbe.downloadImageDetailed(rejectedUrl, any(), any()) }
        coVerify(exactly = 0) { avatarStorageService.uploadAvatar(any(), any(), any()) }
        coVerify(exactly = 1) {
            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = BusinessProfileVerificationState.Verified,
                evidenceScore = 88,
                evidenceChecksJson = any(),
                websiteUrl = "https://acme.example",
                businessSummary = null,
                businessActivities = null,
                logoStorageKey = null,
                lastErrorCode = null,
                lastErrorMessage = null
            )
        }
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
            logoSelectionService = BusinessLogoSelectionService(websiteProbe),
            llmQueue = tech.dokus.features.ai.queue.LlmQueue()
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
