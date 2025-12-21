package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.model.EntityLookupResponse

/**
 * Remote data source for external data lookups (CBE company search, etc.)
 */
interface LookupRemoteDataSource {
    /**
     * Search for companies by name using CBE API.
     * @param name Company name to search for (min 3 characters)
     * @return Result containing EntityLookupResponse with matching companies
     */
    suspend fun searchCompany(name: String): Result<EntityLookupResponse>
}
