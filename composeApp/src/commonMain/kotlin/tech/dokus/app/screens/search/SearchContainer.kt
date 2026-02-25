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
import tech.dokus.domain.model.SearchResultEntityType
import tech.dokus.domain.model.SearchSignalEventRequest
import tech.dokus.domain.model.SearchSignalEventType
import tech.dokus.domain.model.SearchSuggestion
import tech.dokus.domain.model.SearchPreset
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
                    is SearchIntent.SuggestionSelected -> handleSuggestionSelected(intent.suggestion)
                    SearchIntent.Retry -> handleRetry()
                    is SearchIntent.FocusRequested -> handleFocusRequest(intent.requestId)
                    is SearchIntent.OpenDocument -> handleOpenDocument(intent.documentId.toString(), intent)
                    is SearchIntent.OpenContact -> handleOpenContact(intent.contactId.toString(), intent)
                    is SearchIntent.OpenTransaction -> handleOpenTransaction(intent.entryId.toString(), intent)
                }
            }
        }

    private suspend fun SearchCtx.handleScreenAppeared() {
        withState<SearchState, _> {
            if (hasInitialized) return@withState
            fetch(
                query = "",
                scope = scope,
                preset = activePreset,
                debounce = false,
                trackQueryCommit = false,
            )
        }
    }

    private suspend fun SearchCtx.handleQueryChanged(query: String) {
        updateState { copy(query = query, activePreset = null) }
        withState<SearchState, _> {
            fetch(
                query = query,
                scope = scope,
                preset = null,
                debounce = true,
                trackQueryCommit = true,
            )
        }
    }

    private suspend fun SearchCtx.handleScopeChanged(scope: UnifiedSearchScope) {
        updateState { copy(scope = scope, activePreset = null) }
        withState<SearchState, _> {
            fetch(
                query = query,
                scope = scope,
                preset = null,
                debounce = false,
                trackQueryCommit = false,
            )
        }
    }

    private suspend fun SearchCtx.handleSuggestionSelected(suggestion: SearchSuggestion) {
        val nextQuery = suggestion.actionQuery?.takeIf { it.isNotBlank() } ?: suggestion.label
        val currentScope = withState<SearchState, _> { scope }
        val nextScope = suggestion.actionScope ?: currentScope
        val nextPreset = suggestion.actionPreset

        updateState {
            copy(
                query = nextQuery,
                scope = nextScope,
                activePreset = nextPreset,
            )
        }

        recordSignalFireAndForget(
            SearchSignalEventRequest(
                eventType = SearchSignalEventType.SuggestionSelected,
                query = nextQuery,
                scope = nextScope,
                suggestionLabel = suggestion.label,
            )
        )

        withState<SearchState, _> {
            fetch(
                query = nextQuery,
                scope = nextScope,
                preset = nextPreset,
                debounce = false,
                trackQueryCommit = false,
            )
        }
    }

    private suspend fun SearchCtx.handleRetry() {
        withState<SearchState, _> {
            fetch(
                query = query,
                scope = scope,
                preset = activePreset,
                debounce = false,
                trackQueryCommit = false,
            )
        }
    }

    private suspend fun SearchCtx.handleFocusRequest(requestId: Long) {
        withState<SearchState, _> {
            if (requestId <= focusRequestId) return@withState
            updateState { copy(focusRequestId = requestId) }
        }
    }

    private suspend fun SearchCtx.handleOpenDocument(
        entityId: String,
        intent: SearchIntent.OpenDocument
    ) {
        withState<SearchState, _> {
            recordResultOpenedSignal(
                query = query,
                scope = scope,
                entityId = entityId,
                entityType = SearchResultEntityType.Document,
            )
        }
        action(SearchAction.NavigateToDocumentReview(intent.documentId))
    }

    private suspend fun SearchCtx.handleOpenContact(
        entityId: String,
        intent: SearchIntent.OpenContact
    ) {
        withState<SearchState, _> {
            recordResultOpenedSignal(
                query = query,
                scope = scope,
                entityId = entityId,
                entityType = SearchResultEntityType.Contact,
            )
        }
        action(SearchAction.NavigateToContactDetails(intent.contactId))
    }

    private suspend fun SearchCtx.handleOpenTransaction(
        entityId: String,
        intent: SearchIntent.OpenTransaction
    ) {
        withState<SearchState, _> {
            recordResultOpenedSignal(
                query = query,
                scope = scope,
                entityId = entityId,
                entityType = SearchResultEntityType.Transaction,
            )
        }
        action(SearchAction.NavigateToCashflowEntry(intent.entryId))
    }

    private fun SearchCtx.recordResultOpenedSignal(
        query: String,
        scope: UnifiedSearchScope,
        entityId: String,
        entityType: SearchResultEntityType,
    ) {
        recordSignalFireAndForget(
            SearchSignalEventRequest(
                eventType = SearchSignalEventType.ResultOpened,
                query = query,
                scope = scope,
                resultEntityType = entityType,
                resultEntityId = entityId,
            )
        )
    }

    private fun SearchCtx.recordSignalFireAndForget(
        request: SearchSignalEventRequest
    ) {
        launch {
            remoteDataSource.recordSignal(request).onFailure { error ->
                logger.e(error) { "Failed to record search signal ${request.eventType}" }
            }
        }
    }

    private suspend fun SearchCtx.fetch(
        query: String,
        scope: UnifiedSearchScope,
        preset: SearchPreset?,
        debounce: Boolean,
        trackQueryCommit: Boolean,
    ) {
        searchJob?.cancel()
        searchJob = launch {
            if (debounce) delay(SearchDebounceMs)

            updateState { copy(isLoading = true) }

            val normalizedQuery = query.trim()
            remoteDataSource.search(
                query = normalizedQuery,
                scope = scope,
                preset = preset,
                limit = DefaultLimit,
                suggestionLimit = DefaultSuggestionLimit,
            ).fold(
                onSuccess = { response ->
                    val shouldTrackCommittedQuery = trackQueryCommit &&
                        normalizedQuery.isNotBlank() &&
                        response.counts.all > 0L

                    var committedQueryToRecord: String? = null
                    if (shouldTrackCommittedQuery) {
                        withState<SearchState, _> {
                            if (lastCommittedQuery != normalizedQuery) {
                                committedQueryToRecord = normalizedQuery
                            }
                        }
                    }

                    updateState {
                        copy(
                            response = response,
                            suggestions = response.suggestions,
                            isLoading = false,
                            hasInitialized = true,
                            lastCommittedQuery = committedQueryToRecord ?: lastCommittedQuery,
                        )
                    }

                    committedQueryToRecord?.let { committedQuery ->
                        recordSignalFireAndForget(
                            SearchSignalEventRequest(
                                eventType = SearchSignalEventType.QueryCommitted,
                                query = committedQuery,
                                scope = scope,
                            )
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Search request failed" }
                    updateState {
                        copy(
                            response = null,
                            suggestions = emptyList(),
                            isLoading = false,
                            hasInitialized = true,
                        )
                    }
                },
            )
        }
    }
}
