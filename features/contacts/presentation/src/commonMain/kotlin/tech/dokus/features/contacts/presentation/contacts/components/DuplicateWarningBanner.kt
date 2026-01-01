package tech.dokus.features.contacts.presentation.contacts.components

import tech.dokus.features.contacts.mvi.DuplicateReason
import tech.dokus.features.contacts.mvi.PotentialDuplicate
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.contacts_continue_anyway
import tech.dokus.aura.resources.contacts_duplicate_warning
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.aura.resources.contacts_merge
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * A warning banner displayed when potential duplicate contacts are detected
 * during contact creation or editing.
 *
 * Shows a list of matching contacts with their match reasons, and provides
 * action buttons for the user to continue, merge, or cancel.
 *
 * @param duplicates List of potential duplicate contacts with match reasons
 * @param onContinueAnyway Called when user chooses to ignore duplicates and proceed
 * @param onMergeWithExisting Called when user wants to merge with an existing contact
 * @param onCancel Called when user cancels the current operation
 * @param modifier Optional modifier for the banner
 */
@Composable
internal fun DuplicateWarningBanner(
    duplicates: List<PotentialDuplicate>,
    onContinueAnyway: () -> Unit,
    onMergeWithExisting: (PotentialDuplicate) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (duplicates.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.large)
        ) {
            // Warning header
            DuplicateWarningHeader()

            Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

            // List of potential duplicates
            duplicates.forEach { duplicate ->
                DuplicateContactItem(
                    duplicate = duplicate,
                    onMerge = { onMergeWithExisting(duplicate) }
                )
                Spacer(modifier = Modifier.height(Constrains.Spacing.small))
            }

            Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

            // Action buttons
            DuplicateWarningActions(
                onContinueAnyway = onContinueAnyway,
                onCancel = onCancel
            )
        }
    }
}

/**
 * Header section showing warning icon and title.
 */
@Composable
private fun DuplicateWarningHeader(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = stringResource(Res.string.contacts_duplicate_warning),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Constrains.IconSize.medium)
        )

        Spacer(modifier = Modifier.width(Constrains.Spacing.small))

        Text(
            text = stringResource(Res.string.contacts_duplicate_warning),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * A single duplicate contact item showing contact info and match reason.
 */
@Composable
private fun DuplicateContactItem(
    duplicate: PotentialDuplicate,
    onMerge: () -> Unit,
    modifier: Modifier = Modifier
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Contact name
                Text(
                    text = duplicate.contact.name.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Constrains.Spacing.xxSmall))

                // Email (if available)
                duplicate.contact.email?.let { email ->
                    Text(
                        text = email.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Constrains.Spacing.xxSmall))
                }

                // Match reason badge
                DuplicateReasonBadge(reason = duplicate.matchReason)
            }

            Spacer(modifier = Modifier.width(Constrains.Spacing.small))

            // Merge button
            TextButton(onClick = onMerge) {
                Text(text = stringResource(Res.string.contacts_merge))
            }
        }
    }
}

/**
 * Badge showing the reason for the duplicate match.
 */
@Composable
private fun DuplicateReasonBadge(
    reason: DuplicateReason,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (reason) {
        DuplicateReason.VatNumber -> {
            MaterialTheme.colorScheme.error.copy(alpha = 0.1f) to MaterialTheme.colorScheme.error
        }
        DuplicateReason.Email -> {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.primary
        }
        DuplicateReason.NameAndCountry -> {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.tertiary
        }
    }

    Text(
        text = stringResource(reason.labelRes),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(
                horizontal = Constrains.Spacing.small,
                vertical = Constrains.Spacing.xxSmall
            )
    )
}

/**
 * Action buttons for the duplicate warning banner.
 */
@Composable
private fun DuplicateWarningActions(
    onContinueAnyway: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel button
        TextButton(onClick = onCancel) {
            Text(text = stringResource(Res.string.action_cancel))
        }

        Spacer(modifier = Modifier.width(Constrains.Spacing.small))

        // Continue anyway button
        OutlinedButton(
            onClick = onContinueAnyway,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = stringResource(Res.string.contacts_continue_anyway))
        }
    }
}
