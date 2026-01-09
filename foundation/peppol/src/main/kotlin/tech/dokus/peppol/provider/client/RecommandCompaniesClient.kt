package tech.dokus.peppol.provider.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
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

        response.body<RecommandListCompaniesResponse>().companies
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
}

@Serializable
data class RecommandCompany(
    val id: String,
    val teamId: String,
    val name: String,
    val address: String,
    val postalCode: String,
    val city: String,
    val country: String,
    val enterpriseNumber: String,
    val vatNumber: String,
    val isSmpRecipient: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class RecommandCreateCompanyRequest(
    val name: String,
    val address: String,
    val postalCode: String,
    val city: String,
    val country: String,
    val enterpriseNumber: String? = null,
    val vatNumber: String? = null,
    val isSmpRecipient: Boolean = true,
    val skipDefaultCompanySetup: Boolean = false,
)

@Serializable
private data class RecommandListCompaniesResponse(
    val success: Boolean,
    val companies: List<RecommandCompany>,
)

@Serializable
private data class RecommandCreateCompanyResponse(
    val success: Boolean,
    val company: RecommandCompany,
)

class RecommandUnauthorizedException(
    val statusCode: Int,
    val responseBody: String,
) : Exception("Recommand API unauthorized (HTTP $statusCode): $responseBody")
