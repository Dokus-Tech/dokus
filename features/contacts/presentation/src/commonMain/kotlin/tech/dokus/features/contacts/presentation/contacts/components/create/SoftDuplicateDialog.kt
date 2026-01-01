package tech.dokus.features.contacts.presentation.contacts.components.create

import tech.dokus.features.contacts.mvi.SoftDuplicateReason
import tech.dokus.features.contacts.mvi.SoftDuplicateUi
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_view
import tech.dokus.aura.resources.contacts_cancel
import tech.dokus.aura.resources.contacts_continue_anyway
import tech.dokus.aura.resources.contacts_duplicate_list_hint
import tech.dokus.aura.resources.contacts_duplicate_match_name
import tech.dokus.aura.resources.contacts_duplicate_match_name_country
import tech.dokus.aura.resources.contacts_duplicate_warning
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tech.dokus.domain.ids.ContactId
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog shown when soft duplicates are found during manual contact creation.
 * User can choose to continue anyway or cancel.
 */
@Composable
fun SoftDuplicateDialog(
    duplicates: List<SoftDuplicateUi>,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onViewContact: (ContactId) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.contacts_duplicate_warning),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.contacts_duplicate_list_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

                duplicates.forEach { duplicate ->
                    DuplicateItem(
                        duplicate = duplicate,
                        onClick = { onViewContact(duplicate.contactId) }
                    )
                    Spacer(modifier = Modifier.height(Constrains.Spacing.small))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(Res.string.contacts_continue_anyway))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.contacts_cancel))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun DuplicateItem(
    duplicate: SoftDuplicateUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reasonText = when (duplicate.matchReason) {
        SoftDuplicateReason.NameAndCountry ->
            stringResource(Res.string.contacts_duplicate_match_name_country)
        SoftDuplicateReason.Name ->
            stringResource(Res.string.contacts_duplicate_match_name)
    }

    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = duplicate.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(Res.string.action_view),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
