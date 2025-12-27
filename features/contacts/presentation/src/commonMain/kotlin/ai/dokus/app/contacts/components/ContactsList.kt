package ai.dokus.app.contacts.components

import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.ShimmerBox
import ai.dokus.foundation.design.components.common.ShimmerLine
import tech.dokus.domain.model.ContactDto
import tech.dokus.domain.model.common.PaginationState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import tech.dokus.foundation.app.state.DokusState

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
 */
@Composable
internal fun ContactsList(
    state: DokusState<PaginationState<ContactDto>>,
    onContactClick: (ContactDto) -> Unit,
    onLoadMore: () -> Unit,
    onAddContactClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Extract pagination state for infinite scroll (if available)
    val paginationState = (state as? DokusState.Success)?.data

    // Infinite scroll trigger
    LaunchedEffect(listState, paginationState?.hasMorePages, paginationState?.isLoadingMore) {
        if (paginationState == null) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (last, total) ->
                (last + 1) > (total - 5) &&
                        paginationState.hasMorePages &&
                        !paginationState.isLoadingMore
            }
            .collect { onLoadMore() }
    }

    when (state) {
        is DokusState.Loading, is DokusState.Idle -> {
            ContactsListSkeleton(
                contentPadding = contentPadding,
                modifier = modifier
            )
        }

        is DokusState.Success -> {
            if (state.data.data.isEmpty()) {
                ContactsEmptyState(
                    onAddContactClick = onAddContactClick,
                    modifier = modifier.padding(contentPadding)
                )
            } else {
                ContactsListContent(
                    contacts = state.data.data,
                    listState = listState,
                    isLoadingMore = state.data.isLoadingMore,
                    onContactClick = onContactClick,
                    contentPadding = contentPadding,
                    modifier = modifier
                )
            }
        }

        is DokusState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                DokusErrorContent(
                    exception = state.exception,
                    retryHandler = state.retryHandler
                )
            }
        }
    }
}

/**
 * The actual list content showing contact cards.
 */
@Composable
private fun ContactsListContent(
    contacts: List<ContactDto>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    onContactClick: (ContactDto) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = contacts,
            key = { it.id.toString() }
        ) { contact ->
            ContactCard(
                contact = contact,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContactClick(contact) }
            )
        }

        // Loading more indicator
        if (isLoadingMore) {
            item {
                ContactsLoadingMoreIndicator()
            }
        }
    }
}

/**
 * Empty state when no contacts exist.
 * Shows a call-to-action to add the first contact.
 */
@Composable
private fun ContactsEmptyState(
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier
                    .height(64.dp)
                    .width(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No contacts yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add your first contact to get started with invoices and bills",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Empty state CTA card
            Card(
                onClick = onAddContactClick,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add your first contact",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Skeleton loading state for the contacts list.
 */
@Composable
private fun ContactsListSkeleton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) {
            ContactCardSkeleton()
        }
    }
}

/**
 * Skeleton for a single contact card.
 */
@Composable
private fun ContactCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Name and status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerLine(
                    modifier = Modifier.weight(0.6f),
                    height = 20.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(16.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Email line
            ShimmerLine(
                modifier = Modifier.fillMaxWidth(0.7f),
                height = 14.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tags row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

/**
 * Loading indicator for infinite scroll pagination.
 */
@Composable
private fun ContactsLoadingMoreIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
    }
}
