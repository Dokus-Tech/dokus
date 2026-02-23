package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_filter_role_expand
import tech.dokus.aura.resources.contacts_filter_role_label
import tech.dokus.aura.resources.contacts_filter_sort_expand
import tech.dokus.aura.resources.contacts_filter_sort_label
import tech.dokus.aura.resources.contacts_filter_status_expand
import tech.dokus.aura.resources.contacts_filter_status_label
import tech.dokus.features.contacts.mvi.ContactActiveFilter
import tech.dokus.features.contacts.mvi.ContactRoleFilter
import tech.dokus.features.contacts.mvi.ContactSortOption
import tech.dokus.foundation.aura.components.dropdown.PFilterDropdown

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
            label = stringResource(Res.string.contacts_filter_sort_label),
            selectedOption = selectedSortOption,
            onOptionSelected = onSortOptionSelected,
            contentDescription = stringResource(Res.string.contacts_filter_sort_expand)
        )

        // Role filter dropdown
        PFilterDropdown(
            label = stringResource(Res.string.contacts_filter_role_label),
            selectedOption = selectedRoleFilter,
            onOptionSelected = onRoleFilterSelected,
            contentDescription = stringResource(Res.string.contacts_filter_role_expand)
        )

        // Active status filter dropdown
        PFilterDropdown(
            label = stringResource(Res.string.contacts_filter_status_label),
            selectedOption = selectedActiveFilter,
            onOptionSelected = onActiveFilterSelected,
            contentDescription = stringResource(Res.string.contacts_filter_status_expand)
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
            label = stringResource(Res.string.contacts_filter_sort_label),
            selectedOption = selectedSortOption,
            onOptionSelected = onSortOptionSelected,
            contentDescription = stringResource(Res.string.contacts_filter_sort_expand)
        )

        // Role filter dropdown
        PFilterDropdown(
            label = stringResource(Res.string.contacts_filter_role_label),
            selectedOption = selectedRoleFilter,
            onOptionSelected = onRoleFilterSelected,
            contentDescription = stringResource(Res.string.contacts_filter_role_expand)
        )

        // Active status filter dropdown
        PFilterDropdown(
            label = stringResource(Res.string.contacts_filter_status_label),
            selectedOption = selectedActiveFilter,
            onOptionSelected = onActiveFilterSelected,
            contentDescription = stringResource(Res.string.contacts_filter_status_expand)
        )
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactsFiltersPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactsFilters(
            selectedSortOption = ContactSortOption.Default,
            selectedRoleFilter = ContactRoleFilter.All,
            selectedActiveFilter = ContactActiveFilter.All,
            onSortOptionSelected = {},
            onRoleFilterSelected = {},
            onActiveFilterSelected = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactsFiltersMobilePreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactsFiltersMobile(
            selectedSortOption = ContactSortOption.Default,
            selectedRoleFilter = ContactRoleFilter.All,
            selectedActiveFilter = ContactActiveFilter.Active,
            onSortOptionSelected = {},
            onRoleFilterSelected = {},
            onActiveFilterSelected = {}
        )
    }
}
