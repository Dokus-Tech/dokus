package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_filter_all
import tech.dokus.aura.resources.documents_filter_confirmed
import tech.dokus.aura.resources.documents_filter_needs_attention
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter
import tech.dokus.foundation.aura.components.tabs.DokusTab
import tech.dokus.foundation.aura.components.tabs.DokusTabs
import tech.dokus.foundation.aura.style.amberSoft
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Filter tabs for the documents list using DokusTabs.
 * Three options: All, Attention (amber count badge), Confirmed.
 */
@Composable
internal fun DocumentFilterButtons(
    currentFilter: DocumentFilter,
    totalCount: Int,
    needsAttentionCount: Int,
    onFilterSelected: (DocumentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        DokusTab(
            id = DocumentFilter.All.name,
            label = stringResource(Res.string.documents_filter_all),
            count = totalCount.takeIf { it > 0 },
        ),
        DokusTab(
            id = DocumentFilter.NeedsAttention.name,
            label = stringResource(Res.string.documents_filter_needs_attention),
            count = needsAttentionCount.takeIf { it > 0 },
            countColor = MaterialTheme.colorScheme.primary,
            countBackground = MaterialTheme.colorScheme.amberSoft,
        ),
        DokusTab(
            id = DocumentFilter.Confirmed.name,
            label = stringResource(Res.string.documents_filter_confirmed),
        ),
    )

    DokusTabs(
        tabs = tabs,
        activeId = currentFilter.name,
        onTabSelected = { id ->
            val filter = DocumentFilter.entries.first { it.name == id }
            onFilterSelected(filter)
        },
        modifier = modifier,
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun DocumentFilterButtonsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentFilterButtons(
            currentFilter = DocumentFilter.All,
            totalCount = 12,
            needsAttentionCount = 3,
            onFilterSelected = {},
        )
    }
}
