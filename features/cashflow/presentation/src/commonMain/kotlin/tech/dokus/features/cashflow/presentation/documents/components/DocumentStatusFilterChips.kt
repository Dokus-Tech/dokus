package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_filter_all
import tech.dokus.aura.resources.documents_filter_confirmed
import tech.dokus.aura.resources.documents_filter_failed
import tech.dokus.aura.resources.documents_filter_needs_review
import tech.dokus.aura.resources.documents_filter_processing
import tech.dokus.aura.resources.documents_filter_ready
import tech.dokus.aura.resources.documents_filter_rejected

/**
 * Status filter chips for the documents list.
 * Supports all DocumentDisplayStatus values with horizontal scrolling.
 */
@Composable
internal fun DocumentStatusFilterChips(
    selectedStatus: DocumentDisplayStatus?,
    onStatusSelected: (DocumentDisplayStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text(stringResource(Res.string.documents_filter_all)) }
        )

        // Processing
        FilterChip(
            selected = selectedStatus == DocumentDisplayStatus.Processing,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DocumentDisplayStatus.Processing) null
                    else DocumentDisplayStatus.Processing
                )
            },
            label = { Text(stringResource(Res.string.documents_filter_processing)) }
        )

        // Needs review
        FilterChip(
            selected = selectedStatus == DocumentDisplayStatus.NeedsReview,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DocumentDisplayStatus.NeedsReview) null
                    else DocumentDisplayStatus.NeedsReview
                )
            },
            label = { Text(stringResource(Res.string.documents_filter_needs_review)) }
        )

        // Ready
        FilterChip(
            selected = selectedStatus == DocumentDisplayStatus.Ready,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DocumentDisplayStatus.Ready) null
                    else DocumentDisplayStatus.Ready
                )
            },
            label = { Text(stringResource(Res.string.documents_filter_ready)) }
        )

        // Confirmed
        FilterChip(
            selected = selectedStatus == DocumentDisplayStatus.Confirmed,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DocumentDisplayStatus.Confirmed) null
                    else DocumentDisplayStatus.Confirmed
                )
            },
            label = { Text(stringResource(Res.string.documents_filter_confirmed)) }
        )

        // Failed
        FilterChip(
            selected = selectedStatus == DocumentDisplayStatus.Failed,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DocumentDisplayStatus.Failed) null
                    else DocumentDisplayStatus.Failed
                )
            },
            label = { Text(stringResource(Res.string.documents_filter_failed)) }
        )

        // Rejected
        FilterChip(
            selected = selectedStatus == DocumentDisplayStatus.Rejected,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DocumentDisplayStatus.Rejected) null
                    else DocumentDisplayStatus.Rejected
                )
            },
            label = { Text(stringResource(Res.string.documents_filter_rejected)) }
        )
    }
}
