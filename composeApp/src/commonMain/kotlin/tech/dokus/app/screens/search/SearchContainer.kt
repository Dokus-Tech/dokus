package tech.dokus.app.screens.search

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.model.UnifiedSearchScope
import tech.dokus.foundation.platform.Logger

private const val SearchDebounceMs = 220L
private const val DefaultLimit = 20
private const val DefaultSuggestionLimit = 8

internal typealias SearchCtx = PipelineContext<SearchState, SearchIntent, SearchAction>

internal class SearchContainer(
    private val remoteDataSource: SearchRemoteDataSource,
) : Container<SearchState, SearchIntent, SearchAction> {
    private val logger = Logger.forClass<SearchContainer>()
    private var searchJob: Job? = null

    override val store: Store<SearchState, SearchIntent, SearchAction> =
        store(SearchState()) {
            init {
                intent(SearchIntent.ScreenAppeared)
            }

            reduce { intent ->
                when (intent) {
                    SearchIntent.ScreenAppeared -> handleScreenAppeared()
                    is SearchIntent.QueryChanged -> handleQueryChanged(intent.query)
                    is SearchIntent.ScopeChanged -> handleScopeChanged(intent.scope)
                    is SearchIntent.SuggestionSelected -> handleSuggestionSelected(intent.query)
                    SearchIntent.Retry -> handleRetry()
                    is SearchIntent.FocusRequested -> handleFocusRequest(intent.requestId)
                    is SearchIntent.OpenDocument -> action(
                        SearchAction.NavigateToDocumentReview(intent.documentId)
                    )
                    is SearchIntent.OpenContact -> action(
                        SearchAction.NavigateToContactDetails(intent.contactId)
                    )
                    is SearchIntent.OpenTransaction -> action(
                        SearchAction.NavigateToCashflowEntry(intent.entryId)
                    )
                }
            }
        }

    private suspend fun SearchCtx.handleScreenAppeared() {
        withState<SearchState, _> {
            if (hasInitialized) return@withState
            fetch(query = "", scope = scope, debounce = false)
        }
    }

    private suspend fun SearchCtx.handleQueryChanged(query: String) {
        updateState { copy(query = query) }
        withState<SearchState, _> {
            fetch(query = query, scope = scope, debounce = true)
        }
    }

    private suspend fun SearchCtx.handleScopeChanged(scope: UnifiedSearchScope) {
        updateState { copy(scope = scope) }
        withState<SearchState, _> {
            fetch(query = query, scope = scope, debounce = false)
        }
    }

    private suspend fun SearchCtx.handleSuggestionSelected(query: String) {
        updateState { copy(query = query) }
        withState<SearchState, _> {
            fetch(query = query, scope = scope, debounce = false)
        }
    }

    private suspend fun SearchCtx.handleRetry() {
        withState<SearchState, _> {
            fetch(query = query, scope = scope, debounce = false)
        }
    }

    private suspend fun SearchCtx.handleFocusRequest(requestId: Long) {
        withState<SearchState, _> {
            if (requestId <= focusRequestId) return@withState
            updateState { copy(focusRequestId = requestId) }
        }
    }

    private suspend fun SearchCtx.fetch(
        query: String,
        scope: UnifiedSearchScope,
        debounce: Boolean
    ) {
        searchJob?.cancel()
        searchJob = launch {
            if (debounce) delay(SearchDebounceMs)

            updateState { copy(isLoading = true) }

            val normalizedQuery = query.trim()
            remoteDataSource.search(
                query = normalizedQuery,
                scope = scope,
                limit = DefaultLimit,
                suggestionLimit = DefaultSuggestionLimit,
            ).fold(
                onSuccess = { response ->
                    updateState {
                        copy(
                            response = response,
                            suggestions = response.suggestions,
                            isLoading = false,
                            hasInitialized = true,
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Search request failed" }
                    updateState {
                        copy(
                            isLoading = false,
                            hasInitialized = true,
                        )
                    }
                },
            )
        }
    }
}
