package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.CreateTenantRequest
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.domain.routes.Tenants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.uuid.ExperimentalUuidApi

/**
 * HTTP implementation of TenantRemoteDataSource.
 * Uses authenticated Ktor HttpClient with type-safe routing to communicate with the tenant service.
 */
@OptIn(ExperimentalUuidApi::class)
internal class TenantRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : TenantRemoteDataSource {

    override suspend fun listMyTenants(): Result<List<Tenant>> {
        return runCatching {
            httpClient.get(Tenants()).body()
        }
    }

    override suspend fun createTenant(request: CreateTenantRequest): Result<Tenant> {
        return runCatching {
            httpClient.post(Tenants()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getTenant(id: TenantId): Result<Tenant> {
        return runCatching {
            httpClient.get(Tenants.Id(id = id.value.toString())).body()
        }
    }

    override suspend fun getTenantSettings(): Result<TenantSettings> {
        return runCatching {
            httpClient.get(Tenants.Settings()).body()
        }
    }

    override suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit> {
        return runCatching {
            httpClient.put(Tenants.Settings()) {
                contentType(ContentType.Application.Json)
                setBody(settings)
            }
        }
    }

    // Note: getNextInvoiceNumber and hasFreelancerTenant were removed as they
    // were RPC-style endpoints. These should be computed client-side or included
    // in existing resource responses.
}
