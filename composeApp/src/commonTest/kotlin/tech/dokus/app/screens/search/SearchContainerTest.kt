package tech.dokus.app.screens.search

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.SearchCounts
import tech.dokus.domain.model.SearchSuggestion
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope
import kotlin.test.Test
import kotlin.test.assertEquals
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
            assertEquals(listOf("" to UnifiedSearchScope.All), remote.requests)
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
                    "" to UnifiedSearchScope.All,
                    "kbc" to UnifiedSearchScope.All
                ),
                actual = remote.requests
            )
            assertEquals("kbc", states.value.query)
            assertEquals("kbc", states.value.response?.query)
        }
    }

    @Test
    fun `open document emits navigation action`() = runTest {
        val documentId = DocumentId.parse("00000000-0000-0000-0000-000000000123")
        val container = SearchContainer(FakeSearchRemoteDataSource())

        container.store.subscribeAndTest {
            SearchIntent.OpenDocument(documentId) resultsIn
                SearchAction.NavigateToDocumentReview(documentId)
        }
    }
}

private class FakeSearchRemoteDataSource : SearchRemoteDataSource {
    val requests = mutableListOf<Pair<String, UnifiedSearchScope>>()

    override suspend fun search(
        query: String,
        scope: UnifiedSearchScope,
        limit: Int,
        suggestionLimit: Int,
    ): Result<UnifiedSearchResponse> {
        requests += query to scope
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
                    counts = SearchCounts(all = 1, documents = 1, contacts = 0, transactions = 0),
                    suggestions = emptyList(),
                )
            }
        )
    }
}
