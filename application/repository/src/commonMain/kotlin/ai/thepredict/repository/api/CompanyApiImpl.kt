package ai.thepredict.repository.api

import ai.thepredict.apispec.CompanyApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Company
import ai.thepredict.domain.model.CreateCompanyRequest
import ai.thepredict.domain.model.UpdateCompanyRequest
import ai.thepredict.repository.extensions.bodyIfOk
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class CompanyApiImpl(
    private val client: HttpClient,
) : CompanyApi {
    private val basePath = "/api/v1/companies"

    override suspend fun getCompanies(): Result<List<Company>> {
        return runCatching {
            client.get(basePath).bodyIfOk()
        }
    }

    override suspend fun createCompany(request: CreateCompanyRequest): Result<Company> {
        return runCatching {
            client.post(basePath) {
                setBody(request)
            }.body()
        }
    }

    override suspend fun getCompany(companyId: String): Result<Company> {
        return runCatching {
            client.get("$basePath/$companyId").body()
        }
    }

    override suspend fun updateCompany(
        companyId: String,
        request: UpdateCompanyRequest
    ): Result<Company> {
        return runCatching {
            client.put("$basePath/$companyId") {
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteCompany(companyId: String): Result<Unit> {
        return runCatching {
            client.delete("$basePath/$companyId")
        }
    }

    override suspend fun checkCompanyExists(companyId: String): Result<Boolean> {
        return runCatching {
            client.get("$basePath/$companyId/exists").body()
        }
    }
}

internal fun CompanyApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): CompanyApi {
    return CompanyApiImpl(
        client = httpClient,
    )
}