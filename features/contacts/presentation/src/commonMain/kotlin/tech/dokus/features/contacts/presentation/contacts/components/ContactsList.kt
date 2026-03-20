package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.constrains.Constraints

private const val InfiniteScrollThreshold = 5

/**
 * ContactsList component with scrollable list and empty state.
 * Handles loading, success, error, and empty states.
 *
 * @param state The DokusState containing pagination state for contacts
 * @param onContactClick Callback when a contact is clicked
 * @param onLoadMore Callback for infinite scroll pagination
 * @param onAddContactClick Callback when the empty state add button is clicked
 * @param contentPadding Optional content padding
 * @param modifier Optional modifier
 * @param selectedContactId Currently selected contact for highlight (desktop)
 * @param isDesktop Desktop mode renders simple list rows instead of cards
 */
@Composable
internal fun ContactsList(
    state: DokusState<PaginationState<ContactDto>>,
    onContactClick: (ContactDto) -> Unit,
    onLoadMore: () -> Unit,
    onAddContactClick: (() -> Unit)?,
    contentPadding: PaddingValues = PaddingValues(Constraints.Elevation.none),
    modifier: Modifier = Modifier,
    selectedContactId: ContactId? = null,
    isDesktop: Boolean = false,
) {
    val listState = rememberLazyListState()
    val paginationData = state.lastData
    val contacts = paginationData?.data ?: emptyList()

    // Infinite scroll trigger
    LaunchedEffect(listState, paginationData?.hasMorePages, state.isLoading()) {
        if (paginationData == null) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (last, total) ->
                (last + 1) > (total - InfiniteScrollThreshold) &&
                        paginationData.hasMorePages &&
                        !state.isLoading()
            }
            .collect { onLoadMore() }
    }

    when {
        state.isLoading() && paginationData == null -> {
            ContactsListSkeleton(
                contentPadding = contentPadding,
                modifier = modifier
            )
        }

        state is DokusState.Error -> {
            DokusErrorContent(
                exception = state.exception,
                retryHandler = state.retryHandler,
                modifier = Modifier.fillMaxSize()
            )
        }

        contacts.isEmpty() -> {
            ContactsEmptyState(
                onAddContactClick = onAddContactClick,
                modifier = modifier.padding(contentPadding)
            )
        }

        else -> {
            ContactsListContent(
                contacts = contacts,
                listState = listState,
                isLoadingMore = state.isLoading(),
                onContactClick = onContactClick,
                contentPadding = contentPadding,
                modifier = modifier,
                selectedContactId = selectedContactId,
                isDesktop = isDesktop,
            )
        }
    }
}
