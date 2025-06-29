package ai.thepredict.repository.api

import ai.thepredict.apispec.CompanyMembersApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Role
import ai.thepredict.domain.model.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class CompanyMembersApiImpl(
    private val client: HttpClient,
) : CompanyMembersApi {
    private val basePath: String = "/api/v1/companies"

    override suspend fun listCompanyMembers(
        companyId: String,
        offset: Int,
        limit: Int
    ): List<User> {
        return client.get("$basePath/$companyId/members") {
            parameter("offset", offset)
            parameter("limit", limit)
        }.body()
    }

    override suspend fun checkCompanyMember(companyId: String, userId: String): Boolean {
        return client.get("$basePath/$companyId/members/$userId/exists").body()
    }

    override suspend fun updateUserCompanyRole(
        companyId: String,
        userId: String,
        role: Role
    ): User {
        return client.put("$basePath/$companyId/members/$userId/role") {
            setBody(role)
        }.body()
    }
}

internal fun CompanyMembersApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): CompanyMembersApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return CompanyMembersApiImpl(
        client = httpClient,
    )
}