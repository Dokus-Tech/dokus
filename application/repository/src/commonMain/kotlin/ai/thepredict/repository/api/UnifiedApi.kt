package ai.thepredict.repository.api

import ai.thepredict.apispec.AuthApi
import ai.thepredict.apispec.CompanyApi
import ai.thepredict.apispec.CompanyMembersApi
import ai.thepredict.apispec.DocumentApi
import ai.thepredict.apispec.DocumentExtractionApi
import ai.thepredict.apispec.DocumentFileApi
import ai.thepredict.apispec.InfoApi
import ai.thepredict.apispec.MatchingApi
import ai.thepredict.apispec.TransactionApi
import ai.thepredict.apispec.TransactionExtractionApi
import ai.thepredict.apispec.TransactionFileApi
import ai.thepredict.apispec.TransactionMatchingApi
import ai.thepredict.apispec.UserApi
import ai.thepredict.app.platform.persistence
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.httpClient
import ai.thepredict.repository.utils.LoggingPlugin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

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
                    host = endpoint.externalHost
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

internal interface ApiCompanion<ApiClass, EndpointType : ServerEndpoint> {
    fun create(
        coroutineContext: CoroutineContext,
        endpoint: EndpointType,
    ): ApiClass

    fun create(
        coroutineContext: CoroutineContext,
        endpoint: ServerEndpoint.Gateway,
    ): ApiClass
}