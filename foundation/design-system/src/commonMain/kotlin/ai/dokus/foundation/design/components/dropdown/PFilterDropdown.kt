package ai.dokus.foundation.design.components.dropdown

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default values for [PFilterDropdown] component.
 */
object PFilterDropdownDefaults {
    /** Default corner radius for the dropdown surface */
    val cornerRadius: Dp = 8.dp

    /** Default border width for the dropdown surface */
    val borderWidth: Dp = 1.dp

    /** Default horizontal padding inside the dropdown */
    val horizontalPadding: Dp = 12.dp

    /** Default vertical padding inside the dropdown */
    val verticalPadding: Dp = 8.dp

    /** Default size for the dropdown icon */
    val iconSize: Dp = 16.dp

    /** Default spacing between elements */
    val spacing: Dp = 4.dp
}

/**
 * A generic dropdown component for selecting filter options.
 *
 * Displays the currently selected option with a label and dropdown arrow.
 * Clicking opens a menu with all available options. The selected option
 * is highlighted with the primary color.
 *
 * This component works with any type that implements [FilterOption], enabling
 * type-safe, reusable filter dropdowns across the application.
 *
 * Example usage:
 * ```kotlin
 * enum class SortOption(override val displayName: String) : FilterOption {
 *     Newest("Newest First"),
 *     Oldest("Oldest First"),
 *     Name("By Name")
 * }
 *
 * PFilterDropdown(
 *     label = "Sort:",
 *     selectedOption = selectedSort,
 *     options = SortOption.entries.toList(),
 *     onOptionSelected = { viewModel.updateSort(it) }
 * )
 * ```
 *
 * @param T The type of filter option, must implement [FilterOption]
 * @param label The label text displayed before the selected option (e.g., "Sort:", "Filter:")
 * @param selectedOption The currently selected option
 * @param options List of all available options to choose from
 * @param onOptionSelected Callback invoked when a new option is selected
 * @param modifier Optional modifier for the component
 * @param contentDescription Description for accessibility, defaults to "Expand [label] options"
 */
@Composable
fun <T : FilterOption> PFilterDropdown(
    label: String,
    selectedOption: T,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Expand ${label.removeSuffix(":")} options"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(PFilterDropdownDefaults.cornerRadius),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(
                width = PFilterDropdownDefaults.borderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(PFilterDropdownDefaults.cornerRadius)
            )
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = PFilterDropdownDefaults.horizontalPadding,
                    vertical = PFilterDropdownDefaults.verticalPadding
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(PFilterDropdownDefaults.spacing))

                Text(
                    text = selectedOption.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(PFilterDropdownDefaults.spacing))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(PFilterDropdownDefaults.iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
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
 * A convenience overload of [PFilterDropdown] for enum types that automatically
 * provides all enum entries as options.
 *
 * This inline reified function enables type-safe access to enum entries without
 * requiring the caller to explicitly pass the options list.
 *
 * Example usage:
 * ```kotlin
 * PFilterDropdown<SortOption>(
 *     label = "Sort:",
 *     selectedOption = selectedSort,
 *     onOptionSelected = { viewModel.updateSort(it) }
 * )
 * ```
 *
 * @param T The enum type that implements [FilterOption]
 * @param label The label text displayed before the selected option
 * @param selectedOption The currently selected option
 * @param onOptionSelected Callback invoked when a new option is selected
 * @param modifier Optional modifier for the component
 * @param contentDescription Description for accessibility
 */
@Composable
inline fun <reified T> PFilterDropdown(
    label: String,
    selectedOption: T,
    noinline onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Expand ${label.removeSuffix(":")} options"
) where T : Enum<T>, T : FilterOption {
    PFilterDropdown(
        label = label,
        selectedOption = selectedOption,
        options = enumValues<T>().toList(),
        onOptionSelected = onOptionSelected,
        modifier = modifier,
        contentDescription = contentDescription
    )
}
