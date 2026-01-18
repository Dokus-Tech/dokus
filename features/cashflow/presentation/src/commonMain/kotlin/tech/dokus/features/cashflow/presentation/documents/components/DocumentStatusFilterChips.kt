package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_filter_all
import tech.dokus.aura.resources.documents_filter_confirmed
import tech.dokus.aura.resources.documents_filter_needs_attention
import tech.dokus.foundation.aura.components.filter.DokusFilterToggle
import tech.dokus.foundation.aura.components.filter.DokusFilterToggleRow
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter

/**
 * Simplified filter buttons for the documents list.
 * Three options: All, Needs attention, Confirmed
 *
 * Uses DokusFilterToggle for consistent styling across the app.
 */
@Composable
internal fun DocumentFilterButtons(
    currentFilter: DocumentFilter,
    needsAttentionCount: Int,
    onFilterSelected: (DocumentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    DokusFilterToggleRow(modifier = modifier) {
        DocumentFilter.entries.forEach { filter ->
            val badge = if (filter == DocumentFilter.NeedsAttention && needsAttentionCount > 0) {
                needsAttentionCount
            } else {
                null
            }

            DokusFilterToggle(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = filter.label,
                badge = badge
            )
        }
    }
}

/**
 * Returns the localized label for each filter option.
 */
private val DocumentFilter.label: String
    @Composable get() = when (this) {
        DocumentFilter.All -> stringResource(Res.string.documents_filter_all)
        DocumentFilter.NeedsAttention -> stringResource(Res.string.documents_filter_needs_attention)
        DocumentFilter.Confirmed -> stringResource(Res.string.documents_filter_confirmed)
    }
