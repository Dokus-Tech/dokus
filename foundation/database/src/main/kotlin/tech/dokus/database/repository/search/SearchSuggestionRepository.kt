package tech.dokus.database.repository.search

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.SearchPreset
import tech.dokus.domain.model.SearchSignalEventType
import tech.dokus.domain.model.SearchSuggestion
import tech.dokus.domain.model.UnifiedSearchScope

private const val UserSignalTypeQueryCommittedWeight = 40L
private const val UserSignalTypeSuggestionSelectedWeight = 70L
private const val UserSignalTypeResultOpenedWeight = 100L
private const val TenantSignalWeight = 10L
private const val PresetBaseScore = 80L
private val BlockedSuggestionLabels = setOf("overdue", "paid")

class SearchSuggestionRepository(
    private val searchSignalRepository: SearchSignalRepository,
    private val personalizationQueries: SearchPersonalizationQueries,
) {

    suspend fun personalizedSuggestions(
        tenantId: TenantId,
        userId: UserId,
        limit: Int,
    ): List<SearchSuggestion> {
        val safeLimit = limit.coerceIn(1, 50)
        val now = Clock.System.now()

        val userCandidates = userSignalCandidates(
            tenantId = tenantId,
            userId = userId,
            now = now,
        )
        val tenantCandidates = tenantEntityCandidates(
            tenantId = tenantId,
            now = now,
        )
        val presetCandidates = presetCandidates(
            tenantId = tenantId,
            now = now,
        )

        return mergeAndRankSuggestions(
            candidates = userCandidates + tenantCandidates + presetCandidates,
            limit = safeLimit,
        )
    }

    suspend fun presetSearch(
        tenantId: TenantId,
        preset: SearchPreset,
        limit: Int,
    ): PresetSearchResult = personalizationQueries.presetSearch(
        tenantId = tenantId,
        preset = preset,
        limit = limit,
    )

    private suspend fun userSignalCandidates(
        tenantId: TenantId,
        userId: UserId,
        now: Instant,
    ): List<SuggestionCandidate> {
        val signals = searchSignalRepository.topUserSignals(
            tenantId = tenantId,
            userId = userId,
            limit = 80,
        ).getOrElse { emptyList() }

        return signals.mapNotNull { signal ->
            val normalized = normalizeSuggestionKey(signal.displayText)
            if (!isValidSuggestionKey(normalized)) {
                return@mapNotNull null
            }
            val score = signal.count * signal.signalType.weight + recencyBonus(signal.lastSeenAt, now)
            SuggestionCandidate(
                normalizedKey = normalized,
                label = signal.displayText,
                countHint = signal.count,
                score = score,
                lastSeenAt = signal.lastSeenAt,
                actionQuery = signal.displayText,
                actionScope = null,
                actionPreset = null,
            )
        }
    }

    private suspend fun tenantEntityCandidates(
        tenantId: TenantId,
        now: Instant,
    ): List<SuggestionCandidate> {
        val grouped = linkedMapOf<String, TenantEntityCounter>()
        personalizationQueries.tenantEntitySamples(tenantId).forEach { sample ->
            val normalized = normalizeSuggestionKey(sample.label)
            if (!isValidSuggestionKey(normalized)) return@forEach
            val existing = grouped[normalized]
            if (existing == null) {
                grouped[normalized] = TenantEntityCounter(
                    label = sample.label,
                    count = 1L,
                    lastSeenAt = sample.seenAt,
                )
            } else {
                grouped[normalized] = existing.copy(
                    label = if (sample.seenAt > existing.lastSeenAt) sample.label else existing.label,
                    count = existing.count + 1L,
                    lastSeenAt = maxOf(existing.lastSeenAt, sample.seenAt),
                )
            }
        }

        return grouped.map { (normalized, counter) ->
            SuggestionCandidate(
                normalizedKey = normalized,
                label = counter.label,
                countHint = counter.count,
                score = counter.count * TenantSignalWeight + recencyBonus(counter.lastSeenAt, now),
                lastSeenAt = counter.lastSeenAt,
                actionQuery = counter.label,
                actionScope = null,
                actionPreset = null,
            )
        }
    }

    private suspend fun presetCandidates(
        tenantId: TenantId,
        now: Instant,
    ): List<SuggestionCandidate> {
        val overdueCount = personalizationQueries.presetCount(tenantId, SearchPreset.OverdueInvoices)
        val upcomingCount = personalizationQueries.presetCount(tenantId, SearchPreset.UpcomingPayments)
        val nowLocal = now.toLocalDateTime(TimeZone.UTC)

        val candidates = mutableListOf<SuggestionCandidate>()
        if (overdueCount > 0L) {
            val label = "Overdue invoices"
            candidates += SuggestionCandidate(
                normalizedKey = normalizeSuggestionKey(label),
                label = label,
                countHint = overdueCount,
                score = PresetBaseScore + overdueCount,
                lastSeenAt = nowLocal,
                actionQuery = label,
                actionScope = UnifiedSearchScope.Transactions,
                actionPreset = SearchPreset.OverdueInvoices,
            )
        }
        if (upcomingCount > 0L) {
            val label = "Upcoming payments"
            candidates += SuggestionCandidate(
                normalizedKey = normalizeSuggestionKey(label),
                label = label,
                countHint = upcomingCount,
                score = PresetBaseScore + upcomingCount,
                lastSeenAt = nowLocal,
                actionQuery = label,
                actionScope = UnifiedSearchScope.Transactions,
                actionPreset = SearchPreset.UpcomingPayments,
            )
        }
        return candidates
    }

    private fun mergeAndRankSuggestions(
        candidates: List<SuggestionCandidate>,
        limit: Int,
    ): List<SearchSuggestion> {
        if (candidates.isEmpty()) return emptyList()

        val merged = linkedMapOf<String, SuggestionCandidate>()
        candidates.forEach { candidate ->
            val existing = merged[candidate.normalizedKey]
            merged[candidate.normalizedKey] = if (existing == null) {
                candidate
            } else {
                mergeCandidates(existing, candidate)
            }
        }

        return merged.values
            .sortedWith(
                compareByDescending<SuggestionCandidate> { it.score }
                    .thenByDescending { it.countHint }
                    .thenBy { it.label.lowercase() }
            )
            .take(limit)
            .map { candidate ->
                SearchSuggestion(
                    label = candidate.label,
                    countHint = candidate.countHint,
                    actionQuery = candidate.actionQuery,
                    actionScope = candidate.actionScope,
                    actionPreset = candidate.actionPreset,
                )
            }
    }

    private fun mergeCandidates(
        first: SuggestionCandidate,
        second: SuggestionCandidate,
    ): SuggestionCandidate {
        val latestCandidate = if (second.lastSeenAt > first.lastSeenAt) second else first

        return SuggestionCandidate(
            normalizedKey = first.normalizedKey,
            label = latestCandidate.label,
            countHint = first.countHint + second.countHint,
            score = first.score + second.score,
            lastSeenAt = latestCandidate.lastSeenAt,
            actionQuery = latestCandidate.actionQuery,
            actionScope = latestCandidate.actionScope,
            actionPreset = latestCandidate.actionPreset,
        )
    }

    private fun recencyBonus(
        lastSeenAt: LocalDateTime,
        now: Instant,
    ): Long {
        val nowDate = now.toLocalDateTime(TimeZone.UTC).date
        val elapsedDays = lastSeenAt.date.daysUntil(nowDate).coerceAtLeast(0)
        return when {
            elapsedDays <= 1 -> 30L
            elapsedDays <= 7 -> 20L
            elapsedDays <= 30 -> 10L
            else -> 0L
        }
    }

    private fun normalizeSuggestionKey(label: String): String =
        label.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

    private fun isValidSuggestionKey(normalized: String): Boolean {
        if (normalized.length !in 2..80) return false
        if (normalized in BlockedSuggestionLabels) return false
        return true
    }
}

private val SearchSignalEventType.weight: Long
    get() = when (this) {
        SearchSignalEventType.QueryCommitted -> UserSignalTypeQueryCommittedWeight
        SearchSignalEventType.SuggestionSelected -> UserSignalTypeSuggestionSelectedWeight
        SearchSignalEventType.ResultOpened -> UserSignalTypeResultOpenedWeight
    }

private data class SuggestionCandidate(
    val normalizedKey: String,
    val label: String,
    val countHint: Long,
    val score: Long,
    val lastSeenAt: LocalDateTime,
    val actionQuery: String?,
    val actionScope: UnifiedSearchScope?,
    val actionPreset: SearchPreset?,
)

private data class TenantEntityCounter(
    val label: String,
    val count: Long,
    val lastSeenAt: LocalDateTime,
)
