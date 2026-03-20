@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.worker

import tech.dokus.backend.services.business.BusinessWebsiteProbe
import tech.dokus.backend.services.business.BusinessWebsiteRanker
import tech.dokus.backend.services.business.CrawledBusinessPage
import tech.dokus.backend.services.business.EnrichmentTrigger
import tech.dokus.backend.services.business.WebsiteCandidateInput
import tech.dokus.backend.services.business.WebsiteRankingContext
import tech.dokus.backend.services.business.WebsiteRankingDecision
import tech.dokus.backend.services.business.isAggregatorOrSocialHost
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobEntity
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.agents.BusinessProfileContentExtractionAgent
import tech.dokus.features.ai.models.BusinessProfileContentExtractionInput
import tech.dokus.features.ai.models.BusinessProfileContentExtractionResult
import tech.dokus.features.ai.models.BusinessProfileContentPage
import tech.dokus.features.ai.queue.LlmQueue
import tech.dokus.features.ai.queue.businessEnrichment
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.net.URI
import kotlin.uuid.ExperimentalUuidApi

private data class RankedCandidateWithPages(
    val input: WebsiteCandidateInput,
)

internal class BusinessProfileEnrichmentJobProcessor(
    private val config: BusinessProfileEnrichmentConfig,
    private val jobRepository: BusinessProfileEnrichmentJobRepository,
    private val businessProfileService: tech.dokus.backend.services.business.BusinessProfileService,
    private val contentExtractionAgent: BusinessProfileContentExtractionAgent,
    private val websiteProbe: BusinessWebsiteProbe,
    private val websiteRanker: BusinessWebsiteRanker,
    private val subjectContextLoader: BusinessSubjectContextLoader,
    private val logoResolver: BusinessProfileLogoResolver,
    private val llmQueue: LlmQueue,
) {
    private val logger = loggerFor()

    suspend fun process(job: BusinessProfileEnrichmentJobEntity) {
        val context = subjectContextLoader.load(job) ?: run {
            jobRepository.markCompletedWithError(job.id, "Subject not found")
            return
        }

        if (job.triggerReason == EnrichmentTrigger.WebsiteChanged.reason) {
            processWebsiteChanged(job, context)
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

        val logoResolution = logoResolver.resolve(
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
    }

    private suspend fun processWebsiteChanged(job: BusinessProfileEnrichmentJobEntity, context: BusinessSubjectContext) {
        val profile = businessProfileService.getProfileRecord(job.tenantId, job.subjectType, job.subjectId)
        val websiteUrl = profile?.websiteUrl
        if (websiteUrl.isNullOrBlank()) {
            logger.info("No website URL for website-changed enrichment, subjectType={}, subjectId={}", job.subjectType, job.subjectId)
            jobRepository.markCompleted(job.id)
            return
        }

        val crawl = websiteProbe.crawl(startUrl = websiteUrl, maxPages = config.maxPages)
        val extracted = extractBusinessContent(context = context, websiteUrl = websiteUrl, pages = crawl.pages)
        val logoResolution = logoResolver.resolve(
            job = job,
            websiteUrl = websiteUrl,
            selectedPages = crawl.pages,
            failedCandidateUrls = emptyList(),
            failedCandidatePages = emptyList()
        )

        businessProfileService.applyEnrichment(
            tenantId = job.tenantId,
            subjectType = job.subjectType,
            subjectId = job.subjectId,
            verificationState = BusinessProfileVerificationState.Verified,
            evidenceScore = 100,
            evidenceChecksJson = null,
            websiteUrl = websiteUrl,
            businessSummary = extracted?.businessSummary,
            businessActivities = extracted?.activities,
            logoStorageKey = logoResolution.storageKey,
            lastErrorCode = null,
            lastErrorMessage = null,
        )
        jobRepository.markCompleted(job.id)
        logger.info("Completed website-changed enrichment for subjectType={}, subjectId={}, website={}", job.subjectType, job.subjectId, websiteUrl)
    }

    private suspend fun extractBusinessContent(
        context: BusinessSubjectContext,
        websiteUrl: String,
        pages: List<CrawledBusinessPage>
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
                    structuredDataSnippets = page.structuredDataSnippets.map { it.take(1_200) }.take(5)
                )
            }
        )

        return runSuspendCatching {
            llmQueue.businessEnrichment("biz-content:${context.name}") {
                contentExtractionAgent.extract(input)
            }
        }.onFailure { error ->
            logger.warn(
                "Content extraction failed for website={}, company={}, error={}",
                websiteUrl,
                context.name,
                error.message
            )
        }.getOrNull()
    }

    private fun isAggregatorOrSocialUrl(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull()?.removePrefix("www.") ?: return true
        return isAggregatorOrSocialHost(host)
    }
}
