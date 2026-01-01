package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Filter controls for the Cashflow screen.
 *
 * Displays filter options including sort dropdown. This component provides
 * a standardized filter row layout that can be extended with additional
 * filter controls (e.g., date range, document type, status filters).
 *
 * @param selectedSortOption The currently selected sort option
 * @param onSortOptionSelected Callback when a sort option is selected
 * @param modifier Optional modifier for the component
 */
@Composable
fun CashflowFilters(
    selectedSortOption: DocumentSortOption,
    onSortOptionSelected: (DocumentSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort dropdown - primary filter control
        SortDropdown(
            selectedOption = selectedSortOption,
            onOptionSelected = onSortOptionSelected
        )

        // Future filter controls can be added here:
        // - Document type filter (Invoice, Expense, Bill)
        // - Date range picker
        // - Status filter (Paid, Unpaid, Overdue)
        // - Amount range filter
    }
}

/**
 * Compact filter controls for mobile screens.
 *
 * Provides a mobile-optimized filter layout with responsive sizing.
 * Uses the same filter controls as [CashflowFilters] but with
 * mobile-specific spacing and layout.
 *
 * @param selectedSortOption The currently selected sort option
 * @param onSortOptionSelected Callback when a sort option is selected
 * @param modifier Optional modifier for the component
 */
@Composable
fun CashflowFiltersMobile(
    selectedSortOption: DocumentSortOption,
    onSortOptionSelected: (DocumentSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort dropdown - same component with mobile spacing
        SortDropdown(
            selectedOption = selectedSortOption,
            onOptionSelected = onSortOptionSelected
        )
    }
}
