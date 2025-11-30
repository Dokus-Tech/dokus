package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.model.CreateTenantRequest
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * HTTP implementation of TenantRemoteDataSource.
 * Uses authenticated Ktor HttpClient to communicate with the tenant service.
 */
internal class TenantRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : TenantRemoteDataSource {

    override suspend fun listMyTenants(): Result<List<Tenant>> {
        return runCatching {
            httpClient.get("/api/v1/tenants").body()
        }
    }

    override suspend fun createTenant(request: CreateTenantRequest): Result<Tenant> {
        return runCatching {
            httpClient.post("/api/v1/tenants") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getTenant(id: TenantId): Result<Tenant> {
        return runCatching {
            httpClient.get("/api/v1/tenants/${id.value}").body()
        }
    }

    override suspend fun getTenantSettings(): Result<TenantSettings> {
        return runCatching {
            httpClient.get("/api/v1/tenants/settings").body()
        }
    }

    override suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit> {
        return runCatching {
            httpClient.put("/api/v1/tenants/settings") {
                contentType(ContentType.Application.Json)
                setBody(settings)
            }
        }
    }

    override suspend fun getNextInvoiceNumber(): Result<InvoiceNumber> {
        return runCatching {
            httpClient.get("/api/v1/tenants/next-invoice-number").body()
        }
    }

    override suspend fun hasFreelancerTenant(): Result<Boolean> {
        return runCatching {
            httpClient.get("/api/v1/tenants/has-freelancer").body()
        }
    }
}
