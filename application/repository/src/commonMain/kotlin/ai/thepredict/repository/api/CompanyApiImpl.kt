package ai.thepredict.repository.api

import ai.thepredict.apispec.CompanyApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Company
import ai.thepredict.domain.model.CreateCompanyRequest
import ai.thepredict.domain.model.UpdateCompanyRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class CompanyApiImpl(
    private val client: HttpClient,
) : CompanyApi {
    private val basePath = "/api/v1/companies"

    override suspend fun getCompanies(): List<Company> {
        return client.get(basePath).body()
    }

    override suspend fun createCompany(request: CreateCompanyRequest): Company {
        return client.post(basePath) {
            setBody(request)
        }.body()
    }

    override suspend fun getCompany(companyId: String): Company {
        return client.get("$basePath/$companyId").body()
    }

    override suspend fun updateCompany(companyId: String, request: UpdateCompanyRequest): Company {
        return client.put("$basePath/$companyId") {
            setBody(request)
        }.body()
    }

    override suspend fun deleteCompany(companyId: String) {
        client.delete("$basePath/$companyId")
    }

    override suspend fun checkCompanyExists(companyId: String): Boolean {
        return client.get("$basePath/$companyId/exists").body()
    }
}

internal fun CompanyApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): CompanyApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return CompanyApiImpl(
        client = httpClient,
    )
}