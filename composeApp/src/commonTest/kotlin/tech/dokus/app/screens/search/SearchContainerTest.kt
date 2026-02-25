package tech.dokus.app.screens.search

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.SearchCounts
import tech.dokus.domain.model.SearchPreset
import tech.dokus.domain.model.SearchResultEntityType
import tech.dokus.domain.model.SearchSignalEventRequest
import tech.dokus.domain.model.SearchSignalEventType
import tech.dokus.domain.model.SearchSuggestion
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchContainerTest {

    @Test
    fun `screen appeared loads blank-query suggestions`() = runTest {
        val remote = FakeSearchRemoteDataSource()
        val container = SearchContainer(remote)

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val state = states.value
            assertTrue(state.hasInitialized)
            assertEquals("", state.query)
            assertEquals(
                listOf(SearchRequest("", UnifiedSearchScope.All, null)),
                remote.requests
            )
            assertEquals(listOf("KBC Bank", "January"), state.suggestions.map { it.label })
        }
    }

    @Test
    fun `query changes are debounced and only latest query is fetched`() = runTest {
        val remote = FakeSearchRemoteDataSource()
        val container = SearchContainer(remote)

        container.store.subscribeAndTest {
            advanceUntilIdle() // init -> blank query fetch
            emit(SearchIntent.QueryChanged("k"))
            advanceTimeBy(80)
            emit(SearchIntent.QueryChanged("kb"))
            advanceTimeBy(80)
            emit(SearchIntent.QueryChanged("kbc"))
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    SearchRequest("", UnifiedSearchScope.All, null),
                    SearchRequest("kbc", UnifiedSearchScope.All, null),
                ),
                actual = remote.requests
            )
            assertEquals("kbc", states.value.query)
            assertEquals("kbc", states.value.response?.query)
        }
    }

    @Test
    fun `suggestion payload applies backend action scope and preset`() = runTest {
        val remote = FakeSearchRemoteDataSource()
        val container = SearchContainer(remote)

        container.store.subscribeAndTest {
            advanceUntilIdle() // init
            emit(
                SearchIntent.SuggestionSelected(
                    SearchSuggestion(
                        label = "Overdue invoices",
                        countHint = 2,
                        actionQuery = "Overdue invoices",
                        actionScope = UnifiedSearchScope.Transactions,
                        actionPreset = SearchPreset.OverdueInvoices,
                    )
                )
            )
            advanceUntilIdle()

            val state = states.value
            assertEquals("Overdue invoices", state.query)
            assertEquals(UnifiedSearchScope.Transactions, state.scope)
            assertEquals(SearchPreset.OverdueInvoices, state.activePreset)
            assertEquals(
                SearchRequest(
                    query = "Overdue invoices",
                    scope = UnifiedSearchScope.Transactions,
                    preset = SearchPreset.OverdueInvoices,
                ),
                remote.requests.last()
            )
            assertEquals(
                SearchSignalEventType.SuggestionSelected,
                remote.signals.lastOrNull()?.eventType
            )
        }
    }

    @Test
    fun `query committed signal is deduplicated for same query`() = runTest {
        val remote = FakeSearchRemoteDataSource()
        val container = SearchContainer(remote)

        container.store.subscribeAndTest {
            advanceUntilIdle() // init
            emit(SearchIntent.QueryChanged("kbc"))
            advanceUntilIdle()
            emit(SearchIntent.QueryChanged("kbc"))
            advanceUntilIdle()

            val committed = remote.signals.filter { it.eventType == SearchSignalEventType.QueryCommitted }
            assertEquals(1, committed.size)
            assertEquals("kbc", committed.single().query)
            assertEquals("kbc", states.value.lastCommittedQuery)
        }
    }

    @Test
    fun `manual query edit clears active preset`() = runTest {
        val remote = FakeSearchRemoteDataSource()
        val container = SearchContainer(remote)

        container.store.subscribeAndTest {
            advanceUntilIdle()
            emit(
                SearchIntent.SuggestionSelected(
                    SearchSuggestion(
                        label = "Upcoming payments",
                        actionScope = UnifiedSearchScope.Transactions,
                        actionPreset = SearchPreset.UpcomingPayments,
                    )
                )
            )
            advanceUntilIdle()
            assertEquals(SearchPreset.UpcomingPayments, states.value.activePreset)

            emit(SearchIntent.QueryChanged("custom"))
            advanceUntilIdle()
            assertNull(states.value.activePreset)
        }
    }

    @Test
    fun `open result emits signal and navigation action`() = runTest {
        val documentId = DocumentId.parse("00000000-0000-0000-0000-000000000123")
        val contactId = ContactId.parse("00000000-0000-0000-0000-000000000124")
        val entryId = CashflowEntryId.parse("00000000-0000-0000-0000-000000000125")
        val remote = FakeSearchRemoteDataSource()
        val container = SearchContainer(remote)

        container.store.subscribeAndTest {
            emit(SearchIntent.QueryChanged("kbc"))
            advanceUntilIdle()

            SearchIntent.OpenDocument(documentId) resultsIn SearchAction.NavigateToDocumentReview(documentId)
            SearchIntent.OpenContact(contactId) resultsIn SearchAction.NavigateToContactDetails(contactId)
            SearchIntent.OpenTransaction(entryId) resultsIn SearchAction.NavigateToCashflowEntry(entryId)
            advanceUntilIdle()

            val resultOpenedSignals = remote.signals.filter { it.eventType == SearchSignalEventType.ResultOpened }
            assertEquals(3, resultOpenedSignals.size)
            assertEquals(
                listOf(
                    SearchResultEntityType.Document,
                    SearchResultEntityType.Contact,
                    SearchResultEntityType.Transaction,
                ),
                resultOpenedSignals.map { it.resultEntityType }
            )
            assertEquals(
                listOf(documentId.toString(), contactId.toString(), entryId.toString()),
                resultOpenedSignals.map { it.resultEntityId }
            )
        }
    }
}

private class FakeSearchRemoteDataSource : SearchRemoteDataSource {
    val requests = mutableListOf<SearchRequest>()
    val signals = mutableListOf<SearchSignalEventRequest>()

    override suspend fun search(
        query: String,
        scope: UnifiedSearchScope,
        preset: SearchPreset?,
        limit: Int,
        suggestionLimit: Int,
    ): Result<UnifiedSearchResponse> {
        requests += SearchRequest(query, scope, preset)
        return Result.success(
            if (query.isBlank()) {
                UnifiedSearchResponse(
                    query = "",
                    scope = scope,
                    suggestions = listOf(
                        SearchSuggestion(label = "KBC Bank", countHint = 4),
                        SearchSuggestion(label = "January", countHint = 5),
                    ),
                )
            } else {
                UnifiedSearchResponse(
                    query = query,
                    scope = scope,
                    counts = SearchCounts(
                        all = 1,
                        documents = if (scope != UnifiedSearchScope.Transactions) 1 else 0,
                        contacts = if (scope == UnifiedSearchScope.Contacts) 1 else 0,
                        transactions = if (scope == UnifiedSearchScope.Transactions) 1 else 0,
                    ),
                    suggestions = emptyList(),
                )
            }
        )
    }

    override suspend fun recordSignal(
        request: SearchSignalEventRequest
    ): Result<Unit> {
        signals += request
        return Result.success(Unit)
    }
}

private data class SearchRequest(
    val query: String,
    val scope: UnifiedSearchScope,
    val preset: SearchPreset?,
)
