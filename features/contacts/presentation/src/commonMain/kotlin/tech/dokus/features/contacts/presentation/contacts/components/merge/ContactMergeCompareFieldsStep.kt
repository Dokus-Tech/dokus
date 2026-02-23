package tech.dokus.features.contacts.presentation.contacts.components.merge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_merge_move_items_info
import tech.dokus.aura.resources.contacts_merge_no_conflicts
import tech.dokus.aura.resources.contacts_merge_resolve_conflict_plural
import tech.dokus.aura.resources.contacts_merge_resolve_conflict_single
import tech.dokus.aura.resources.contacts_merge_source_archive
import tech.dokus.aura.resources.contacts_merge_target_keep
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.presentation.contacts.model.MergeFieldConflict
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun ContactMergeCompareFieldsStep(
    sourceContact: ContactDto,
    targetContact: ContactDto,
    conflicts: List<MergeFieldConflict>,
    onConflictResolutionChange: (Int, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(
                min = Constraints.SearchField.minWidth,
                max = Constraints.DialogSize.maxWidth
            )
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.contacts_merge_source_archive),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = sourceContact.name.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = Constraints.Spacing.small)
                    .size(Constraints.IconSize.smallMedium),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.contacts_merge_target_keep),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = targetContact.name.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        if (conflicts.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Constraints.Spacing.medium),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Constraints.IconSize.smallMedium)
                    )
                    Spacer(modifier = Modifier.width(Constraints.Spacing.small))
                    Text(
                        text = stringResource(Res.string.contacts_merge_no_conflicts),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            Text(
                text = if (conflicts.size == 1) {
                    stringResource(Res.string.contacts_merge_resolve_conflict_single, conflicts.size)
                } else {
                    stringResource(Res.string.contacts_merge_resolve_conflict_plural, conflicts.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(Constraints.Spacing.small))

            conflicts.forEachIndexed { index, conflict ->
                ContactMergeConflictRow(
                    conflict = conflict,
                    onKeepSourceChange = { keepSource ->
                        onConflictResolutionChange(index, keepSource)
                    }
                )

                if (index < conflicts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Constraints.Spacing.small),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.contacts_merge_move_items_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Constraints.Spacing.medium)
            )
        }
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactMergeCompareFieldsStepPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    val now = kotlinx.datetime.LocalDateTime(2026, 1, 15, 10, 0)
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactMergeCompareFieldsStep(
            sourceContact = ContactDto(
                id = tech.dokus.domain.ids.ContactId.generate(),
                tenantId = tech.dokus.domain.ids.TenantId.generate(),
                name = tech.dokus.domain.Name("Old Company Name"),
                email = tech.dokus.domain.Email("old@acme.be"),
                createdAt = now,
                updatedAt = now
            ),
            targetContact = ContactDto(
                id = tech.dokus.domain.ids.ContactId.generate(),
                tenantId = tech.dokus.domain.ids.TenantId.generate(),
                name = tech.dokus.domain.Name("Acme Corporation"),
                email = tech.dokus.domain.Email("info@acme.be"),
                createdAt = now,
                updatedAt = now
            ),
            conflicts = listOf(
                MergeFieldConflict(
                    fieldName = "email",
                    fieldLabelRes = Res.string.contacts_email,
                    sourceValue = "old@acme.be",
                    targetValue = "info@acme.be",
                    keepSource = false
                )
            ),
            onConflictResolutionChange = { _, _ -> }
        )
    }
}
