package ai.dokus.app.auth.repository

import ai.dokus.app.auth.datasource.LookupRemoteDataSource
import ai.dokus.foundation.domain.model.EntityLookupResponse
import ai.dokus.foundation.platform.Logger

/**
 * Repository for external data lookups (CBE company search, etc.)
 */
class LookupRepository(
    private val lookupDataSource: LookupRemoteDataSource,
) {
    private val logger = Logger.forClass<LookupRepository>()

    /**
     * Search for companies by name using CBE API.
     * @param name Company name to search for (min 3 characters)
     * @return Result containing EntityLookupResponse with matching companies
     */
    suspend fun searchCompany(name: String): Result<EntityLookupResponse> {
        logger.d { "Searching for company: $name" }
        return lookupDataSource.searchCompany(name)
            .onSuccess { response ->
                logger.d { "Found ${response.totalCount} companies for '$name'" }
            }
            .onFailure { error ->
                logger.e(error) { "Company search failed for '$name'" }
            }
    }
}
