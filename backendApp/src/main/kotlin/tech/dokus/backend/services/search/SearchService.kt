package tech.dokus.backend.services.search

import tech.dokus.database.repository.search.SearchRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope
import tech.dokus.foundation.backend.utils.loggerFor

class SearchService(
    private val searchRepository: SearchRepository,
) {
    private val logger = loggerFor()

    suspend fun unifiedSearch(
        tenantId: TenantId,
        query: String,
        scope: UnifiedSearchScope,
        limit: Int = 20,
        suggestionLimit: Int = 8
    ): Result<UnifiedSearchResponse> {
        logger.debug(
            "Unified search for tenant=$tenantId queryLength=${query.trim().length} scope=$scope limit=$limit"
        )

        return searchRepository.search(
            tenantId = tenantId,
            query = query,
            scope = scope,
            limit = limit,
            suggestionLimit = suggestionLimit,
        ).onFailure { error ->
            logger.error("Unified search failed for tenant=$tenantId", error)
        }
    }
}
