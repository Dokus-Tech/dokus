package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.domain.enums.DraftStatus

/**
 * Search bar with status filter chips for documents screen.
 */
@Composable
internal fun DocumentsSearchBar(
    searchQuery: String,
    statusFilter: DraftStatus?,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (DraftStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search documents...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = statusFilter == DraftStatus.NeedsReview,
                onClick = {
                    onStatusFilterChange(
                        if (statusFilter == DraftStatus.NeedsReview) null else DraftStatus.NeedsReview
                    )
                },
                label = { Text("Review") }
            )
            FilterChip(
                selected = statusFilter == DraftStatus.Ready,
                onClick = {
                    onStatusFilterChange(
                        if (statusFilter == DraftStatus.Ready) null else DraftStatus.Ready
                    )
                },
                label = { Text("Ready") }
            )
            FilterChip(
                selected = statusFilter == DraftStatus.Confirmed,
                onClick = {
                    onStatusFilterChange(
                        if (statusFilter == DraftStatus.Confirmed) null else DraftStatus.Confirmed
                    )
                },
                label = { Text("Confirmed") }
            )
        }
    }
}
