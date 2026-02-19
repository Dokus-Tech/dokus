package tech.dokus.foundation.aura.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_edit
import tech.dokus.foundation.aura.components.icons.LockIcon
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.status.toColor
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textMuted

/**
 * Status configuration for a DataRow.
 *
 * @property label Status text (e.g., "Verified", "Pending")
 * @property type Status type determining color
 */
data class DataRowStatus(
    val label: String,
    val type: StatusDotType,
)

/**
 * A row displaying a label-value pair with optional status and edit/lock indicators.
 *
 * Responsive behavior:
 * - Desktop (isLarge): Horizontal layout - `[Label 140dp] [Value] [Status] [Lock/Edit]`
 * - Mobile: Stacked layout - `[Label] [Status]` / `[Value full width]`
 *
 * @param label The field label
 * @param value The field value
 * @param modifier Optional modifier
 * @param locked Show lock icon and hide edit action
 * @param status Optional status indicator
 * @param onEdit Edit action callback (hidden if locked or null)
 */
@Composable
fun DataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    status: DataRowStatus? = null,
    onEdit: (() -> Unit)? = null,
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    if (isLargeScreen) {
        DataRowDesktop(
            label = label,
            value = value,
            locked = locked,
            status = status,
            onEdit = onEdit,
            modifier = modifier,
        )
    } else {
        DataRowMobile(
            label = label,
            value = value,
            locked = locked,
            status = status,
            onEdit = onEdit,
            modifier = modifier,
        )
    }
}

@Composable
private fun DataRowDesktop(
    label: String,
    value: String,
    locked: Boolean,
    status: DataRowStatus?,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Constrains.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Label (fixed width)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted,
            modifier = Modifier.width(140.dp),
        )

        // Value (flexible width)
        Text(
            text = value.ifEmpty { "-" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        // Status (if present)
        if (status != null) {
            Spacer(Modifier.width(Constrains.Spacing.medium))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall),
            ) {
                StatusDot(type = status.type)
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = status.type.toColor(),
                )
            }
        }

        // Lock or Edit action
        Spacer(Modifier.width(Constrains.Spacing.small))
        if (locked) {
            LockIcon()
        } else if (onEdit != null) {
            TextButton(onClick = onEdit) {
                Text(
                    text = stringResource(Res.string.action_edit),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun DataRowMobile(
    label: String,
    value: String,
    locked: Boolean,
    status: DataRowStatus?,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val rowModifier = if (onEdit != null && !locked) {
        modifier.clickable(onClick = onEdit)
    } else {
        modifier
    }

    Column(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(vertical = Constrains.Spacing.small),
    ) {
        // First line: Label + Status + Lock
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.weight(1f),
            )

            if (status != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall),
                ) {
                    StatusDot(type = status.type)
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = status.type.toColor(),
                    )
                }
            }

            if (locked) {
                Spacer(Modifier.width(Constrains.Spacing.small))
                LockIcon()
            }
        }

        // Second line: Value (full width)
        Text(
            text = value.ifEmpty { "-" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Constrains.Spacing.xxSmall),
        )
    }
}
