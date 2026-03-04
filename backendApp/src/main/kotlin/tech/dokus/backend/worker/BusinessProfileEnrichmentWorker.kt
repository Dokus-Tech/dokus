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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.business.BusinessLogoSelectionService
import tech.dokus.backend.services.business.LogoSelectionTrace
import tech.dokus.backend.services.business.LogoPipelineTotalBudgetMs
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
import tech.dokus.features.ai.agents.BusinessLogoFallbackAgent
import tech.dokus.features.ai.agents.BusinessProfileContentExtractionAgent
import tech.dokus.features.ai.models.BusinessLogoFallbackInput
import tech.dokus.features.ai.models.BusinessLogoFallbackPage
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

private data class LogoResolutionResult(
    val storageKey: String?,
    val trace: LogoSelectionTrace,
)

private val AbsoluteHttpUrlRegex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)

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
    private val logoFallbackAgent: BusinessLogoFallbackAgent,
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
                    val candidateUrl = websiteProbe.canonicalizeWebsiteUrl(searchResult.url) ?: searchResult.url.trim()
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
            val failedCandidateInputs = candidatesWithPages
                .map { it.input }
                .filterNot { it.url == selected.url }
                .take(3)
            val failedCandidateUrls = failedCandidateInputs.map { it.url }
            val failedCandidatePages = failedCandidateInputs.flatMap { it.pages }

            val extracted = extractBusinessContent(
                context = context,
                websiteUrl = selected.url,
                pages = selectedPages
            )

            val logoResolution = resolveLogoStorageKey(
                job = job,
                websiteUrl = selected.url,
                selectedPages = selectedPages,
                failedCandidateUrls = failedCandidateUrls,
                failedCandidatePages = failedCandidatePages
            )
            logger.info(
                "Logo discovery summary for subjectType={}, subjectId={}, website={}, candidatesInitial={}, fallbackCandidatesAppended={}, attempts={}, successesPng={}, successesSvg={}, successesIco={}, selectedSource={}, selectedFormat={}, terminalReason={}, elapsedMs={}, aiFallbackInvoked={}, aiCallMs={}, aiCandidateCountRaw={}, aiCandidateCountAccepted={}, aiCandidateRejectReasons={}, phaseTerminalReason={}",
                job.subjectType,
                job.subjectId,
                selected.url,
                logoResolution.trace.initialCandidates,
                logoResolution.trace.fallbackCandidatesAppended,
                logoResolution.trace.attempts,
                logoResolution.trace.pngSuccesses,
                logoResolution.trace.svgSuccesses,
                logoResolution.trace.icoSuccesses,
                logoResolution.trace.selectedSourceUrl,
                logoResolution.trace.selectedFormat,
                logoResolution.trace.terminalReason,
                logoResolution.trace.elapsedMs,
                logoResolution.trace.aiFallbackInvoked,
                logoResolution.trace.aiCallMs,
                logoResolution.trace.aiCandidateCountRaw,
                logoResolution.trace.aiCandidateCountAccepted,
                logoResolution.trace.aiCandidateRejectReasons,
                logoResolution.trace.phaseTerminalReason
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
                logoStorageKey = logoResolution.storageKey,
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
        selectedPages: List<tech.dokus.backend.services.business.CrawledBusinessPage>,
        failedCandidateUrls: List<String>,
        failedCandidatePages: List<tech.dokus.backend.services.business.CrawledBusinessPage>,
    ): LogoResolutionResult {
        val existingProfile = profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId)
        if (existingProfile?.logoPinned == true) {
            return LogoResolutionResult(
                storageKey = null,
                trace = emptyLogoTrace()
            )
        }
        if (!existingProfile?.logoStorageKey.isNullOrBlank()) {
            return LogoResolutionResult(
                storageKey = null,
                trace = emptyLogoTrace()
            )
        }

        if (job.subjectType == BusinessProfileSubjectType.Tenant) {
            val tenantAvatar = tenantRepository.getAvatarStorageKey(job.tenantId)
            if (!tenantAvatar.isNullOrBlank()) {
                return LogoResolutionResult(
                    storageKey = null,
                    trace = emptyLogoTrace()
                )
            }
        }

        val deterministicCandidates = selectedPages.flatMap { it.logoCandidates }
        val pipelineStartedAtNanos = System.nanoTime()
        fun elapsedMs(): Long = (System.nanoTime() - pipelineStartedAtNanos) / 1_000_000
        fun remainingBudgetMs(): Long = (LogoPipelineTotalBudgetMs - elapsedMs()).coerceAtLeast(0)

        val deterministicSelection = logoSelectionService.selectPreferredLogo(
            websiteUrl = websiteUrl,
            logoCandidates = deterministicCandidates,
            budgetMs = remainingBudgetMs(),
            phaseLabel = "DETERMINISTIC"
        )

        var aiFallbackInvoked = false
        var aiCallMs = 0L
        var aiCandidateCountRaw = 0
        var aiCandidateCountAccepted = 0
        var aiCandidateRejectReasons: Map<String, Int> = emptyMap()
        var aiSelection: tech.dokus.backend.services.business.LogoSelectionResult? = null
        var fallbackWithoutSelection = false

        if (deterministicSelection.image == null && remainingBudgetMs() > 0L) {
            aiFallbackInvoked = true
            val aiInput = buildLogoFallbackInput(
                selectedWebsiteUrl = websiteUrl,
                failedCandidateUrls = failedCandidateUrls,
                selectedPages = selectedPages,
                failedPages = failedCandidatePages
            )
            val aiStartedAtNanos = System.nanoTime()
            val aiBudgetMs = remainingBudgetMs()
            val aiResult = if (aiBudgetMs <= 0L) {
                null
            } else {
                withTimeoutOrNull(aiBudgetMs) {
                    runSuspendCatching { logoFallbackAgent.findLogoCandidates(aiInput) }
                        .onFailure { error ->
                            logger.warn(
                                "Logo AI fallback failed for subjectType={}, subjectId={}, website={}, error={}",
                                job.subjectType,
                                job.subjectId,
                                websiteUrl,
                                error.message
                            )
                        }
                        .getOrNull()
                }
            }
            aiCallMs = ((System.nanoTime() - aiStartedAtNanos) / 1_000_000).coerceAtLeast(0)
            if (aiResult == null) {
                fallbackWithoutSelection = true
                aiCandidateRejectReasons = mapOf("ai_no_result" to 1)
            }

            val rawCandidates = aiResult?.candidates.orEmpty()
            aiCandidateCountRaw = rawCandidates.size
            val knownAssetHosts = collectKnownAssetHosts(
                websiteUrl = websiteUrl,
                pages = selectedPages + failedCandidatePages
            )
            val validated = logoSelectionService.validateAiFallbackCandidates(
                selectedWebsiteUrl = websiteUrl,
                knownAssetHosts = knownAssetHosts,
                aiCandidates = rawCandidates
            )
            aiCandidateCountAccepted = validated.acceptedUrls.size
            aiCandidateRejectReasons = mergeRejectReasons(aiCandidateRejectReasons, validated.rejectReasons)

            if (validated.acceptedUrls.isNotEmpty() && remainingBudgetMs() > 0L) {
                aiSelection = logoSelectionService.selectPreferredLogo(
                    websiteUrl = websiteUrl,
                    logoCandidates = validated.acceptedUrls,
                    budgetMs = remainingBudgetMs(),
                    phaseLabel = "AI"
                )
            } else if (validated.acceptedUrls.isEmpty()) {
                fallbackWithoutSelection = true
            }
        }

        val mergedTrace = mergeLogoSelectionTrace(
            deterministicTrace = deterministicSelection.trace,
            aiTrace = aiSelection?.trace,
            aiFallbackInvoked = aiFallbackInvoked,
            aiCallMs = aiCallMs,
            aiCandidateCountRaw = aiCandidateCountRaw,
            aiCandidateCountAccepted = aiCandidateCountAccepted,
            aiCandidateRejectReasons = aiCandidateRejectReasons,
            totalElapsedMs = elapsedMs(),
            fallbackWithoutSelection = fallbackWithoutSelection
        )

        val preferred = aiSelection?.image ?: deterministicSelection.image
        if (preferred == null) {
            return LogoResolutionResult(
                storageKey = null,
                trace = mergedTrace
            )
        }

        val upload = runSuspendCatching {
            avatarStorageService.uploadAvatar(job.tenantId, preferred.bytes, preferred.contentType)
        }.getOrElse { error ->
            logger.warn("Failed to upload discovered logo for {}: {}", job.subjectId, error.message)
            return LogoResolutionResult(
                storageKey = null,
                trace = mergedTrace
            )
        }

        if (job.subjectType == BusinessProfileSubjectType.Tenant) {
            runSuspendCatching { tenantRepository.updateAvatarStorageKey(job.tenantId, upload.storageKeyPrefix) }
                .onFailure { logger.warn("Failed to apply discovered tenant logo for {}: {}", job.tenantId, it.message) }
        }
        return LogoResolutionResult(
            storageKey = upload.storageKeyPrefix,
            trace = mergedTrace
        )
    }

    private fun mergeLogoSelectionTrace(
        deterministicTrace: LogoSelectionTrace,
        aiTrace: LogoSelectionTrace?,
        aiFallbackInvoked: Boolean,
        aiCallMs: Long,
        aiCandidateCountRaw: Int,
        aiCandidateCountAccepted: Int,
        aiCandidateRejectReasons: Map<String, Int>,
        totalElapsedMs: Long,
        fallbackWithoutSelection: Boolean,
    ): LogoSelectionTrace {
        val terminalTrace = aiTrace ?: deterministicTrace
        val phaseTerminalReason = when {
            aiTrace != null -> aiTrace.phaseTerminalReason
            fallbackWithoutSelection -> "AI_NO_ACCEPTED_CANDIDATES"
            else -> deterministicTrace.phaseTerminalReason
        }
        return terminalTrace.copy(
            initialCandidates = deterministicTrace.initialCandidates + (aiTrace?.initialCandidates ?: 0),
            fallbackCandidatesAppended = deterministicTrace.fallbackCandidatesAppended + (aiTrace?.fallbackCandidatesAppended
                ?: 0),
            totalCandidates = deterministicTrace.totalCandidates + (aiTrace?.totalCandidates ?: 0),
            attempts = deterministicTrace.attempts + (aiTrace?.attempts ?: 0),
            pngSuccesses = deterministicTrace.pngSuccesses + (aiTrace?.pngSuccesses ?: 0),
            svgSuccesses = deterministicTrace.svgSuccesses + (aiTrace?.svgSuccesses ?: 0),
            icoSuccesses = deterministicTrace.icoSuccesses + (aiTrace?.icoSuccesses ?: 0),
            elapsedMs = totalElapsedMs,
            aiFallbackInvoked = aiFallbackInvoked,
            aiCallMs = aiCallMs,
            aiCandidateCountRaw = aiCandidateCountRaw,
            aiCandidateCountAccepted = aiCandidateCountAccepted,
            aiCandidateRejectReasons = aiCandidateRejectReasons,
            phaseTerminalReason = phaseTerminalReason
        )
    }

    private fun emptyLogoTrace(): LogoSelectionTrace {
        return LogoSelectionTrace(
            initialCandidates = 0,
            fallbackCandidatesAppended = 0,
            totalCandidates = 0,
            attempts = 0,
            pngSuccesses = 0,
            svgSuccesses = 0,
            icoSuccesses = 0,
            terminalReason = null,
            elapsedMs = 0,
            aiFallbackInvoked = false,
            aiCallMs = 0,
            aiCandidateCountRaw = 0,
            aiCandidateCountAccepted = 0,
            aiCandidateRejectReasons = emptyMap(),
            phaseTerminalReason = null
        )
    }

    private fun buildLogoFallbackInput(
        selectedWebsiteUrl: String,
        failedCandidateUrls: List<String>,
        selectedPages: List<tech.dokus.backend.services.business.CrawledBusinessPage>,
        failedPages: List<tech.dokus.backend.services.business.CrawledBusinessPage>,
    ): BusinessLogoFallbackInput {
        val pages = (selectedPages + failedPages)
            .distinctBy { it.url }
            .take(15)
            .map { page ->
                BusinessLogoFallbackPage(
                    url = page.url,
                    title = page.title,
                    description = page.description,
                    headHtmlSnippet = page.headHtmlSnippet?.take(6_000),
                    logoRelevantHtmlSnippet = page.logoRelevantHtmlSnippet?.take(8_000),
                    structuredDataSnippets = page.structuredDataSnippets
                        .map { it.take(1_200) }
                        .take(6),
                    assetUrls = page.logoCandidates.take(20)
                )
            }
        return BusinessLogoFallbackInput(
            selectedWebsiteUrl = selectedWebsiteUrl,
            failedCandidateUrls = failedCandidateUrls.take(3),
            pages = pages
        )
    }

    private fun collectKnownAssetHosts(
        websiteUrl: String,
        pages: List<tech.dokus.backend.services.business.CrawledBusinessPage>
    ): Set<String> {
        val hosts = linkedSetOf<String>()
        runCatching { URI(websiteUrl).host }
            .getOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
            ?.let(hosts::add)
        pages.forEach { page ->
            runCatching { URI(page.url).host }
                .getOrNull()
                ?.lowercase()
                ?.removePrefix("www.")
                ?.takeIf { it.isNotBlank() }
                ?.let(hosts::add)
            page.logoCandidates.forEach { candidate ->
                runCatching { URI(candidate).host }
                    .getOrNull()
                    ?.lowercase()
                    ?.removePrefix("www.")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(hosts::add)
            }
            extractAbsoluteUrls(page.headHtmlSnippet).forEach { candidate ->
                runCatching { URI(candidate).host }
                    .getOrNull()
                    ?.lowercase()
                    ?.removePrefix("www.")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(hosts::add)
            }
            extractAbsoluteUrls(page.logoRelevantHtmlSnippet).forEach { candidate ->
                runCatching { URI(candidate).host }
                    .getOrNull()
                    ?.lowercase()
                    ?.removePrefix("www.")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(hosts::add)
            }
        }
        return hosts
    }

    private fun extractAbsoluteUrls(snippet: String?): List<String> {
        if (snippet.isNullOrBlank()) return emptyList()
        return AbsoluteHttpUrlRegex.findAll(snippet)
            .map { it.value.trim().trimEnd('"', '\'', ')', ']', '>') }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
            .take(30)
            .toList()
    }

    private fun mergeRejectReasons(
        left: Map<String, Int>,
        right: Map<String, Int>
    ): Map<String, Int> {
        if (left.isEmpty()) return right
        if (right.isEmpty()) return left
        val merged = left.toMutableMap()
        right.forEach { (key, value) ->
            merged[key] = (merged[key] ?: 0) + value
        }
        return merged
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
