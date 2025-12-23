package ai.dokus.app.contacts.components

import ai.dokus.app.contacts.viewmodel.ContactActiveFilter
import ai.dokus.app.contacts.viewmodel.ContactRoleFilter
import ai.dokus.app.contacts.viewmodel.ContactSortOption
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A compact dropdown component for selecting contact sort order.
 *
 * Displays the currently selected option with a dropdown arrow.
 * Clicking opens a menu with all available sort options.
 *
 * @param selectedOption The currently selected sort option
 * @param onOptionSelected Callback when a new option is selected
 * @param modifier Optional modifier for the component
 */
@Composable
internal fun ContactSortDropdown(
    selectedOption: ContactSortOption,
    onOptionSelected: (ContactSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = selectedOption.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand sort options",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ContactSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (option == selectedOption) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * A compact dropdown component for filtering contacts by role (Customer/Vendor).
 *
 * @param selectedFilter The currently selected role filter
 * @param onFilterSelected Callback when a new filter is selected
 * @param modifier Optional modifier for the component
 */
@Composable
internal fun ContactRoleFilterDropdown(
    selectedFilter: ContactRoleFilter,
    onFilterSelected: (ContactRoleFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Role:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = selectedFilter.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand role filter options",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ContactRoleFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = filter.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (filter == selectedFilter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * A compact dropdown component for filtering contacts by active status.
 *
 * @param selectedFilter The currently selected active filter
 * @param onFilterSelected Callback when a new filter is selected
 * @param modifier Optional modifier for the component
 */
@Composable
internal fun ContactActiveFilterDropdown(
    selectedFilter: ContactActiveFilter,
    onFilterSelected: (ContactActiveFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = selectedFilter.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand status filter options",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ContactActiveFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = filter.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (filter == selectedFilter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

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
        ContactSortDropdown(
            selectedOption = selectedSortOption,
            onOptionSelected = onSortOptionSelected
        )

        // Role filter dropdown
        ContactRoleFilterDropdown(
            selectedFilter = selectedRoleFilter,
            onFilterSelected = onRoleFilterSelected
        )

        // Active status filter dropdown
        ContactActiveFilterDropdown(
            selectedFilter = selectedActiveFilter,
            onFilterSelected = onActiveFilterSelected
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
        ContactSortDropdown(
            selectedOption = selectedSortOption,
            onOptionSelected = onSortOptionSelected
        )

        // Role filter dropdown
        ContactRoleFilterDropdown(
            selectedFilter = selectedRoleFilter,
            onFilterSelected = onRoleFilterSelected
        )

        // Active status filter dropdown
        ContactActiveFilterDropdown(
            selectedFilter = selectedActiveFilter,
            onFilterSelected = onActiveFilterSelected
        )
    }
}
