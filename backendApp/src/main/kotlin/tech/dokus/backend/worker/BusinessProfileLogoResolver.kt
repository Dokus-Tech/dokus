@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.worker

import kotlinx.coroutines.withTimeoutOrNull
import tech.dokus.backend.services.business.BusinessLogoSelectionService
import tech.dokus.backend.services.business.CrawledBusinessPage
import tech.dokus.backend.services.business.LogoPipelineTotalBudgetMs
import tech.dokus.backend.services.business.LogoSelectionResult
import tech.dokus.backend.services.business.LogoSelectionTrace
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.entity.BusinessProfileEnrichmentJobEntity
import tech.dokus.database.repository.business.BusinessProfileRepository
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.features.ai.agents.BusinessLogoFallbackAgent
import tech.dokus.features.ai.models.BusinessLogoFallbackInput
import tech.dokus.features.ai.models.BusinessLogoFallbackPage
import tech.dokus.features.ai.queue.LlmQueue
import tech.dokus.features.ai.queue.businessEnrichment
import tech.dokus.foundation.backend.storage.AvatarStorageService
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.net.URI
import kotlin.uuid.ExperimentalUuidApi

private val AbsoluteHttpUrlRegex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)

internal class BusinessProfileLogoResolver(
    private val profileRepository: BusinessProfileRepository,
    private val tenantRepository: TenantRepository,
    private val avatarStorageService: AvatarStorageService,
    private val logoFallbackAgent: BusinessLogoFallbackAgent,
    private val logoSelectionService: BusinessLogoSelectionService,
    private val llmQueue: LlmQueue,
) {
    private val logger = loggerFor()

    suspend fun resolve(
        job: BusinessProfileEnrichmentJobEntity,
        websiteUrl: String,
        selectedPages: List<CrawledBusinessPage>,
        failedCandidateUrls: List<String>,
        failedCandidatePages: List<CrawledBusinessPage>,
    ): LogoResolutionResult {
        val existingProfile = profileRepository.getBySubject(job.tenantId, job.subjectType, job.subjectId)
        if (existingProfile?.logoPinned == true) {
            return LogoResolutionResult(storageKey = null, trace = emptyLogoTrace())
        }
        if (!existingProfile?.logoStorageKey.isNullOrBlank()) {
            return LogoResolutionResult(storageKey = null, trace = emptyLogoTrace())
        }

        if (job.subjectType == BusinessProfileSubjectType.Tenant) {
            val tenantAvatar = tenantRepository.getAvatarStorageKey(job.tenantId)
            if (!tenantAvatar.isNullOrBlank()) {
                return LogoResolutionResult(storageKey = null, trace = emptyLogoTrace())
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
        var aiSelection: LogoSelectionResult? = null
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
                    runSuspendCatching {
                        llmQueue.businessEnrichment("logo-fallback:${job.subjectId}") {
                            logoFallbackAgent.findLogoCandidates(aiInput)
                        }
                    }.onFailure { error ->
                        logger.warn(
                            "Logo AI fallback failed for subjectType={}, subjectId={}, website={}, error={}",
                            job.subjectType,
                            job.subjectId,
                            websiteUrl,
                            error.message
                        )
                    }.getOrNull()
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
            return LogoResolutionResult(storageKey = null, trace = mergedTrace)
        }

        val upload = runSuspendCatching {
            avatarStorageService.uploadAvatar(job.tenantId, preferred.bytes, preferred.contentType)
        }.getOrElse { error ->
            logger.warn("Failed to upload discovered logo for {}: {}", job.subjectId, error.message)
            return LogoResolutionResult(storageKey = null, trace = mergedTrace)
        }

        if (job.subjectType == BusinessProfileSubjectType.Tenant) {
            runSuspendCatching { tenantRepository.updateAvatarStorageKey(job.tenantId, upload.storageKeyPrefix) }
                .onFailure {
                    logger.warn(
                        "Failed to apply discovered tenant logo for {}: {}",
                        job.tenantId,
                        it.message
                    )
                }
        }
        return LogoResolutionResult(storageKey = upload.storageKeyPrefix, trace = mergedTrace)
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
            fallbackCandidatesAppended = deterministicTrace.fallbackCandidatesAppended +
                (aiTrace?.fallbackCandidatesAppended ?: 0),
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
        selectedPages: List<CrawledBusinessPage>,
        failedPages: List<CrawledBusinessPage>,
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
                    structuredDataSnippets = page.structuredDataSnippets.map { it.take(1_200) }.take(6),
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
        pages: List<CrawledBusinessPage>
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
}
