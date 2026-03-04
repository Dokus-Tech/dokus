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
import tech.dokus.backend.services.business.BusinessProfileEvidenceResult
import tech.dokus.backend.services.business.EvidenceGateOutcome
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.business.BusinessProfileEvidenceGate
import tech.dokus.backend.services.business.BusinessProfileEvidenceInput
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.backend.services.business.BusinessWebsiteCrawlResult
import tech.dokus.backend.services.business.BusinessWebsiteProbe
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
import tech.dokus.features.ai.agents.BusinessProfileEnrichmentAgent
import tech.dokus.features.ai.models.BusinessProfileDiscoveryResult
import tech.dokus.features.ai.models.BusinessProfileEnrichmentInput
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
    private val enrichmentAgent: BusinessProfileEnrichmentAgent,
    private val websiteProbe: BusinessWebsiteProbe,
    private val evidenceGate: BusinessProfileEvidenceGate,
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

            val modelInput = BusinessProfileEnrichmentInput(
                tenantId = job.tenantId.toString(),
                subjectType = job.subjectType.dbValue,
                subjectId = job.subjectId.toString(),
                companyName = context.name,
                companyVatNumber = context.vatNumber,
                companyCountry = context.country,
                companyCity = context.city,
                companyPostalCode = context.postalCode,
                companyEmail = context.email,
                companyPhone = context.phone,
                outputLanguage = context.language,
                maxPages = config.maxPages
            )

            val discovery = enrichmentAgent.enrich(modelInput)
            var searchResultUrls = discovery.searchResultUrls
            var candidateWebsite = pickCandidateWebsite(discovery)

            if (candidateWebsite.isNullOrBlank()) {
                val deterministicSearchUrls = websiteProbe.searchWebsiteCandidates(
                    companyName = context.name,
                    vatNumber = context.vatNumber,
                    country = context.country,
                    maxResults = 5
                )
                if (deterministicSearchUrls.isNotEmpty()) {
                    searchResultUrls = (searchResultUrls + deterministicSearchUrls).distinct()
                    candidateWebsite = searchResultUrls.firstOrNull { !isAggregatorOrSocialUrl(it) }
                    if (!candidateWebsite.isNullOrBlank()) {
                        logger.info(
                            "Using deterministic Serper fallback candidate for subjectType={}, subjectId={}: {}",
                            job.subjectType,
                            job.subjectId,
                            candidateWebsite
                        )
                    }
                }
            }

            val evidence: BusinessProfileEvidenceResult
            val crawl: BusinessWebsiteCrawlResult?

            if (!candidateWebsite.isNullOrBlank()) {
                crawl = websiteProbe.crawl(startUrl = candidateWebsite, maxPages = config.maxPages)
                evidence = evidenceGate.evaluate(
                    BusinessProfileEvidenceInput(
                        companyName = context.name,
                        companyVatNumber = context.vatNumber,
                        companyCountry = context.country,
                        companyCity = context.city,
                        companyPostalCode = context.postalCode,
                        companyEmail = context.email,
                        companyPhone = context.phone,
                        candidateWebsiteUrl = candidateWebsite,
                        llmConfidence = discovery.confidence,
                        searchResultUrls = searchResultUrls,
                        crawledText = crawl.pages.joinToString("\n") { it.textContent },
                        structuredDataSnippets = crawl.pages.flatMap { it.structuredDataSnippets }
                    )
                )
            } else {
                logger.info(
                    "No website candidate resolved for subjectType={}, subjectId={} (agent status={})",
                    job.subjectType,
                    job.subjectId,
                    discovery.status
                )
                crawl = null
                evidence = BusinessProfileEvidenceResult(
                    outcome = EvidenceGateOutcome.SKIP,
                    verificationState = BusinessProfileVerificationState.Unset,
                    evidenceScore = 0,
                    checks = emptyList()
                )
            }

            val checksJson = json.encodeToString(evidence.checks)

            val shouldSkip = evidence.verificationState == BusinessProfileVerificationState.Unset
            val websiteToPersist = if (shouldSkip) null else candidateWebsite
            val summaryToPersist = if (shouldSkip) null else discovery.businessSummary
            val activitiesToPersist = if (shouldSkip) null else discovery.activities
            val lastErrorCode = if (shouldSkip) "LOW_EVIDENCE" else null
            val lastErrorMessage = if (shouldSkip) "Evidence score below threshold" else null

            val shouldAttemptLogo = !shouldSkip && evidence.verificationState != BusinessProfileVerificationState.Unset
            val logoStorageKey = if (shouldAttemptLogo) {
                resolveLogoStorageKey(job, discovery.logoUrl, crawl?.pages.orEmpty().flatMap { it.logoCandidates })
            } else {
                null
            }
            if (evidence.verificationState == BusinessProfileVerificationState.Suggested && !logoStorageKey.isNullOrBlank()) {
                logger.info(
                    "Applied discovered logo for suggested business profile subjectType={}, subjectId={}",
                    job.subjectType,
                    job.subjectId
                )
            }

            businessProfileService.applyEnrichment(
                tenantId = job.tenantId,
                subjectType = job.subjectType,
                subjectId = job.subjectId,
                verificationState = evidence.verificationState,
                evidenceScore = evidence.evidenceScore,
                evidenceChecksJson = checksJson,
                websiteUrl = websiteToPersist,
                businessSummary = summaryToPersist,
                businessActivities = activitiesToPersist,
                logoStorageKey = logoStorageKey,
                lastErrorCode = lastErrorCode,
                lastErrorMessage = lastErrorMessage,
            )

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

    private fun pickCandidateWebsite(discovery: BusinessProfileDiscoveryResult): String? {
        val directCandidate = discovery.candidateWebsiteUrl?.takeUnless { isAggregatorOrSocialUrl(it) }
        if (discovery.candidateWebsiteUrl != null && directCandidate == null) {
            logger.info("Discarded aggregator/social direct website candidate: {}", discovery.candidateWebsiteUrl)
        }
        if (!directCandidate.isNullOrBlank()) return directCandidate

        val searchFallback = discovery.searchResultUrls.firstOrNull { !isAggregatorOrSocialUrl(it) }
        if (!searchFallback.isNullOrBlank()) {
            logger.info("Using search result fallback website candidate: {}", searchFallback)
        }
        return searchFallback
    }

    private suspend fun resolveLogoStorageKey(
        job: BusinessProfileEnrichmentJob,
        preferredLogoUrl: String?,
        fallbackLogoUrls: List<String>
    ): String? {
        val existingProfile = profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId)
        if (existingProfile?.logoPinned == true) return null
        if (!existingProfile?.logoStorageKey.isNullOrBlank()) return null

        if (job.subjectType == BusinessProfileSubjectType.Tenant) {
            val tenantAvatar = tenantRepository.getAvatarStorageKey(job.tenantId)
            if (!tenantAvatar.isNullOrBlank()) return null
        }

        val candidateUrl = sequenceOf(preferredLogoUrl)
            .plus(fallbackLogoUrls.asSequence())
            .mapNotNull { it?.trim() }
            .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
            ?: return null

        val image = websiteProbe.downloadImage(candidateUrl) ?: return null
        val upload = runSuspendCatching {
            avatarStorageService.uploadAvatar(job.tenantId, image.bytes, image.contentType)
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
                BusinessSubjectContext(
                    name = contact.name.value,
                    vatNumber = contact.vatNumber?.value,
                    country = contactAddress?.country,
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
