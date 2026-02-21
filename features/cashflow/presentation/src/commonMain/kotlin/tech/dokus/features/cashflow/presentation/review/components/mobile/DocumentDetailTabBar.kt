package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_tab_details
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.aura.resources.cashflow_tab_preview
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Tab bar for mobile document detail screen.
 * Two tabs: Preview (document view) | Details (fact validation).
 */
@Composable
internal fun DocumentDetailTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTab,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        Tab(
            selected = selectedTab == TAB_PREVIEW,
            onClick = { onTabSelected(TAB_PREVIEW) },
            text = { Text(stringResource(Res.string.cashflow_tab_preview)) }
        )
        Tab(
            selected = selectedTab == TAB_DETAILS,
            onClick = { onTabSelected(TAB_DETAILS) },
            text = { Text(stringResource(Res.string.cashflow_tab_details)) }
        )
    }
}

/** Tab index for Preview tab (document view). */
internal const val TAB_PREVIEW = 0

/** Tab index for Details tab (fact validation). */
internal const val TAB_DETAILS = 1

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun DocumentDetailTabBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentDetailTabBar(
            selectedTab = TAB_PREVIEW,
            onTabSelected = {}
        )
    }
}
