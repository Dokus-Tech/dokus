package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_filter_all
import tech.dokus.aura.resources.draft_status_confirmed
import tech.dokus.aura.resources.draft_status_needs_review
import tech.dokus.aura.resources.draft_status_ready
import tech.dokus.domain.enums.DraftStatus

/**
 * Status filter chips for the documents list.
 */
@Composable
internal fun DocumentStatusFilterChips(
    selectedStatus: DraftStatus?,
    onStatusSelected: (DraftStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text(stringResource(Res.string.contacts_filter_all)) }
        )
        FilterChip(
            selected = selectedStatus == DraftStatus.NeedsReview,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DraftStatus.NeedsReview) null else DraftStatus.NeedsReview
                )
            },
            label = { Text(stringResource(Res.string.draft_status_needs_review)) }
        )
        FilterChip(
            selected = selectedStatus == DraftStatus.Ready,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DraftStatus.Ready) null else DraftStatus.Ready
                )
            },
            label = { Text(stringResource(Res.string.draft_status_ready)) }
        )
        FilterChip(
            selected = selectedStatus == DraftStatus.Confirmed,
            onClick = {
                onStatusSelected(
                    if (selectedStatus == DraftStatus.Confirmed) null else DraftStatus.Confirmed
                )
            },
            label = { Text(stringResource(Res.string.draft_status_confirmed)) }
        )
    }
}
