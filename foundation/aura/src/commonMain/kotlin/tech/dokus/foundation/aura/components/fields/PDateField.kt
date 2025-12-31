package tech.dokus.foundation.aura.components.fields

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_select_date
import tech.dokus.foundation.aura.components.PDatePickerDialog
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
fun PDateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(Res.string.action_select_date),
    enabled: Boolean = true,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .border(
                    width = Constrains.Stroke.thin,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small,
                )
                .clickable(enabled = enabled) { showDatePicker = true }
                .padding(
                    horizontal = Constrains.Spacing.large,
                    vertical = Constrains.Spacing.medium,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value?.toString() ?: placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = if (value != null && enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Icon(
                imageVector = FeatherIcons.Calendar,
                contentDescription = placeholder,
                modifier = Modifier.size(Constrains.IconSize.small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDatePicker) {
        PDatePickerDialog(
            initialDate = value,
            onDateSelected = { selectedDate ->
                onValueChange(selectedDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}
