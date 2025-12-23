package ai.dokus.app.contacts.components

import ai.dokus.app.contacts.viewmodel.ContactActiveFilter
import ai.dokus.app.contacts.viewmodel.ContactRoleFilter
import ai.dokus.app.contacts.viewmodel.ContactSortOption
import ai.dokus.foundation.design.components.dropdown.PFilterDropdown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Filter controls for the Contacts screen (desktop layout).
 *
 * Displays filter options including sort dropdown, role filter, and active status filter.
 * This component provides a standardized filter row layout.
 *
 * @param selectedSortOption The currently selected sort option
 * @param selectedRoleFilter The currently selected role filter
 * @param selectedActiveFilter The currently selected active status filter
 * @param onSortOptionSelected Callback when a sort option is selected
 * @param onRoleFilterSelected Callback when a role filter is selected
 * @param onActiveFilterSelected Callback when an active filter is selected
 * @param modifier Optional modifier for the component
 */
@Composable
internal fun ContactsFilters(
    selectedSortOption: ContactSortOption,
    selectedRoleFilter: ContactRoleFilter,
    selectedActiveFilter: ContactActiveFilter,
    onSortOptionSelected: (ContactSortOption) -> Unit,
    onRoleFilterSelected: (ContactRoleFilter) -> Unit,
    onActiveFilterSelected: (ContactActiveFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort dropdown
        PFilterDropdown(
            label = "Sort:",
            selectedOption = selectedSortOption,
            onOptionSelected = onSortOptionSelected,
            contentDescription = "Expand sort options"
        )

        // Role filter dropdown
        PFilterDropdown(
            label = "Role:",
            selectedOption = selectedRoleFilter,
            onOptionSelected = onRoleFilterSelected,
            contentDescription = "Expand role filter options"
        )

        // Active status filter dropdown
        PFilterDropdown(
            label = "Status:",
            selectedOption = selectedActiveFilter,
            onOptionSelected = onActiveFilterSelected,
            contentDescription = "Expand status filter options"
        )
    }
}

/**
 * Compact filter controls for mobile screens.
 *
 * Provides a mobile-optimized filter layout with responsive sizing.
 * Uses the same filter controls as [ContactsFilters] but with
 * mobile-specific spacing and layout.
 *
 * @param selectedSortOption The currently selected sort option
 * @param selectedRoleFilter The currently selected role filter
 * @param selectedActiveFilter The currently selected active status filter
 * @param onSortOptionSelected Callback when a sort option is selected
 * @param onRoleFilterSelected Callback when a role filter is selected
 * @param onActiveFilterSelected Callback when an active filter is selected
 * @param modifier Optional modifier for the component
 */
@Composable
internal fun ContactsFiltersMobile(
    selectedSortOption: ContactSortOption,
    selectedRoleFilter: ContactRoleFilter,
    selectedActiveFilter: ContactActiveFilter,
    onSortOptionSelected: (ContactSortOption) -> Unit,
    onRoleFilterSelected: (ContactRoleFilter) -> Unit,
    onActiveFilterSelected: (ContactActiveFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort dropdown
        PFilterDropdown(
            label = "Sort:",
            selectedOption = selectedSortOption,
            onOptionSelected = onSortOptionSelected,
            contentDescription = "Expand sort options"
        )

        // Role filter dropdown
        PFilterDropdown(
            label = "Role:",
            selectedOption = selectedRoleFilter,
            onOptionSelected = onRoleFilterSelected,
            contentDescription = "Expand role filter options"
        )

        // Active status filter dropdown
        PFilterDropdown(
            label = "Status:",
            selectedOption = selectedActiveFilter,
            onOptionSelected = onActiveFilterSelected,
            contentDescription = "Expand status filter options"
        )
    }
}
