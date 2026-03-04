@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.business.BusinessLogoSelectionService
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.backend.services.business.BusinessWebsiteProbe
import tech.dokus.backend.services.business.BusinessWebsiteRanker
import tech.dokus.backend.services.business.WebsiteCandidateInput
import tech.dokus.backend.services.business.WebsiteRankingDecision
import tech.dokus.backend.services.business.WebsiteRankingContext
import tech.dokus.backend.services.business.isAggregatorOrSocialHost
import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJob
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.database.repository.business.BusinessProfileRepository
import tech.dokus.database.repository.contacts.ContactAddressRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.agents.BusinessProfileContentExtractionAgent
import tech.dokus.features.ai.models.BusinessProfileContentExtractionInput
import tech.dokus.features.ai.models.BusinessProfileContentExtractionResult
import tech.dokus.features.ai.models.BusinessProfileContentPage
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import tech.dokus.foundation.backend.storage.AvatarStorageService
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi

private data class BusinessSubjectContext(
    val name: String,
    val vatNumber: String? = null,
    val country: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val language: tech.dokus.domain.enums.Language,
)

private data class RankedCandidateWithPages(
    val input: WebsiteCandidateInput,
)

class BusinessProfileEnrichmentWorker(
    private val config: BusinessProfileEnrichmentConfig,
    private val jobRepository: BusinessProfileEnrichmentJobRepository,
    private val profileRepository: BusinessProfileRepository,
    private val businessProfileService: BusinessProfileService,
    private val tenantRepository: TenantRepository,
    private val addressRepository: AddressRepository,
    private val contactRepository: ContactRepository,
    private val contactAddressRepository: ContactAddressRepository,
    private val avatarStorageService: AvatarStorageService,
    private val contentExtractionAgent: BusinessProfileContentExtractionAgent,
    private val websiteProbe: BusinessWebsiteProbe,
    private val websiteRanker: BusinessWebsiteRanker,
    private val logoSelectionService: BusinessLogoSelectionService,
) {
    private val logger = loggerFor()
    private var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val running = AtomicBoolean(false)

    fun start() {
        if (!config.enabled) {
            logger.info("Business profile enrichment worker disabled by config")
            return
        }
        if (config.serperApiKey.isBlank()) {
            logger.warn("Business profile enrichment worker disabled: serperApiKey is not configured")
            return
        }
        if (!running.compareAndSet(false, true)) {
            logger.warn("Business profile enrichment worker already running")
            return
        }
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        pollingJob = scope.launch {
            while (isActive && running.get()) {
                try {
                    processBatch()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Business profile enrichment loop failed", e)
                }
                delay(config.pollingIntervalMs)
            }
        }
        logger.info(
            "Started business profile enrichment worker (interval={}ms, batch={}, maxAttempts={})",
            config.pollingIntervalMs,
            config.batchSize,
            config.maxAttempts
        )
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        pollingJob?.cancel()
        pollingJob = null
        scope.coroutineContext[Job]?.cancel()
        logger.info("Stopped business profile enrichment worker")
    }

    private suspend fun processBatch() {
        val nowInstant = Clock.System.now()
        val now = nowInstant.toLocalDateTime(TimeZone.UTC)
        val staleBefore = (nowInstant - config.staleLeaseMinutes.minutes).toLocalDateTime(TimeZone.UTC)
        jobRepository.recoverStaleProcessing(
            staleBefore = staleBefore,
            retryAt = now,
            reason = "Recovered stale business profile enrichment lease"
        ).onFailure { logger.error("Failed to recover stale enrichment jobs", it) }

        val jobs = jobRepository.claimDue(now = now, limit = config.batchSize)
            .getOrElse { error ->
                logger.error("Failed to claim enrichment jobs", error)
                return
            }

        if (jobs.isEmpty()) {
            logger.debug("No business profile enrichment jobs due")
            return
        }
        logger.info("Processing {} business profile enrichment job(s)", jobs.size)

        supervisorScope {
            jobs.map { job -> async { processSingleJob(job) } }.awaitAll()
        }
    }

    internal suspend fun processBatchForTest() {
        running.set(true)
        try {
            processBatch()
        } finally {
            running.set(false)
        }
    }

    private suspend fun processSingleJob(job: BusinessProfileEnrichmentJob) {
        runSuspendCatching {
            val context = loadSubjectContext(job) ?: run {
                jobRepository.markCompletedWithError(job.id, "Subject not found")
                return
            }

            val searchQuery = websiteProbe.buildStrictSearchQuery(context.name, context.country)
            val searchCandidates = if (searchQuery == null) {
                emptyList()
            } else {
                websiteProbe.searchWebsiteCandidates(
                    companyName = context.name,
                    country = context.country,
                    maxResults = 3
                )
            }

            val candidatesWithPages = searchCandidates
                .take(3)
                .mapNotNull { searchResult ->
                    val candidateUrl = searchResult.url.trim()
                    if (candidateUrl.isBlank()) return@mapNotNull null
                    if (isAggregatorOrSocialUrl(candidateUrl)) {
                        logger.info(
                            "Skipping aggregator/social candidate for subjectType={}, subjectId={}, url={}",
                            job.subjectType,
                            job.subjectId,
                            candidateUrl
                        )
                        return@mapNotNull null
                    }

                    val crawl = websiteProbe.crawl(startUrl = candidateUrl, maxPages = config.maxPages)
                    RankedCandidateWithPages(
                        input = WebsiteCandidateInput(
                            url = candidateUrl,
                            searchRank = searchResult.searchRank,
                            pages = crawl.pages,
                            searchTitle = searchResult.title,
                            searchSnippet = searchResult.snippet
                        )
                    )
                }

            val ranking = websiteRanker.rank(
                context = WebsiteRankingContext(
                    companyName = context.name,
                    vatNumber = context.vatNumber,
                    country = context.country,
                    city = context.city,
                    postalCode = context.postalCode,
                    email = context.email,
                    phone = context.phone,
                ),
                searchQuery = searchQuery ?: context.name,
                candidates = candidatesWithPages.map { it.input }
            )
            val rankingJson = json.encodeToString(ranking.evidence)
            logger.info(
                "Website ranking decision for subjectType={}, subjectId={}, decision={}, selectedScore={}, verifiedThreshold={}, suggestedThreshold={}, selectedUrl={}",
                job.subjectType,
                job.subjectId,
                ranking.decision,
                ranking.evidence.selectedScore,
                ranking.evidence.verifiedThreshold,
                ranking.evidence.suggestedThreshold,
                ranking.evidence.selectedUrl
            )
            ranking.allCandidates.forEach { candidate ->
                val signals = candidate.signals.joinToString(",") { signal ->
                    "${signal.signal.name}:${if (signal.passed) 1 else 0}"
                }
                logger.debug(
                    "Website candidate ranking for subjectType={}, subjectId={}, rank={}, score={}, url={}, signals={}",
                    job.subjectType,
                    job.subjectId,
                    candidate.searchRank,
                    candidate.score,
                    candidate.url,
                    signals
                )
            }

            val selected = ranking.bestCandidate
            if (ranking.decision == WebsiteRankingDecision.REJECTED || selected == null) {
                val bestHost = selected?.url
                    ?.let { runCatching { URI(it).host?.removePrefix("www.") }.getOrNull() }
                val bestScore = selected?.score ?: 0
                val message = if (bestHost.isNullOrBlank()) {
                    "No official website candidate scored at least 50"
                } else {
                    "Top candidate $bestHost scored $bestScore (requires >= 50)"
                }
                logger.info(
                    "Rejected business profile enrichment for subjectType={}, subjectId={}, reason={}, score={}",
                    job.subjectType,
                    job.subjectId,
                    message,
                    bestScore
                )

                businessProfileService.applyEnrichment(
                    tenantId = job.tenantId,
                    subjectType = job.subjectType,
                    subjectId = job.subjectId,
                    verificationState = BusinessProfileVerificationState.Unset,
                    evidenceScore = bestScore,
                    evidenceChecksJson = rankingJson,
                    websiteUrl = null,
                    businessSummary = null,
                    businessActivities = null,
                    logoStorageKey = null,
                    lastErrorCode = "LOW_CONFIDENCE_WEBSITE",
                    lastErrorMessage = message,
                )
                jobRepository.markCompleted(job.id)
                return
            }

            val selectedPages = candidatesWithPages
                .firstOrNull { it.input.url == selected.url }
                ?.input
                ?.pages
                .orEmpty()

            val extracted = extractBusinessContent(
                context = context,
                websiteUrl = selected.url,
                pages = selectedPages
            )

            val logoStorageKey = resolveLogoStorageKey(
                job = job,
                websiteUrl = selected.url,
                logoCandidates = selectedPages.flatMap { it.logoCandidates }
            )
            val verificationState = when (ranking.decision) {
                WebsiteRankingDecision.VERIFIED -> BusinessProfileVerificationState.Verified
                WebsiteRankingDecision.SUGGESTED -> BusinessProfileVerificationState.Suggested
                WebsiteRankingDecision.REJECTED -> BusinessProfileVerificationState.Unset
            }

            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = verificationState,
                evidenceScore = selected.score,
                evidenceChecksJson = rankingJson,
                websiteUrl = selected.url,
                businessSummary = extracted?.businessSummary,
                businessActivities = extracted?.activities,
                logoStorageKey = logoStorageKey,
                lastErrorCode = null,
                lastErrorMessage = null,
            )
            if (verificationState == BusinessProfileVerificationState.Suggested) {
                logger.info(
                    "Persisted suggested website enrichment for subjectType={}, subjectId={}, website={}, score={}",
                    job.subjectType,
                    job.subjectId,
                    selected.url,
                    selected.score
                )
            }

            jobRepository.markCompleted(job.id)
        }.onFailure { error ->
            logger.error(
                "Business profile enrichment failed for job={}, subjectType={}, subjectId={}",
                job.id,
                job.subjectType,
                job.subjectId,
                error
            )
            scheduleRetry(job, error)
        }
    }

    private suspend fun extractBusinessContent(
        context: BusinessSubjectContext,
        websiteUrl: String,
        pages: List<tech.dokus.backend.services.business.CrawledBusinessPage>
    ): BusinessProfileContentExtractionResult? {
        if (pages.isEmpty()) return null
        val input = BusinessProfileContentExtractionInput(
            companyName = context.name,
            companyVatNumber = context.vatNumber,
            websiteUrl = websiteUrl,
            outputLanguage = context.language,
            pages = pages.take(config.maxPages).map { page ->
                BusinessProfileContentPage(
                    url = page.url,
                    title = page.title,
                    description = page.description,
                    textContent = page.textContent.take(4_000),
                    structuredDataSnippets = page.structuredDataSnippets
                        .map { it.take(1_200) }
                        .take(5)
                )
            }
        )

        return runSuspendCatching { contentExtractionAgent.extract(input) }
            .onFailure { error ->
                logger.warn(
                    "Content extraction failed for website={}, company={}, error={}",
                    websiteUrl,
                    context.name,
                    error.message
                )
            }
            .getOrNull()
    }

    private suspend fun resolveLogoStorageKey(
        job: BusinessProfileEnrichmentJob,
        websiteUrl: String,
        logoCandidates: List<String>
    ): String? {
        val existingProfile = profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId)
        if (existingProfile?.logoPinned == true) return null
        if (!existingProfile?.logoStorageKey.isNullOrBlank()) return null

        if (job.subjectType == BusinessProfileSubjectType.Tenant) {
            val tenantAvatar = tenantRepository.getAvatarStorageKey(job.tenantId)
            if (!tenantAvatar.isNullOrBlank()) return null
        }

        val preferred = logoSelectionService.selectPreferredLogo(websiteUrl = websiteUrl, logoCandidates = logoCandidates) ?: return null
        val upload = runSuspendCatching {
            avatarStorageService.uploadAvatar(job.tenantId, preferred.bytes, preferred.contentType)
        }.getOrElse { error ->
            logger.warn("Failed to upload discovered logo for {}: {}", job.subjectId, error.message)
            return null
        }

        if (job.subjectType == BusinessProfileSubjectType.Tenant) {
            runSuspendCatching { tenantRepository.updateAvatarStorageKey(job.tenantId, upload.storageKeyPrefix) }
                .onFailure { logger.warn("Failed to apply discovered tenant logo for {}: {}", job.tenantId, it.message) }
        }
        return upload.storageKeyPrefix
    }

    private suspend fun scheduleRetry(job: BusinessProfileEnrichmentJob, error: Throwable) {
        val nextAttemptCount = job.attemptCount + 1
        if (nextAttemptCount >= config.maxAttempts) {
            jobRepository.markCompletedWithError(
                jobId = job.id,
                error = "Max attempts reached: ${error.message ?: "unknown"}"
            )
            return
        }
        val delayDuration = retryDelay(nextAttemptCount)
        val nextAttemptAt = (Clock.System.now() + delayDuration).toLocalDateTime(TimeZone.UTC)
        jobRepository.scheduleRetry(
            jobId = job.id,
            attemptCount = nextAttemptCount,
            nextAttemptAt = nextAttemptAt,
            error = error.message ?: error::class.simpleName.orEmpty()
        )
    }

    private suspend fun loadSubjectContext(job: BusinessProfileEnrichmentJob): BusinessSubjectContext? {
        return when (job.subjectType) {
            BusinessProfileSubjectType.Tenant -> {
                val tenant = tenantRepository.findById(job.tenantId) ?: return null
                val address = addressRepository.getCompanyAddress(job.tenantId)
                BusinessSubjectContext(
                    name = tenant.legalName.value,
                    vatNumber = tenant.vatNumber.value,
                    country = address?.country,
                    city = address?.city,
                    postalCode = address?.postalCode,
                    language = tenant.language
                )
            }

            BusinessProfileSubjectType.Contact -> {
                val contactId = ContactId(job.subjectId)
                val contact = contactRepository.getContact(contactId, job.tenantId).getOrNull() ?: return null
                val contactAddress = contactAddressRepository.listAddresses(job.tenantId, contactId)
                    .getOrDefault(emptyList())
                    .let { addresses -> addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull() }
                    ?.address
                val tenant = tenantRepository.findById(job.tenantId) ?: return null
                val companyAddress = addressRepository.getCompanyAddress(job.tenantId)
                BusinessSubjectContext(
                    name = contact.name.value,
                    vatNumber = contact.vatNumber?.value,
                    country = contactAddress?.country ?: companyAddress?.country,
                    city = contactAddress?.city,
                    postalCode = contactAddress?.postalCode,
                    email = contact.email?.value,
                    phone = contact.phone?.value,
                    language = tenant.language
                )
            }
        }
    }

    private fun retryDelay(attemptCount: Int): Duration {
        return when (attemptCount) {
            1 -> 1.minutes
            2 -> 5.minutes
            3 -> 15.minutes
            4 -> 1.hours
            else -> 6.hours
        }
    }

    private fun isAggregatorOrSocialUrl(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull()?.removePrefix("www.") ?: return true
        return isAggregatorOrSocialHost(host)
    }
}
