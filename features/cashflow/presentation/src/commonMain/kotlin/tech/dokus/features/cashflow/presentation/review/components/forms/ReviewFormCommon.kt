package tech.dokus.features.cashflow.presentation.review.components.forms

import tech.dokus.features.cashflow.presentation.review.ContactSuggestion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_discard
import tech.dokus.aura.resources.action_save
import tech.dokus.aura.resources.cashflow_suggested_contacts
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.state_saving
import tech.dokus.aura.resources.state_unsaved_changes
import tech.dokus.domain.ids.ContactId
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.chips.PChoiceChips
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.ui.unit.dp

@Composable
internal fun UnsavedChangesBar(
    isSaving: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.state_unsaved_changes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)) {
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = !isSaving,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 4.dp
                    )
                ) {
                    Text(
                        stringResource(Res.string.action_discard),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                POutlinedButton(
                    text = if (isSaving) {
                        stringResource(Res.string.state_saving)
                    } else {
                        stringResource(Res.string.action_save)
                    },
                    enabled = !isSaving,
                    isLoading = isSaving,
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
internal fun ContactSuggestionsChips(
    suggestions: List<ContactSuggestion>,
    selectedContactId: ContactId?,
    onSelect: (ContactId) -> Unit,
) {
    if (suggestions.isEmpty()) return

    PChoiceChips(
        options = suggestions,
        selected = suggestions.firstOrNull { it.contactId == selectedContactId },
        onSelect = { onSelect(it.contactId) },
        optionLabel = { suggestion ->
            suggestion.name.takeIf { it.isNotBlank() } ?: stringResource(Res.string.common_unknown)
        },
        label = stringResource(Res.string.cashflow_suggested_contacts),
    )
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Constrains.Spacing.small)
    )
}

@Composable
internal fun DetailRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    val trimmedValue = value?.trim()
    val isUnknown = trimmedValue.isNullOrBlank()
    val displayValue = if (isUnknown) {
        stringResource(Res.string.common_unknown)
    } else {
        trimmedValue.orEmpty()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isUnknown) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DetailBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
