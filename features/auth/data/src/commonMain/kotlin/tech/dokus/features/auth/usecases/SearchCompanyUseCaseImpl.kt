package tech.dokus.features.auth.usecases

import tech.dokus.features.auth.datasource.LookupRemoteDataSource
import tech.dokus.foundation.platform.Logger
import tech.dokus.domain.model.entity.EntityLookupResponse
import tech.dokus.domain.usecases.SearchCompanyUseCase

/**
 * Implementation of [SearchCompanyUseCase] that uses CBE API via [LookupRemoteDataSource].
 *
 * Searches for companies by name or VAT number. Used during contact creation
 * to lookup and prefill company data from official sources.
 */
class SearchCompanyUseCaseImpl(
    private val lookupDataSource: LookupRemoteDataSource,
) : SearchCompanyUseCase {

    private val logger = Logger.forClass<SearchCompanyUseCaseImpl>()

    /**
     * Search for companies matching the given query.
     *
     * @param query Company name or VAT number (minimum 3 characters for name search)
     * @return List of matching companies wrapped in Result
     */
    override suspend fun invoke(query: String): Result<EntityLookupResponse> {
        logger.d { "Searching for company: $query" }

        return lookupDataSource.searchCompany(query)
            .onSuccess { response ->
                logger.d { "Found ${response.totalCount} companies for '$query'" }
            }
            .onFailure { error ->
                logger.e(error) { "Company search failed for '$query'" }
            }
    }
}
