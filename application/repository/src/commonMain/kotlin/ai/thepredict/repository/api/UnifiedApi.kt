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
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.httpClient
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
        fun create(
            gateway: ServerEndpoint.Gateway,
        ): UnifiedApi {
            val httpClient = httpClient()
            return UnifiedApi(
                AuthApi.create(httpClient, gateway),
                CompanyApi.create(httpClient, gateway),
                CompanyMembersApi.create(httpClient, gateway),
                UserApi.create(httpClient, gateway),
                DocumentApi.create(httpClient, gateway),
                DocumentExtractionApi.create(httpClient, gateway),
                DocumentFileApi.create(httpClient, gateway),
                MatchingApi.create(httpClient, gateway),
                TransactionApi.create(httpClient, gateway),
                TransactionExtractionApi.create(httpClient, gateway),
                TransactionFileApi.create(httpClient, gateway),
                TransactionMatchingApi.create(httpClient, gateway),
                InfoApi.create(httpClient, gateway)
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