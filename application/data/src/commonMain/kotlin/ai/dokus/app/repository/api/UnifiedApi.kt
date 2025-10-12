package ai.dokus.app.repository.api

import ai.dokus.foundation.apispec.AuthApi
import ai.dokus.foundation.apispec.CompanyApi
import ai.dokus.foundation.apispec.CompanyMembersApi
import ai.dokus.foundation.apispec.DocumentApi
import ai.dokus.foundation.apispec.DocumentExtractionApi
import ai.dokus.foundation.apispec.DocumentFileApi
import ai.dokus.foundation.apispec.InfoApi
import ai.dokus.foundation.apispec.MatchingApi
import ai.dokus.foundation.apispec.TransactionApi
import ai.dokus.foundation.apispec.TransactionExtractionApi
import ai.dokus.foundation.apispec.TransactionFileApi
import ai.dokus.foundation.apispec.TransactionMatchingApi
import ai.dokus.foundation.apispec.UserApi
import ai.dokus.foundation.platform.persistence
import ai.dokus.foundation.domain.configuration.ServerEndpoint
import ai.dokus.app.repository.httpClient
import ai.dokus.app.repository.utils.LoggingPlugin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class UnifiedApi private constructor(
    authApi: AuthApi,
    companyApi: CompanyApi,
    companyMembersApi: CompanyMembersApi,
    userApi: UserApi,
    documentApi: DocumentApi,
    documentExtractionApi: DocumentExtractionApi,
    documentFileApi: DocumentFileApi,
    matchingApi: MatchingApi,
    transactionApi: TransactionApi,
    transactionExtractionApi: TransactionExtractionApi,
    transactionFileApi: TransactionFileApi,
    transactionMatchingApi: TransactionMatchingApi,
    infoApi: InfoApi,
) : AuthApi by authApi,
    CompanyApi by companyApi,
    CompanyMembersApi by companyMembersApi,
    UserApi by userApi,
    DocumentApi by documentApi,
    DocumentExtractionApi by documentExtractionApi,
    DocumentFileApi by documentFileApi,
    MatchingApi by matchingApi,
    TransactionApi by transactionApi,
    TransactionExtractionApi by transactionExtractionApi,
    TransactionFileApi by transactionFileApi,
    TransactionMatchingApi by transactionMatchingApi,
    InfoApi by infoApi {

    companion object {
        private fun createHttpClient(endpoint: ServerEndpoint): HttpClient {
            return httpClient {
                install(HttpRedirect) {
                    checkHttpMethod = false
                }
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                install(DefaultRequest) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer ${persistence.jwtToken}")
                    url {
                        if (!endpoint.isLocal) {
                            protocol = URLProtocol.HTTPS
                        }
                        host = endpoint.host
                        port = endpoint.port
                    }
                }
                install(LoggingPlugin)
            }
        }

        fun create(
            endpoint: ServerEndpoint,
        ): UnifiedApi {
            val httpClient = createHttpClient(endpoint)
            return UnifiedApi(
                AuthApi.create(httpClient, endpoint),
                CompanyApi.create(httpClient, endpoint),
                CompanyMembersApi.create(httpClient, endpoint),
                UserApi.create(httpClient, endpoint),
                DocumentApi.create(httpClient, endpoint),
                DocumentExtractionApi.create(httpClient, endpoint),
                DocumentFileApi.create(httpClient, endpoint),
                MatchingApi.create(httpClient, endpoint),
                TransactionApi.create(httpClient, endpoint),
                TransactionExtractionApi.create(httpClient, endpoint),
                TransactionFileApi.create(httpClient, endpoint),
                TransactionMatchingApi.create(httpClient, endpoint),
                InfoApi.create(httpClient, endpoint)
            )
        }
    }
}