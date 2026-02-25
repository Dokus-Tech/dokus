package tech.dokus.app.screens.search

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.SearchAggregates
import tech.dokus.domain.model.SearchPreset
import tech.dokus.domain.model.SearchSuggestion
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope

@Immutable
data class SearchState(
    val query: String = "",
    val scope: UnifiedSearchScope = UnifiedSearchScope.All,
    val response: UnifiedSearchResponse? = null,
    val suggestions: List<SearchSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val hasInitialized: Boolean = false,
    val focusRequestId: Long = 0L,
    val activePreset: SearchPreset? = null,
    val lastCommittedQuery: String? = null,
) : MVIState {
    val counts = response?.counts

    val visibleResultCount: Long
        get() = when (scope) {
            UnifiedSearchScope.All -> counts?.all ?: 0
            UnifiedSearchScope.Documents -> counts?.documents ?: 0
            UnifiedSearchScope.Contacts -> counts?.contacts ?: 0
            UnifiedSearchScope.Transactions -> counts?.transactions ?: 0
        }

    val aggregates: SearchAggregates
        get() = response?.aggregates ?: SearchAggregates()
}

@Immutable
sealed interface SearchIntent : MVIIntent {
    data object ScreenAppeared : SearchIntent
    data class QueryChanged(val query: String) : SearchIntent
    data class ScopeChanged(val scope: UnifiedSearchScope) : SearchIntent
    data class SuggestionSelected(val suggestion: SearchSuggestion) : SearchIntent
    data object Retry : SearchIntent
    data class FocusRequested(val requestId: Long) : SearchIntent
    data class OpenDocument(val documentId: DocumentId) : SearchIntent
    data class OpenContact(val contactId: ContactId) : SearchIntent
    data class OpenTransaction(val entryId: CashflowEntryId) : SearchIntent
}

@Immutable
sealed interface SearchAction : MVIAction {
    data class NavigateToDocumentReview(val documentId: DocumentId) : SearchAction
    data class NavigateToContactDetails(val contactId: ContactId) : SearchAction
    data class NavigateToCashflowEntry(val entryId: CashflowEntryId) : SearchAction
}