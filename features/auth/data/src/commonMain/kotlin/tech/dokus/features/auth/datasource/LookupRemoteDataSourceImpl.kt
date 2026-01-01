package tech.dokus.features.auth.datasource

import tech.dokus.domain.model.entity.EntityLookupResponse
import tech.dokus.domain.routes.Lookup
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get

/**
 * HTTP implementation of LookupRemoteDataSource.
 * Uses authenticated Ktor HttpClient with type-safe routing for external lookups.
 */
internal class LookupRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : LookupRemoteDataSource {

    override suspend fun searchCompany(name: String): Result<EntityLookupResponse> {
        return runCatching {
            httpClient.get(Lookup.Company(name = name)).body()
        }
    }
}
