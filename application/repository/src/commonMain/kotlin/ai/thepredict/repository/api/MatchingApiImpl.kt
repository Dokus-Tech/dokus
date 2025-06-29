package ai.thepredict.repository.api

import ai.thepredict.apispec.MatchingApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.MatchedSchema
import ai.thepredict.domain.model.SimpleMatchDocumentsResult
import ai.thepredict.repository.extensions.withCompanyId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlin.Result
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class MatchingApiImpl(
    private val client: HttpClient,
) : MatchingApi {
    private val basePath = "/api/v1"

    override suspend fun getDocumentMatching(
        documentId: String,
        companyId: String
    ): Result<MatchedSchema> {
        return runCatching {
            client.get("$basePath/documents/$documentId/matching") {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun getAllMatching(companyId: String): Result<SimpleMatchDocumentsResult> {
        return runCatching {
            client.get("$basePath/matching") {
                withCompanyId(companyId)
            }.body()
        }
    }
}

internal fun MatchingApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): MatchingApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return MatchingApiImpl(
        client = httpClient,
    )
}