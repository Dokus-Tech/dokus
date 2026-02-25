package tech.dokus.app.screens.search

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.navigation.SearchFocusRequestBus
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun SearchRoute(
    container: SearchContainer = container(),
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequestId by SearchFocusRequestBus.focusRequestId.collectAsState()
    var lastSeenFocusId by remember { mutableLongStateOf(focusRequestId) }

    LaunchedEffect(focusRequestId) {
        if (focusRequestId > 0L && focusRequestId != lastSeenFocusId) {
            lastSeenFocusId = focusRequestId
            container.store.intent(SearchIntent.FocusRequested(focusRequestId))
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is SearchAction.NavigateToDocumentReview -> {
                navController.navigateTo(
                    CashFlowDestination.DocumentReview(
                        documentId = action.documentId.toString()
                    )
                )
            }

            is SearchAction.NavigateToContactDetails -> {
                navController.navigateTo(
                    ContactsDestination.ContactDetails(
                        contactId = action.contactId.toString()
                    )
                )
            }

            is SearchAction.NavigateToCashflowEntry -> {
                navController.navigateTo(
                    CashFlowDestination.CashflowLedger(
                        highlightEntryId = action.entryId.toString()
                    )
                )
            }
        }
    }

    SearchScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onQueryChange = { container.store.intent(SearchIntent.QueryChanged(it)) },
        onScopeSelected = { container.store.intent(SearchIntent.ScopeChanged(it)) },
        onSuggestionClick = { container.store.intent(SearchIntent.SuggestionSelected(it)) },
        onDocumentClick = { container.store.intent(SearchIntent.OpenDocument(it)) },
        onContactClick = { container.store.intent(SearchIntent.OpenContact(it)) },
        onTransactionClick = { container.store.intent(SearchIntent.OpenTransaction(it)) },
        onRetry = { container.store.intent(SearchIntent.Retry) },
    )
}
