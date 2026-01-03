package tech.dokus.features.auth.usecases

import tech.dokus.domain.LegalName
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityLookupResponse
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.auth.datasource.LookupRemoteDataSource
import tech.dokus.foundation.platform.Logger

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
     * @param this@invoke Company name or VAT number (minimum 3 characters for name search)
     * @return List of matching companies wrapped in Result
     */
    override suspend fun invoke(
        name: LegalName?,
        number: VatNumber?
    ): Result<EntityLookupResponse> {
        logger.d { "Searching for company: $name, $number" }

        return lookupDataSource.searchCompany(name, number)
            .onSuccess { response ->
                logger.d { "Found ${response.totalCount} companies for '${name}, ${number}'" }
            }
            .onFailure { error ->
                logger.e(error) { "Company search failed for '${name}, ${number}'" }
            }
    }
}
