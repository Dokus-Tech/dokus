package tech.dokus.backend.services.search

import tech.dokus.database.repository.search.SearchRepository
import tech.dokus.database.repository.search.SearchSignalRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.SearchPreset
import tech.dokus.domain.model.SearchResultEntityType
import tech.dokus.domain.model.SearchSignalEventRequest
import tech.dokus.domain.model.SearchSignalEventType
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope
import tech.dokus.foundation.backend.utils.loggerFor

class SearchService(
    private val searchRepository: SearchRepository,
    private val searchSignalRepository: SearchSignalRepository,
) {
    private val logger = loggerFor()

    suspend fun unifiedSearch(
        tenantId: TenantId,
        userId: UserId,
        query: String,
        scope: UnifiedSearchScope,
        preset: SearchPreset? = null,
        limit: Int = 20,
        suggestionLimit: Int = 8
    ): Result<UnifiedSearchResponse> {
        logger.debug(
            "Unified search for tenant=$tenantId user=$userId queryLength=${query.trim().length} scope=$scope limit=$limit preset=$preset"
        )

        return searchRepository.search(
            tenantId = tenantId,
            userId = userId,
            query = query,
            scope = scope,
            preset = preset,
            limit = limit,
            suggestionLimit = suggestionLimit,
        ).onFailure { error ->
            logger.error("Unified search failed for tenant=$tenantId", error)
        }
    }

    suspend fun recordSignal(
        tenantId: TenantId,
        userId: UserId,
        request: SearchSignalEventRequest,
    ): Result<Unit> {
        val normalizedInput = when (request.eventType) {
            SearchSignalEventType.QueryCommitted -> request.query
            SearchSignalEventType.SuggestionSelected -> request.suggestionLabel ?: request.query
            SearchSignalEventType.ResultOpened -> resolveResultOpenedLabel(
                tenantId = tenantId,
                entityType = request.resultEntityType,
                entityId = request.resultEntityId,
                fallbackQuery = request.query,
            )
        }?.trim().orEmpty()

        val normalized = normalizeSuggestionKey(normalizedInput)
        if (!isSignalTextTrackable(normalized)) {
            return Result.success(Unit)
        }

        val displayText = normalizedInput.ifBlank { normalized }

        return searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = request.eventType,
            normalizedText = normalized,
            displayText = displayText,
        ).onFailure { error ->
            logger.error("Failed to record search signal for tenant=$tenantId user=$userId", error)
        }
    }

    private suspend fun resolveResultOpenedLabel(
        tenantId: TenantId,
        entityType: SearchResultEntityType?,
        entityId: String?,
        fallbackQuery: String?,
    ): String? {
        if (entityType == null || entityId.isNullOrBlank()) {
            return fallbackQuery
        }

        return searchSignalRepository.resolveEntityLabel(
            tenantId = tenantId,
            entityType = entityType,
            entityId = entityId,
        ).getOrNull() ?: fallbackQuery
    }

    private fun normalizeSuggestionKey(value: String): String =
        value.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

    private fun isSignalTextTrackable(normalized: String): Boolean {
        if (normalized.length !in 2..80) return false
        if (normalized == "overdue" || normalized == "paid") return false
        return true
    }
}
