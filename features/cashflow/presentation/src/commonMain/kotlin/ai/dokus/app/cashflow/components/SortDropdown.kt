package ai.dokus.app.cashflow.components

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_sort_amount_highest
import tech.dokus.aura.resources.cashflow_sort_amount_lowest
import tech.dokus.aura.resources.cashflow_sort_date_newest
import tech.dokus.aura.resources.cashflow_sort_date_oldest
import tech.dokus.aura.resources.cashflow_sort_default
import tech.dokus.aura.resources.cashflow_sort_label
import tech.dokus.aura.resources.cashflow_sort_type
import tech.dokus.aura.resources.sort_expand
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Sort options for financial documents.
 */
enum class DocumentSortOption(val labelRes: StringResource) {
    Default(Res.string.cashflow_sort_default),
    DateNewest(Res.string.cashflow_sort_date_newest),
    DateOldest(Res.string.cashflow_sort_date_oldest),
    AmountHighest(Res.string.cashflow_sort_amount_highest),
    AmountLowest(Res.string.cashflow_sort_amount_lowest),
    Type(Res.string.cashflow_sort_type)
}

/**
 * A compact dropdown component for selecting sort order.
 *
 * Displays the currently selected option with a dropdown arrow.
 * Clicking opens a menu with all available sort options.
 *
 * @param selectedOption The currently selected sort option
 * @param onOptionSelected Callback when a new option is selected
 * @param modifier Optional modifier for the component
 */
@Composable
fun SortDropdown(
    selectedOption: DocumentSortOption,
    onOptionSelected: (DocumentSortOption) -> Unit,
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
                    text = stringResource(Res.string.cashflow_sort_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = stringResource(selectedOption.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.sort_expand),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DocumentSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes),
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
