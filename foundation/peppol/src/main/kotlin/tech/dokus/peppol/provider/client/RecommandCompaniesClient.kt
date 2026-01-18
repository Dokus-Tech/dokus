package tech.dokus.peppol.provider.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import tech.dokus.peppol.provider.client.recommand.model.RecommandCompany
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreateCompanyRequest
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreateCompanyResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandGetCompaniesResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandUpdateCompanyRequest
import tech.dokus.peppol.provider.client.recommand.model.RecommandUpdateCompanyResponse
import tech.dokus.peppol.config.PeppolProviderConfig

class RecommandCompaniesClient(
    private val httpClient: HttpClient,
) {
    private val baseUrl = PeppolProviderConfig.Recommand.baseUrl

    suspend fun listCompanies(
        apiKey: String,
        apiSecret: String,
        vatNumber: String,
    ): Result<List<RecommandCompany>> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/companies") {
            basicAuth(apiKey, apiSecret)
            parameter("vatNumber", vatNumber)
        }

        if (response.status.value == 401) {
            throw RecommandUnauthorizedException(response.status.value, response.bodyAsText())
        }

        if (!response.status.isSuccess()) {
            throw RecommandApiException(response.status.value, response.bodyAsText())
        }

        response.body<RecommandGetCompaniesResponse>().companies
    }

    suspend fun createCompany(
        apiKey: String,
        apiSecret: String,
        request: RecommandCreateCompanyRequest,
    ): Result<RecommandCompany> = runCatching {
        val response = httpClient.post("$baseUrl/api/v1/companies") {
            basicAuth(apiKey, apiSecret)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status.value == 401) {
            throw RecommandUnauthorizedException(response.status.value, response.bodyAsText())
        }

        if (!response.status.isSuccess()) {
            throw RecommandApiException(response.status.value, response.bodyAsText())
        }

        response.body<RecommandCreateCompanyResponse>().company
    }

    suspend fun updateCompany(
        apiKey: String,
        apiSecret: String,
        companyId: String,
        request: RecommandUpdateCompanyRequest,
    ): Result<RecommandCompany> = runCatching {
        val response = httpClient.put("$baseUrl/api/v1/companies/$companyId") {
            basicAuth(apiKey, apiSecret)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status.value == 401) {
            throw RecommandUnauthorizedException(response.status.value, response.bodyAsText())
        }

        if (!response.status.isSuccess()) {
            throw RecommandApiException(response.status.value, response.bodyAsText())
        }

        response.body<RecommandUpdateCompanyResponse>().company
    }
}

class RecommandUnauthorizedException(
    val statusCode: Int,
    val responseBody: String,
) : Exception("Recommand API unauthorized (HTTP $statusCode): $responseBody")
