package tech.dokus.domain.usecases

import tech.dokus.domain.model.entity.EntityLookupResponse

/**
 * Use case for searching companies by name or VAT number.
 *
 * Searches the CBE (Crossroads Bank for Enterprises) for Belgian companies.
 * Used during contact creation to lookup and prefill company data.
 *
 * @see EntityLookupResponse for the result format
 */
interface SearchCompanyUseCase {
    /**
     * Search for companies matching the given query.
     *
     * @param query Company name or VAT number (minimum 3 characters for name search)
     * @return List of matching companies wrapped in Result
     */
    suspend operator fun invoke(query: String): Result<EntityLookupResponse>
}
