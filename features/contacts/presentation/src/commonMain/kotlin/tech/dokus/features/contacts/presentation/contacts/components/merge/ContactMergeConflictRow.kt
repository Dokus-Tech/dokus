package tech.dokus.features.contacts.presentation.contacts.components.merge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_merge_source_archive
import tech.dokus.aura.resources.contacts_merge_target_keep
import tech.dokus.features.contacts.presentation.contacts.model.MergeFieldConflict

@Composable
internal fun ContactMergeConflictRow(
    conflict: MergeFieldConflict,
    onKeepSourceChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(conflict.fieldLabelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = conflict.keepSource,
                onClick = { onKeepSourceChange(true) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.contacts_merge_source_archive),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = formatConflictValue(conflict.fieldName, conflict.sourceValue),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (conflict.keepSource) FontWeight.Medium else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = !conflict.keepSource,
                onClick = { onKeepSourceChange(false) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.contacts_merge_target_keep),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatConflictValue(conflict.fieldName, conflict.targetValue),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (!conflict.keepSource) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}
