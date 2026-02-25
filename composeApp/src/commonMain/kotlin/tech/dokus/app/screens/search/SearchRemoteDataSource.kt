package tech.dokus.app.screens.search

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import tech.dokus.domain.model.SearchPreset
import tech.dokus.domain.model.SearchSignalEventRequest
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope
import tech.dokus.domain.routes.Search
import tech.dokus.foundation.platform.Logger

internal interface SearchRemoteDataSource {
    suspend fun search(
        query: String,
        scope: UnifiedSearchScope = UnifiedSearchScope.All,
        preset: SearchPreset? = null,
        limit: Int = 20,
        suggestionLimit: Int = 8,
    ): Result<UnifiedSearchResponse>

    suspend fun recordSignal(
        request: SearchSignalEventRequest,
    ): Result<Unit>
}

internal class SearchRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : SearchRemoteDataSource {
    private val logger = Logger.forClass<SearchRemoteDataSourceImpl>()

    override suspend fun search(
        query: String,
        scope: UnifiedSearchScope,
        preset: SearchPreset?,
        limit: Int,
        suggestionLimit: Int,
    ): Result<UnifiedSearchResponse> {
        return runCatching {
            httpClient.get(
                Search(
                    query = query,
                    scope = scope,
                    preset = preset,
                    limit = limit,
                    suggestionLimit = suggestionLimit,
                )
            ).body<UnifiedSearchResponse>()
        }.onFailure { error ->
            logger.e(error) { "Unified search failed" }
        }
    }

    override suspend fun recordSignal(
        request: SearchSignalEventRequest
    ): Result<Unit> = runCatching {
        httpClient.post(Search.Events()) {
            setBody(request)
        }
        Unit
    }.onFailure { error ->
        logger.e(error) { "Failed to record search signal" }
    }
}
